import os.path
import json
import contextvars
from datetime import datetime, timedelta
from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from app.agent.undo import global_undo_stack

from pathlib import Path

SCOPES = ['https://www.googleapis.com/auth/calendar']
BACKEND_DIR = Path(__file__).resolve().parent.parent.parent
TOKEN_PATH = BACKEND_DIR / "token.json"
CREDENTIALS_PATH = BACKEND_DIR / "credentials.json"

# Thread-local / Async-safe user context variables
active_user_token = contextvars.ContextVar("active_user_token", default=None)
active_user_id = contextvars.ContextVar("active_user_id", default=None)
active_user_email = contextvars.ContextVar("active_user_email", default=None)

def get_calendar_service():
    """Authenticates and returns the Google Calendar API service instance."""
    creds = None
    token_str = active_user_token.get()
    
    if token_str:
        try:
            creds = Credentials.from_authorized_user_info(json.loads(token_str), SCOPES)
            if creds and creds.expired and creds.refresh_token:
                creds.refresh(Request())
                
                # Write back refreshed token to Database
                user_id = active_user_id.get()
                if user_id:
                    from app.db.session import SessionLocal
                    from app.db.models import User
                    import uuid
                    db_sess = SessionLocal()
                    try:
                        db_sess.query(User).filter(User.user_id == uuid.UUID(user_id)).update({
                            "google_oauth_token": creds.to_json()
                        })
                        db_sess.commit()
                    except Exception as db_ex:
                        db_sess.rollback()
                        print(f"Failed to update refreshed token in DB: {db_ex}")
                    finally:
                        db_sess.close()
        except Exception as e:
            print(f"Failed to load credentials from user token: {e}")
            creds = None

    if not creds:
        # Fallback to global token.json for backward compatibility / tests
        if TOKEN_PATH.exists():
            creds = Credentials.from_authorized_user_file(str(TOKEN_PATH), SCOPES)
    
    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(Request())
        else:
            if not CREDENTIALS_PATH.exists():
                raise FileNotFoundError(
                    f"credentials.json not found at {CREDENTIALS_PATH}! Download it from Google Cloud Console "
                    "and place it in the backend/ folder."
                )
            flow = InstalledAppFlow.from_client_secrets_file(str(CREDENTIALS_PATH), SCOPES)
            creds = flow.run_local_server(port=0)
            
        with open(TOKEN_PATH, 'w') as token:
            token.write(creds.to_json())

    return build('calendar', 'v3', credentials=creds)

def list_calendar_events(max_results: int = 10) -> list:
    """Lists upcoming events on the user's primary calendar."""
    try:
        service = get_calendar_service()
        now = datetime.utcnow().isoformat() + 'Z'  # 'Z' indicates UTC time
        events_result = service.events().list(
            calendarId='primary',
            timeMin=now,
            maxResults=max_results,
            singleEvents=True,
            orderBy='startTime'
        ).execute()
        events = events_result.get('items', [])
        return events
    except Exception as e:
        print(f"Error fetching calendar events: {e}")
        return []

def parse_google_datetime(dt_str: str) -> datetime:
    """Parses Google Calendar datetime string into a naive datetime in UTC."""
    if 'T' not in dt_str:
        return datetime.strptime(dt_str, "%Y-%m-%d")
    if dt_str.endswith('Z'):
        return datetime.strptime(dt_str.replace('Z', ''), "%Y-%m-%dT%H:%M:%S")
    if '+' in dt_str:
        base_str, offset_str = dt_str.split('+', 1)
        dt = datetime.strptime(base_str, "%Y-%m-%dT%H:%M:%S")
        oh, om = map(int, offset_str.split(':', 1))
        return dt - timedelta(hours=oh, minutes=om)
    elif '-' in dt_str:
        parts = dt_str.split('T')
        if '-' in parts[1]:
            base_time, offset_str = parts[1].split('-', 1)
            base_str = parts[0] + 'T' + base_time
            dt = datetime.strptime(base_str, "%Y-%m-%dT%H:%M:%S")
            oh, om = map(int, offset_str.split(':', 1))
            return dt + timedelta(hours=oh, minutes=om)
    return datetime.strptime(dt_str.split('.')[0], "%Y-%m-%dT%H:%M:%S")

def check_calendar_conflict(start_iso: str, end_iso: str) -> dict:
    """
    Checks if there are any existing events that overlap with the requested time window [start_iso, end_iso].
    Returns a dict indicating if there is a conflict, and details of the conflicting event if so.
    """
    try:
        service = get_calendar_service()
        req_start = datetime.strptime(start_iso.replace("Z", ""), "%Y-%m-%dT%H:%M:%S")
        req_end = datetime.strptime(end_iso.replace("Z", ""), "%Y-%m-%dT%H:%M:%S")
        
        time_min = (req_start - timedelta(hours=12)).isoformat() + 'Z'
        time_max = (req_end + timedelta(hours=12)).isoformat() + 'Z'
        
        events_result = service.events().list(
            calendarId='primary',
            timeMin=time_min,
            timeMax=time_max,
            singleEvents=True,
            orderBy='startTime'
        ).execute()
        
        events = events_result.get('items', [])
        for event in events:
            if event.get('status') == 'cancelled':
                continue
            start_str = event.get('start', {}).get('dateTime') or event.get('start', {}).get('date')
            end_str = event.get('end', {}).get('dateTime') or event.get('end', {}).get('date')
            if not start_str or not end_str:
                continue
            try:
                exist_start = parse_google_datetime(start_str)
                exist_end = parse_google_datetime(end_str)
            except Exception:
                continue
            
            if exist_start < req_end and exist_end > req_start:
                return {
                    "conflict": True,
                    "event_summary": event.get('summary', 'Untitled Event'),
                    "start": start_str,
                    "end": end_str,
                    "end_iso": exist_end.strftime("%Y-%m-%dT%H:%M:%SZ")
                }
        return {"conflict": False}
    except Exception as e:
        print(f"Error checking conflicts: {e}")
        return {"conflict": False, "error": str(e)}

def create_calendar_event(summary: str, start_iso: str, end_iso: str) -> dict:
    """
    Creates a Google Calendar event and pushes its inverse action 
    (delete_calendar_event) onto the global LIFO Undo Stack.
    """
    try:
        service = get_calendar_service()
        event = {
            'summary': summary,
            'start': {'dateTime': start_iso, 'timeZone': 'UTC'},
            'end': {'dateTime': end_iso, 'timeZone': 'UTC'},
        }
        created_event = service.events().insert(calendarId='primary', body=event).execute()
        event_id = created_event.get('id')
        html_link = created_event.get('htmlLink')

        # Append authuser parameter if context email is set!
        email = active_user_email.get()
        if email and html_link:
            if '?' in html_link:
                html_link += f"&authuser={email}"
            else:
                html_link += f"?authuser={email}"

        # Build inverse action payload for the Undo Stack
        undo_payload = json.dumps({"event_id": event_id})
        global_undo_stack.push(
            action_name=f"create_event:{summary}",
            undo_payload_json=undo_payload
        )

        return {
            "status": "success",
            "message": f"Event '{summary}' created successfully.",
            "event_id": event_id,
            "htmlLink": html_link
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}

def delete_calendar_event(event_id: str) -> dict:
    """Deletes a Google Calendar event given its event_id."""
    try:
        service = get_calendar_service()
        service.events().delete(calendarId='primary', eventId=event_id).execute()
        return {"status": "success", "message": f"Event {event_id} deleted."}
    except Exception as e:
        return {"status": "error", "message": str(e)}