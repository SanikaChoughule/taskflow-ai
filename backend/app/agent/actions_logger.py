import json
import uuid
import logging
from datetime import datetime
from app.db.session import SessionLocal
from app.db.models import Action, Task
from app.tools.calendar import active_user_id, delete_calendar_event
from app.tools.reminders import delete_reminder

logger = logging.getLogger(__name__)

def log_action(action_type: str, action_payload: dict, reason: str, undo_action: dict, parent_task_id: uuid.UUID = None):
    """
    Logs an action to the database actions trail.
    """
    db = SessionLocal()
    try:
        user_uuid = None
        user_id_str = active_user_id.get()
        if user_id_str:
            try:
                user_uuid = uuid.UUID(user_id_str)
            except Exception:
                pass

        action_entry = Action(
            action_type=action_type,
            action_payload=json.dumps(action_payload) if action_payload else None,
            reason=reason,
            undo_action=json.dumps(undo_action) if undo_action else None,
            status="active",
            user_id=user_uuid,
            parent_task_id=parent_task_id
        )
        db.add(action_entry)
        db.commit()
        logger.info(f"[ACTION LOG] Recorded: {action_type} for User {user_id_str}")
    except Exception as e:
        db.rollback()
        logger.error(f"Failed to log action: {e}")
    finally:
        db.close()

def undo_last_action() -> dict:
    """
    Finds the most recent active action for the current user and reverses it.
    If multiple actions share the same parent_task_id, it reverses all of them atomically.
    """
    db = SessionLocal()
    try:
        user_uuid = None
        user_id_str = active_user_id.get()
        if user_id_str:
            try:
                user_uuid = uuid.UUID(user_id_str)
            except Exception:
                pass

        # Query the most recent active action for the user
        query = db.query(Action).filter(Action.status == "active")
        if user_uuid:
            query = query.filter(Action.user_id == user_uuid)
        
        top_action = query.order_by(Action.timestamp.desc()).first()
        if not top_action:
            return {
                "status": "info",
                "message": "No previous actions available to undo."
            }

        # If it belongs to a grouped parent task execution command, find all active sibling actions
        actions_to_undo = []
        if top_action.parent_task_id:
            actions_to_undo = db.query(Action).filter(
                Action.parent_task_id == top_action.parent_task_id,
                Action.status == "active"
            ).order_by(Action.timestamp.desc()).all()
        else:
            actions_to_undo = [top_action]

        undone_summaries = []
        failed_summaries = []

        for act in actions_to_undo:
            # Mark status as undone
            act.status = "undone"
            db.commit()

            # Execute reverse payload
            undo_payload = {}
            if act.undo_action:
                try:
                    undo_payload = json.loads(act.undo_action)
                except Exception:
                    pass

            action_type = act.action_type
            if action_type.startswith("create_calendar_event") or "event" in action_type:
                event_id = undo_payload.get("event_id")
                event_name = action_type.split(":", 1)[1] if ":" in action_type else "Calendar Event"
                if event_id:
                    delete_res = delete_calendar_event(event_id)
                    err_msg = delete_res.get("message", "").lower() if delete_res.get("message") else ""
                    if delete_res.get("status") == "success" or "404" in err_msg or "410" in err_msg or "not found" in err_msg or "deleted" in err_msg:
                        undone_summaries.append(f"Deleted calendar event '{event_name}'")
                    else:
                        failed_summaries.append(f"Failed to delete event '{event_name}': {delete_res.get('message')}")
                else:
                    failed_summaries.append(f"Failed to delete event '{event_name}' (missing ID)")

            elif action_type.startswith("set_reminder") or "reminder" in action_type:
                reminder_id = undo_payload.get("reminder_id")
                task_name = action_type.split(":", 1)[1] if ":" in action_type else "Reminder"
                if reminder_id:
                    delete_res = delete_reminder(reminder_id)
                    err_msg = delete_res.get("message", "").lower() if delete_res.get("message") else ""
                    if delete_res.get("status") == "success" or "not found" in err_msg or "404" in err_msg or "410" in err_msg or "deleted" in err_msg:
                        undone_summaries.append(f"Deleted reminder for '{task_name}'")
                    else:
                        failed_summaries.append(f"Failed to delete reminder '{task_name}': {delete_res.get('message')}")
                else:
                    failed_summaries.append(f"Failed to delete reminder '{task_name}' (missing ID)")

        if not undone_summaries and failed_summaries:
            return {
                "status": "error",
                "message": "; ".join(failed_summaries)
            }

        reply_msg = "Successfully undone: " + ", and ".join(undone_summaries) + "."
        if failed_summaries:
            reply_msg += " (Warning: " + "; ".join(failed_summaries) + ")"
            
        return {
            "status": "success",
            "message": reply_msg
        }

    except Exception as e:
        db.rollback()
        logger.error(f"Error during undo execution: {e}")
        return {
            "status": "error",
            "message": f"An error occurred while undoing: {str(e)}"
        }
    finally:
        db.close()

def undo_specific_action(action_id: uuid.UUID) -> dict:
    """
    Finds a specific action by ID and reverses it.
    If it belongs to a grouped parent task, it reverses all active sibling actions for that parent_task_id.
    """
    db = SessionLocal()
    try:
        target_action = db.query(Action).filter(Action.id == action_id).first()
        if not target_action:
            return {
                "status": "error",
                "message": "Action not found."
            }
            
        if target_action.status == "undone":
            return {
                "status": "info",
                "message": "This action has already been undone."
            }

        # If it belongs to a grouped parent task execution command, find all active sibling actions
        actions_to_undo = []
        if target_action.parent_task_id:
            actions_to_undo = db.query(Action).filter(
                Action.parent_task_id == target_action.parent_task_id,
                Action.status == "active"
            ).order_by(Action.timestamp.desc()).all()
        else:
            actions_to_undo = [target_action]

        undone_summaries = []
        failed_summaries = []

        for act in actions_to_undo:
            undo_payload = {}
            if act.undo_action:
                try:
                    undo_payload = json.loads(act.undo_action)
                except Exception:
                    pass

            action_type = act.action_type
            if action_type.startswith("create_calendar_event") or "event" in action_type:
                event_id = undo_payload.get("event_id")
                event_name = action_type.split(":", 1)[1] if ":" in action_type else "Calendar Event"
                if event_id:
                    delete_res = delete_calendar_event(event_id)
                    err_msg = delete_res.get("message", "").lower() if delete_res.get("message") else ""
                    if delete_res.get("status") == "success" or "404" in err_msg or "410" in err_msg or "not found" in err_msg or "deleted" in err_msg:
                        undone_summaries.append(f"Deleted calendar event '{event_name}'")
                    else:
                        failed_summaries.append(f"Failed to delete event '{event_name}': {delete_res.get('message')}")
                else:
                    failed_summaries.append(f"Failed to delete event '{event_name}' (missing ID)")

            elif action_type.startswith("set_reminder") or "reminder" in action_type:
                reminder_id = undo_payload.get("reminder_id")
                task_name = action_type.split(":", 1)[1] if ":" in action_type else "Reminder"
                if reminder_id:
                    delete_res = delete_reminder(reminder_id)
                    err_msg = delete_res.get("message", "").lower() if delete_res.get("message") else ""
                    if delete_res.get("status") == "success" or "not found" in err_msg or "404" in err_msg or "410" in err_msg or "deleted" in err_msg:
                        undone_summaries.append(f"Deleted reminder for '{task_name}'")
                    else:
                        failed_summaries.append(f"Failed to delete reminder '{task_name}': {delete_res.get('message')}")
                else:
                    failed_summaries.append(f"Failed to delete reminder '{task_name}' (missing ID)")

        if undone_summaries:
            for act in actions_to_undo:
                act.status = "undone"
            db.commit()

        if not undone_summaries and failed_summaries:
            return {
                "status": "error",
                "message": "; ".join(failed_summaries)
            }

        reply_msg = "Successfully undone: " + ", and ".join(undone_summaries) + "."
        if failed_summaries:
            reply_msg += " (Warning: " + "; ".join(failed_summaries) + ")"
            
        return {
            "status": "success",
            "message": reply_msg
        }

    except Exception as e:
        db.rollback()
        logger.error(f"Error during specific undo execution: {e}")
        return {
            "status": "error",
            "message": f"An error occurred while undoing: {str(e)}"
        }
    finally:
        db.close()
