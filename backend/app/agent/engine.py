import json
from typing import Dict, Callable, Any
from datetime import timedelta
from app.agent.parser import parse_user_intent
from app.agent.models import AgentResponse
from app.tools.calendar import create_calendar_event, delete_calendar_event, check_calendar_conflict
from app.tools.reminders import create_reminder, delete_reminder
from app.agent.undo import global_undo_stack

# Router registry dictionary (HashMap) mapping action string to handler function
ACTION_REGISTRY: Dict[str, Callable[[AgentResponse], dict]] = {}

def register_action(action_name: str):
    """Decorator to register action handlers in the HashMap router."""
    def decorator(func: Callable[[AgentResponse], dict]):
        ACTION_REGISTRY[action_name] = func
        return func
    return decorator

@register_action("create_calendar_event")
def handle_create_calendar_event(agent_res: AgentResponse) -> dict:
    params = agent_res.parameters
    if not params.summary or not params.start_iso:
        return {
            "status": "error",
            "action": "create_calendar_event",
            "reply": "I couldn't extract all necessary event details (title or time). Could you specify them?"
        }
    
    start_iso = params.start_iso
    end_iso = params.end_iso
    
    if not end_iso:
        try:
            from datetime import datetime
            clean_start = start_iso.replace("Z", "")
            dt = datetime.strptime(clean_start, "%Y-%m-%dT%H:%M:%S")
            end_dt = dt + timedelta(hours=1)
            end_iso = end_dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        except Exception:
            end_iso = start_iso

    # Calendar Conflict Check
    if not params.double_book:
        conflict_res = check_calendar_conflict(start_iso, end_iso)
        if conflict_res.get("conflict"):
            event_summary = conflict_res["event_summary"]
            conflict_end = conflict_res["end"]
            
            try:
                from app.tools.calendar import parse_google_datetime
                dt_utc = parse_google_datetime(conflict_end)
                use_ist = "ist" in (agent_res.reply_message or "").lower() or "ist" in (params.summary or "").lower()
                
                if use_ist:
                    ist_dt = dt_utc + timedelta(hours=5, minutes=30)
                    alt_time_str = ist_dt.strftime("%I:%M %p")
                    if alt_time_str.startswith('0'):
                        alt_time_str = alt_time_str[1:]
                    alt_time_str += " IST"
                    
                    conf_end_ist = dt_utc + timedelta(hours=5, minutes=30)
                    conf_end_str = conf_end_ist.strftime("%I:%M %p")
                    if conf_end_str.startswith('0'):
                        conf_end_str = conf_end_str[1:]
                    conf_end_str += " IST"
                else:
                    alt_time_str = dt_utc.strftime("%I:%M %p UTC")
                    if alt_time_str.startswith('0'):
                        alt_time_str = alt_time_str[1:]
                    conf_end_str = dt_utc.strftime("%I:%M %p UTC")
                    if conf_end_str.startswith('0'):
                        conf_end_str = conf_end_str[1:]
            except Exception:
                alt_time_str = "later"
                conf_end_str = "a previous event"

            reply = f"You're booked until {conf_end_str}. Want {params.summary} at {alt_time_str} instead, or double-book?"
            return {
                "status": "conflict",
                "action": "create_calendar_event",
                "reply": reply,
                "details": {
                    "conflict": True,
                    "event_summary": event_summary,
                    "alternative_time": alt_time_str,
                    "end_time": conf_end_str
                }
            }

    tool_result = create_calendar_event(
        summary=params.summary,
        start_iso=start_iso,
        end_iso=end_iso
    )
    if isinstance(tool_result, dict):
        tool_result["start_iso"] = start_iso
        tool_result["end_iso"] = end_iso
        
        # Save to database
        from app.db.session import SessionLocal
        from app.db.models import CalendarEvent, Task
        from datetime import datetime
        import uuid
        db = SessionLocal()
        parent_uuid = None
        try:
            db_task_id = uuid.UUID(params.task_id) if params.task_id else None
            db_event = CalendarEvent(
                task_id=db_task_id,
                google_event_id=tool_result["event_id"],
                start_time=datetime.strptime(start_iso.replace("Z", ""), "%Y-%m-%dT%H:%M:%S"),
                end_time=datetime.strptime(end_iso.replace("Z", ""), "%Y-%m-%dT%H:%M:%S")
            )
            db.add(db_event)
            db.commit()
            
            # Fetch parent task ID to group actions
            if db_task_id:
                t_row = db.query(Task).filter(Task.task_id == db_task_id).first()
                if t_row:
                    parent_uuid = t_row.parent_task_id
        except Exception as e:
            db.rollback()
            print(f"Failed to save calendar event to DB: {e}")
        finally:
            db.close()
            
        # Log to actions trail
        from app.agent.actions_logger import log_action
        reason = agent_res.reply_message or f"Booked meeting '{params.summary}'"
        log_action(
            action_type=f"create_calendar_event:{params.summary}",
            action_payload={
                "summary": params.summary,
                "start_iso": start_iso,
                "end_iso": end_iso
            },
            reason=reason,
            undo_action={"event_id": tool_result["event_id"]},
            parent_task_id=parent_uuid
        )
        
    return {
        "status": "success",
        "action": "create_calendar_event",
        "reply": agent_res.reply_message,
        "details": tool_result
    }

@register_action("view_calendar")
def handle_view_calendar(agent_res: AgentResponse) -> dict:
    from app.tools.calendar import active_user_email
    email = active_user_email.get()
    redirect_url = "https://calendar.google.com/calendar/r"
    if email:
        redirect_url = f"https://calendar.google.com/calendar/r?authuser={email}"
    return {
        "status": "success",
        "action": "view_calendar",
        "reply": agent_res.reply_message or "Opening your Google Calendar now.",
        "details": {
            "redirect_url": redirect_url
        }
    }

@register_action("set_reminder")
def handle_set_reminder(agent_res: AgentResponse) -> dict:
    params = agent_res.parameters
    if not params.task:
        return {
            "status": "error",
            "action": "set_reminder",
            "reply": "I couldn't extract the task to remind you about. Could you specify it?"
        }
    
    # default to 30 mins lead time
    lead_time = params.remind_minutes_before if params.remind_minutes_before is not None else 30
    tool_result = create_reminder(
        task=params.task,
        remind_minutes_before=lead_time,
        start_iso=params.start_iso,
        task_id=params.task_id
    )
    if isinstance(tool_result, dict) and tool_result.get("status") == "success":
        from app.db.session import SessionLocal
        from app.db.models import Task
        import uuid
        parent_uuid = None
        db_task_id = uuid.UUID(params.task_id) if params.task_id else None
        if db_task_id:
            db_s = SessionLocal()
            try:
                t_row = db_s.query(Task).filter(Task.task_id == db_task_id).first()
                if t_row:
                    parent_uuid = t_row.parent_task_id
            except Exception:
                pass
            finally:
                db_s.close()
                
        # Log to actions trail
        from app.agent.actions_logger import log_action
        reason = agent_res.reply_message or f"Set reminder for '{params.task}'"
        log_action(
            action_type=f"set_reminder:{params.task}",
            action_payload={
                "task": params.task,
                "remind_minutes_before": lead_time,
                "start_iso": params.start_iso
            },
            reason=reason,
            undo_action={"reminder_id": tool_result["reminder_id"]},
            parent_task_id=parent_uuid
        )
        
    return {
        "status": "success",
        "action": "set_reminder",
        "reply": agent_res.reply_message,
        "details": tool_result
    }

@register_action("undo")
def handle_undo(agent_res: AgentResponse) -> dict:
    from app.agent.actions_logger import undo_last_action
    undo_res = undo_last_action()
    if undo_res.get("status") == "success":
        return {
            "status": "success",
            "action": "undo",
            "reply": undo_res.get("message")
        }
    elif undo_res.get("status") == "info":
        return {
            "status": "info",
            "action": "undo",
            "reply": undo_res.get("message")
        }
    else:
        return {
            "status": "error",
            "action": "undo",
            "reply": undo_res.get("message")
        }

@register_action("conversation")
def handle_conversation(agent_res: AgentResponse) -> dict:
    return {
        "status": "success",
        "action": "conversation",
        "reply": agent_res.reply_message
    }

async def execute_user_request(user_prompt: str, history: list = None) -> dict:
    """Parses user input using Gemini and executes the requested actions using the DAG Graph executor."""
    # 1. Parse intent
    agent_res: AgentResponse = parse_user_intent(user_prompt, history)

    # If there are no tasks parsed, fallback to conversation
    if not agent_res.tasks:
        return {
            "status": "success",
            "action": "conversation",
            "reply": agent_res.reply_message
        }

    # 2. Database Session Initialization & Task/Sub-Task Insertion
    from app.db.session import SessionLocal
    from app.db.models import Task, User
    import uuid

    db_sess = SessionLocal()
    task_id_map = {}
    db_primary_task_id = None
    try:
        # Resolve or create default user in database
        user = db_sess.query(User).first()
        if not user:
            user = User(name="Sanika Choughule", email="sanikarajuchoughule@gmail.com")
            db_sess.add(user)
            db_sess.commit()
            db_sess.refresh(user)

        # Create primary Task representing this user prompt request
        db_primary_task = Task(
            user_id=user.user_id,
            title=user_prompt,
            intent_type=agent_res.action,
            status="executing"
        )
        db_sess.add(db_primary_task)
        db_sess.commit()
        db_sess.refresh(db_primary_task)
        db_primary_task_id = db_primary_task.task_id

        # Create sub-tasks in DB representing decomposed actions and map their task IDs
        for task in agent_res.tasks:
            db_subtask_id = uuid.uuid4()
            task_id_map[task.task_id] = db_subtask_id
            
            db_subtask = Task(
                task_id=db_subtask_id,
                user_id=user.user_id,
                parent_task_id=db_primary_task_id,
                title=task.action,
                intent_type=task.action,
                status="pending"
            )
            db_sess.add(db_subtask)
            
            # Inject real database UUID into parameters so the node handlers receive it
            task.parameters.task_id = str(db_subtask_id)
        db_sess.commit()
    except Exception as e:
        db_sess.rollback()
        print(f"Failed to create tasks in DB: {e}")
    finally:
        db_sess.close()

    # 3. Build the Task Dependency Graph (DAG) using string IDs
    from app.agent.dag_planner import TaskDAG
    dag = TaskDAG()
    
    for task in agent_res.tasks:
        dag.add_node(task.task_id, task.action, task.parameters.dict())
        
    for task in agent_res.tasks:
        for dep_id in task.dependencies:
            dag.add_dependency(dep_id, task.task_id)

    # 4. Define the node execution worker
    async def node_executor(action: str, parameters: dict) -> dict:
        handler = ACTION_REGISTRY.get(action)
        if not handler:
            return {
                "status": "success",
                "action": "conversation",
                "reply": "No specific handler registered."
            }
        
        # Wrap parameters back into EventParameters
        from app.agent.models import EventParameters, TaskDefinition
        params_obj = EventParameters(**parameters)
        
        # Build mock response for the handler
        mock_res = AgentResponse(
            thought=agent_res.thought,
            tasks=[
                TaskDefinition(
                    task_id="task",
                    action=action,
                    parameters=params_obj,
                    dependencies=[]
                )
            ],
            reply_message=agent_res.reply_message
        )
        return handler(mock_res)

    # 5. Execute the DAG in dependency order (runs independent nodes in parallel!)
    dag_result = await dag.execute_dag(node_executor)

    # Update tasks status in database based on execution results
    db_sess = SessionLocal()
    try:
        if 'db_primary_task_id' in locals() and db_primary_task_id:
            # Update primary task status
            db_sess.query(Task).filter(Task.task_id == db_primary_task_id).update({"status": "completed"})
            # Update subtasks status
            for str_id, db_uuid in task_id_map.items():
                node_res = dag_result["results"].get(str_id)
                status_str = "completed"
                if not node_res:
                    status_str = "skipped"
                elif node_res.get("status") == "conflict":
                    status_str = "conflict"
                elif node_res.get("status") == "error":
                    status_str = "failed"
                db_sess.query(Task).filter(Task.task_id == db_uuid).update({"status": status_str})
            db_sess.commit()
    except Exception as e:
        db_sess.rollback()
        print(f"Failed to update task statuses in DB: {e}")
    finally:
        db_sess.close()

    # 6. Compile the final response to the user
    # If any task resulted in a conflict, return the conflict response immediately
    for node_res in dag_result["results"].values():
        if node_res and node_res.get("status") == "conflict":
            return {
                "status": "conflict",
                "action": node_res.get("action"),
                "reply": node_res.get("reply"),
                "results": dag_result["results"]
            }

    # If it's a single task, return its details directly for backward compatibility
    if len(agent_res.tasks) == 1:
        first_task = agent_res.tasks[0]
        node_res = dag_result["results"].get(first_task.task_id, {})
        return node_res

    # Otherwise return the consolidated results
    return {
        "status": dag_result["status"],
        "reply": agent_res.reply_message,
        "results": dag_result["results"]
    }