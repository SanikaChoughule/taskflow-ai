import os
import json
import uuid
import time
import calendar
from datetime import datetime
from sqlalchemy.orm import Session
from app.db.session import SessionLocal
from app.db.models import Reminder, Task, User
from app.agent.undo import global_undo_stack

def list_reminders() -> list:
    """Returns a list of all active task reminders from the SQL database."""
    db: Session = SessionLocal()
    try:
        reminders = db.query(Reminder).filter(Reminder.delivered == False).all()
        result = []
        for r in reminders:
            task_title = r.task.title if r.task else "Untitled Task"
            trigger_at_iso = r.trigger_time.strftime("%Y-%m-%dT%H:%M:%SZ")
            result.append({
                "id": str(r.reminder_id),
                "task": task_title,
                "remind_minutes_before": 30,
                "created_at": datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
                "trigger_at": trigger_at_iso
            })
        return result
    finally:
        db.close()

def create_reminder(task: str, remind_minutes_before: int = 30, start_iso: str = None, task_id: str = None) -> dict:
    """Creates a new task reminder in the SQL database."""
    db: Session = SessionLocal()
    try:
        # 1. Resolve Task ID
        db_task_id = None
        if task_id:
            db_task_id = uuid.UUID(task_id)
        else:
            # Fallback user and task
            user = db.query(User).first()
            if not user:
                user = User(name="Default User", email="default@example.com")
                db.add(user)
                db.commit()
                db.refresh(user)
            
            fallback_task = Task(user_id=user.user_id, title=task)
            db.add(fallback_task)
            db.commit()
            db.refresh(fallback_task)
            db_task_id = fallback_task.task_id

        # 2. Calculate trigger_time
        if start_iso:
            try:
                iso_clean = start_iso.replace("Z", "")
                dt = datetime.strptime(iso_clean, "%Y-%m-%dT%H:%M:%S")
                event_ts = calendar.timegm(dt.utctimetuple())
                trigger_time = event_ts - (remind_minutes_before * 60)
            except Exception:
                trigger_time = int(time.time()) + (remind_minutes_before * 60)
        else:
            trigger_time = int(time.time()) + (remind_minutes_before * 60)

        trigger_dt = datetime.utcfromtimestamp(trigger_time)
        trigger_at_iso = trigger_dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        reminder_uuid = uuid.uuid4()

        # 3. Save to database
        db_reminder = Reminder(
            reminder_id=reminder_uuid,
            task_id=db_task_id,
            trigger_time=trigger_dt,
            channel="push",
            delivered=False
        )
        db.add(db_reminder)
        db.commit()

        # 4. Push to Priority Queue scheduler
        from app.agent.scheduler import global_reminder_queue
        global_reminder_queue.push(
            trigger_time=trigger_time,
            reminder_id=str(reminder_uuid),
            task=task
        )

        # 5. Build inverse action payload for the Undo Stack
        undo_payload = json.dumps({"reminder_id": str(reminder_uuid)})
        global_undo_stack.push(
            action_name=f"create_reminder:{task}",
            undo_payload_json=undo_payload
        )

        return {
            "status": "success",
            "message": f"Reminder set: '{task}' ({remind_minutes_before} minutes prior).",
            "reminder": {
                "id": str(reminder_uuid),
                "task": task,
                "remind_minutes_before": remind_minutes_before,
                "created_at": datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
                "trigger_at": trigger_at_iso
            }
        }
    except Exception as e:
        db.rollback()
        return {"status": "error", "message": str(e)}
    finally:
        db.close()

def delete_reminder(reminder_id: str) -> dict:
    """Deletes a task reminder by its ID in the SQL database."""
    db: Session = SessionLocal()
    try:
        rem_uuid = uuid.UUID(reminder_id)
        db_reminder = db.query(Reminder).filter(Reminder.reminder_id == rem_uuid).first()
        if not db_reminder:
            return {"status": "error", "message": f"Reminder {reminder_id} not found."}
        
        db.delete(db_reminder)
        db.commit()
        return {"status": "success", "message": f"Reminder {reminder_id} deleted."}
    except Exception as e:
        db.rollback()
        return {"status": "error", "message": str(e)}
    finally:
        db.close()
