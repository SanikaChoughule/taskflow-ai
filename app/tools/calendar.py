import os.path
import json
from datetime import datetime, timedelta
from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from app.agent.undo import global_undo_stack

SCOPES = ['https://www.googleapis.com/auth/calendar']

def get_calendar_service():
    """Authenticates and returns the Google Calendar API service instance."""
    creds = None
    if os.path.exists('token.json'):
        creds = Credentials.from_authorized_user_file('token.json', SCOPES)
    
    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(Request())
        else:
            if not os.path.exists('credentials.json'):
                raise FileNotFoundError(
                    "credentials.json not found! Download it from Google Cloud Console "
                    "and place it in the backend/ folder."
                )
            flow = InstalledAppFlow.from_client_secrets_file('credentials.json', SCOPES)
            creds = flow.run_local_server(port=0)
            
        with open('token.json', 'w') as token:
            token.write(creds.to_json())

    return build('calendar', 'v3', credentials=creds)

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
            "htmlLink": created_event.get('htmlLink')
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