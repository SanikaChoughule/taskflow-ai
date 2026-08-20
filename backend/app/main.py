from fastapi import FastAPI, HTTPException, Cookie, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional
import contextlib
import asyncio
import os
import uuid
from datetime import datetime
from fastapi.responses import HTMLResponse, RedirectResponse
from app.agent.engine import execute_user_request
from app.tools.calendar import list_calendar_events
from app.tools.reminders import list_reminders, create_reminder, delete_reminder
from app.agent.scheduler import start_reminder_scheduler, start_proactive_calendar_checker
from google_auth_oauthlib.flow import Flow
from pathlib import Path

from app.db.session import engine, SessionLocal
from app.db.models import Base, User, Session as DBSession, Action

# Allow HTTP callback redirects locally (loopback)
os.environ['OAUTHLIB_INSECURE_TRANSPORT'] = '1'

BACKEND_DIR = Path(__file__).resolve().parent.parent
CREDENTIALS_PATH = BACKEND_DIR / "credentials.json"
TOKEN_PATH = BACKEND_DIR / "token.json"
SCOPES = [
    'https://www.googleapis.com/auth/calendar',
    'https://www.googleapis.com/auth/userinfo.profile',
    'https://www.googleapis.com/auth/userinfo.email',
    'openid'
]

@contextlib.asynccontextmanager
async def lifespan(app: FastAPI):
    # Auto-initialize SQLite database tables
    Base.metadata.create_all(bind=engine)
    
    # Dynamic database migration: Ensure users table has profile_pic column
    from sqlalchemy import text
    with engine.connect() as conn:
        try:
            conn.execute(text("SELECT profile_pic FROM users LIMIT 1"))
        except Exception:
            try:
                conn.execute(text("ALTER TABLE users ADD COLUMN profile_pic VARCHAR(500)"))
                conn.commit()
                print("Successfully migrated: Added profile_pic column to users table.")
            except Exception as e:
                print(f"Database migration failed: {e}")

    # Start the Priority Queue background scheduler task
    scheduler_task = asyncio.create_task(start_reminder_scheduler())
    # Start the Proactive Calendar Overrun Checker background task
    proactive_task = asyncio.create_task(start_proactive_calendar_checker())
    yield
    # Cleanup task on shutdown
    scheduler_task.cancel()
    proactive_task.cancel()
    try:
        await asyncio.gather(scheduler_task, proactive_task, return_exceptions=True)
    except Exception:
        pass

app = FastAPI(title="TaskFlow AI Backend", lifespan=lifespan)

# Enable CORS for React frontend integration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class Message(BaseModel):
    role: str
    content: str

class ChatRequest(BaseModel):
    prompt: str
    history: Optional[list[Message]] = None

class ReminderCreateRequest(BaseModel):
    task: str
    remind_minutes_before: Optional[int] = 30

@app.get("/")
def read_root():
    return RedirectResponse(url="/dashboard")

@app.get("/login", response_class=HTMLResponse)
def get_login():
    file_path = os.path.join(os.path.dirname(__file__), "templates", "login.html")
    if os.path.exists(file_path):
        with open(file_path, "r", encoding="utf-8") as f:
            return HTMLResponse(content=f.read())
    return HTMLResponse(content="<h1>Login Page Template Not Found</h1>", status_code=404)

@app.get("/dashboard", response_class=HTMLResponse)
def get_dashboard(session_id: Optional[str] = Cookie(None)):
    if not session_id:
        return RedirectResponse(url="/login")
    
    db = SessionLocal()
    try:
        sess_uuid = uuid.UUID(session_id)
        db_session = db.query(DBSession).filter(DBSession.session_id == sess_uuid).first()
        if not db_session:
            return RedirectResponse(url="/login")
        # Update last active timestamp
        db_session.last_active = datetime.utcnow()
        db.commit()
    except Exception:
        return RedirectResponse(url="/login")
    finally:
        db.close()
        
    file_path = os.path.join(os.path.dirname(__file__), "templates", "dashboard.html")
    if os.path.exists(file_path):
        with open(file_path, "r", encoding="utf-8") as f:
            return HTMLResponse(content=f.read())
    return HTMLResponse(content="<h1>Dashboard Template Not Found</h1>", status_code=404)

def setup_user_context(session_id: Optional[str]):
    if not session_id:
        return
    db = SessionLocal()
    try:
        sess_uuid = uuid.UUID(session_id)
        db_session = db.query(DBSession).filter(DBSession.session_id == sess_uuid).first()
        if db_session and db_session.user:
            user = db_session.user
            from app.tools.calendar import active_user_token, active_user_id, active_user_email
            active_user_token.set(user.google_oauth_token)
            active_user_id.set(str(user.user_id))
            active_user_email.set(user.email)
    except Exception as e:
        print(f"Failed to setup user context: {e}")
    finally:
        db.close()

@app.post("/api/chat")
async def chat_endpoint(request: ChatRequest, session_id: Optional[str] = Cookie(None)):
    if not request.prompt.strip():
        raise HTTPException(status_code=400, detail="Prompt cannot be empty.")
    
    setup_user_context(session_id)
    
    history_list = []
    if request.history:
        history_list = [h.dict() for h in request.history]
        
    result = await execute_user_request(request.prompt, history_list)
    return result

@app.get("/api/calendar/events")
def get_calendar_events(max_results: int = 10, session_id: Optional[str] = Cookie(None)):
    try:
        setup_user_context(session_id)
        events = list_calendar_events(max_results=max_results)
        return {"status": "success", "events": events}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to list calendar events: {str(e)}")

@app.get("/api/proactive-suggestions")
def get_proactive_suggestions(session_id: Optional[str] = Cookie(None)):
    if not session_id:
        raise HTTPException(status_code=401, detail="Unauthorized")
    db = SessionLocal()
    user_id_str = None
    try:
        sess_uuid = uuid.UUID(session_id)
        db_session = db.query(DBSession).filter(DBSession.session_id == sess_uuid).first()
        if db_session and db_session.user:
            user_id_str = str(db_session.user.user_id)
    except Exception:
        pass
    finally:
        db.close()
        
    if not user_id_str:
        return {"status": "success", "suggestions": []}
        
    from app.agent.scheduler import proactive_suggestions
    user_suggestions = proactive_suggestions.pop(user_id_str, [])
    return {"status": "success", "suggestions": user_suggestions}

@app.get("/api/reminders")
def get_reminders():
    try:
        reminders = list_reminders()
        return {"status": "success", "reminders": reminders}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to list reminders: {str(e)}")

@app.post("/api/reminders")
def post_reminder(request: ReminderCreateRequest):
    if not request.task.strip():
        raise HTTPException(status_code=400, detail="Task details cannot be empty.")
    try:
        result = create_reminder(task=request.task, remind_minutes_before=request.remind_minutes_before)
        if result.get("status") == "success":
            return result
        else:
            raise HTTPException(status_code=500, detail=result.get("message", "Failed to create reminder."))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to create reminder: {str(e)}")

@app.delete("/api/reminders/{reminder_id}")
def remove_reminder(reminder_id: str):
    try:
        result = delete_reminder(reminder_id)
        if result.get("status") == "success":
            return result
        else:
            raise HTTPException(status_code=404, detail=result.get("message", "Reminder not found."))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to delete reminder: {str(e)}")

@app.get("/api/actions")
def get_actions(session_id: Optional[str] = Cookie(None)):
    setup_user_context(session_id)
    from app.tools.calendar import active_user_id
    user_id_str = active_user_id.get()
    if not user_id_str:
        return {"status": "success", "actions": []}
        
    db = SessionLocal()
    try:
        user_uuid = uuid.UUID(user_id_str)
        actions = db.query(Action).filter(Action.user_id == user_uuid).order_by(Action.timestamp.desc()).all()
        
        result_list = []
        for action in actions:
            import json
            payload = {}
            if action.action_payload:
                try:
                    payload = json.loads(action.action_payload)
                except Exception:
                    pass
            
            result_list.append({
                "id": str(action.id),
                "timestamp": action.timestamp.isoformat() if action.timestamp else None,
                "action_type": action.action_type,
                "payload": payload,
                "reason": action.reason,
                "status": action.status
            })
        return {"status": "success", "actions": result_list}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch actions: {str(e)}")
    finally:
        db.close()

@app.post("/api/actions/{action_id}/undo")
def post_action_undo(action_id: str, session_id: Optional[str] = Cookie(None)):
    setup_user_context(session_id)
    from app.agent.actions_logger import undo_specific_action
    try:
        action_uuid = uuid.UUID(action_id)
        result = undo_specific_action(action_uuid)
        if result.get("status") == "success":
            return result
        else:
            raise HTTPException(status_code=400, detail=result.get("message", "Failed to undo action."))
    except HTTPException as he:
        raise he
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to undo specific action: {str(e)}")

@app.get("/api/auth/google")
def auth_google(request: Request, login_hint: Optional[str] = None):
    host = request.headers.get("host", "localhost:8000")
    redirect_uri = f"http://{host}/oauth2callback"
    
    flow = Flow.from_client_secrets_file(
        str(CREDENTIALS_PATH),
        scopes=SCOPES,
        redirect_uri=redirect_uri
    )
    
    authorization_url, state = flow.authorization_url(
        access_type='offline',
        include_granted_scopes='true',
        login_hint=login_hint,
        prompt='consent'
    )
    
    # Save code_verifier in a cookie for stateless validation on callback
    response = RedirectResponse(url=authorization_url)
    if hasattr(flow, 'code_verifier') and flow.code_verifier:
        response.set_cookie(key="oauth_code_verifier", value=flow.code_verifier, max_age=600, path="/", httponly=True)
    return response

@app.get("/oauth2callback")
def oauth2callback(request: Request, code: str, state: Optional[str] = None, oauth_code_verifier: Optional[str] = Cookie(None)):
    try:
        host = request.headers.get("host", "localhost:8000")
        redirect_uri = f"http://{host}/oauth2callback"
        
        flow = Flow.from_client_secrets_file(
            str(CREDENTIALS_PATH),
            scopes=SCOPES,
            redirect_uri=redirect_uri,
            state=state
        )
        
        # Exchange authorization code using the code verifier
        flow.fetch_token(code=code, code_verifier=oauth_code_verifier)
        creds = flow.credentials
        
        # Get user email, name, and profile picture from Google Userinfo details
        from googleapiclient.discovery import build
        profile_pic = None
        try:
            oauth_service = build('oauth2', 'v2', credentials=creds)
            user_info = oauth_service.userinfo().get().execute()
            email = user_info.get("email")
            name = user_info.get("name", "Sanika Choughule")
            profile_pic = user_info.get("picture")
        except Exception as e:
            print(f"Failed to fetch userinfo details: {e}")
            email = "sanikarajuchoughule@gmail.com"
            name = "Sanika Choughule"

        # Save user and session to DB
        session_id = "default"
        db = SessionLocal()
        try:
            db_user = db.query(User).filter(User.email == email).first()
            if not db_user:
                db_user = User(
                    name=name,
                    email=email,
                    google_oauth_token=creds.to_json(),
                    profile_pic=profile_pic
                )
                db.add(db_user)
                db.commit()
                db.refresh(db_user)
            else:
                db_user.google_oauth_token = creds.to_json()
                db_user.name = name
                if profile_pic:
                    db_user.profile_pic = profile_pic
                db.commit()
                
            db_session = DBSession(
                user_id=db_user.user_id,
                device_type=request.headers.get("user-agent", "Browser Client")
            )
            db.add(db_session)
            db.commit()
            db.refresh(db_session)
            session_id = str(db_session.session_id)
        except Exception as e:
            db.rollback()
            print(f"Failed to write user session: {e}")
            session_id = str(uuid.uuid4()) # fallback session ID if DB write fails
        finally:
            db.close()
            
        # Redirect to dashboard and set session cookie
        response = RedirectResponse(url="/dashboard")
        response.set_cookie(key="session_id", value=session_id, max_age=86400, path="/")
        return response
    except Exception as ex:
        import traceback
        tb = traceback.format_exc()
        print(tb)
        return HTMLResponse(content=f"<h1>OAuth Callback Error</h1><pre>{tb}</pre>", status_code=500)

@app.get("/api/user/profile")
def get_user_profile(session_id: Optional[str] = Cookie(None)):
    if not session_id:
        raise HTTPException(status_code=401, detail="Unauthorized")
    db = SessionLocal()
    try:
        sess_uuid = uuid.UUID(session_id)
        db_session = db.query(DBSession).filter(DBSession.session_id == sess_uuid).first()
        if not db_session or not db_session.user:
            raise HTTPException(status_code=401, detail="Unauthorized")
        user = db_session.user
        first_name = user.name.split()[0] if user.name else "Sanika"
        return {
            "status": "success",
            "name": user.name,
            "first_name": first_name,
            "email": user.email,
            "profile_pic": user.profile_pic or "/static/default_avatar.png"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        db.close()