import logging
import json
import uuid
from sqlalchemy.orm import Session
from app.db.session import SessionLocal
from app.db.models import ActionLog, Task

logger = logging.getLogger(__name__)

class DatabaseUndoStack:
    def push(self, action_name: str, undo_payload_json: str):
        db: Session = SessionLocal()
        try:
            # Associate with the most recent task
            last_task = db.query(Task).order_by(Task.created_at.desc()).first()
            if not last_task:
                # Fallback user & task
                from app.db.models import User
                user = db.query(User).first()
                if not user:
                    user = User(name="Default User", email="default@example.com")
                    db.add(user)
                    db.commit()
                    db.refresh(user)
                last_task = Task(user_id=user.user_id, title="Legacy Action")
                db.add(last_task)
                db.commit()
                db.refresh(last_task)

            log_entry = ActionLog(
                task_id=last_task.task_id,
                action_type=action_name,
                explanation_text=undo_payload_json,
                is_undoable=True
            )
            db.add(log_entry)
            db.commit()
            print(f"[SQL STACK PUSH] Registered action: {action_name}")
        except Exception as e:
            db.rollback()
            logger.error(f"Failed to push action to DB log: {e}")
        finally:
            db.close()

    def pop_and_undo(self) -> dict:
        db: Session = SessionLocal()
        try:
            # Get the most recent undoable action log entry
            top_log = db.query(ActionLog).filter(ActionLog.is_undoable == True).order_by(ActionLog.timestamp.desc()).first()
            if not top_log:
                return {"status": "empty", "message": "No actions available to undo."}
            
            action_name = top_log.action_type
            undo_payload = top_log.explanation_text
            
            db.delete(top_log)
            db.commit()
            
            print(f"[SQL STACK POP] Popped action: {action_name}")
            return {
                "status": "success",
                "action_name": action_name,
                "undo_payload_json": undo_payload
            }
        except Exception as e:
            db.rollback()
            logger.error(f"Failed to pop action from DB log: {e}")
            return {"status": "error", "message": str(e)}
        finally:
            db.close()

    def peek(self) -> str:
        db: Session = SessionLocal()
        try:
            top_log = db.query(ActionLog).filter(ActionLog.is_undoable == True).order_by(ActionLog.timestamp.desc()).first()
            if not top_log:
                return "Empty"
            return top_log.action_type
        finally:
            db.close()

global_undo_stack = DatabaseUndoStack()
