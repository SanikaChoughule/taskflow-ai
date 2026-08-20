import logging
import heapq
import time
import asyncio

logger = logging.getLogger(__name__)

# Try to load the compiled C++ Priority Queue, with a pure Python fallback
try:
    from app.agent.dsa_engine import ReminderPriorityQueueCPP
    logger.info("Using compiled C++ ReminderPriorityQueue (dsa_engine).")
except ImportError:
    logger.warning("C++ ReminderPriorityQueue extension not found or compiled. Falling back to pure Python implementation.")
    class ReminderPriorityQueueCPP:
        def __init__(self):
            # Element format: (trigger_time, reminder_id, task)
            self._queue = []

        def push(self, trigger_time: int, reminder_id: str, task: str):
            # heapq is a min-heap by default, ordering by the first element of the tuple (trigger_time)
            heapq.heappush(self._queue, (trigger_time, reminder_id, task))
            print(f"[Python PQ PUSH] Registered reminder: {task} at {trigger_time}")

        def pop(self) -> dict:
            if not self._queue:
                return {"status": "empty"}
            trigger_time, reminder_id, task = heapq.heappop(self._queue)
            return {
                "status": "success",
                "trigger_time": trigger_time,
                "reminder_id": reminder_id,
                "task": task
            }

        def peek(self) -> dict:
            if not self._queue:
                return {"status": "empty"}
            trigger_time, reminder_id, task = self._queue[0]
            return {
                "status": "success",
                "trigger_time": trigger_time,
                "reminder_id": reminder_id,
                "task": task
            }

        def size(self) -> int:
            return len(self._queue)

        def empty(self) -> bool:
            return len(self._queue) == 0

# Instantiate the global active priority queue
global_reminder_queue = ReminderPriorityQueueCPP()

async def start_reminder_scheduler():
    """Background loop that ticks every few seconds and checks for due reminders."""
    logger.info("Starting Reminder Scheduler background task loop.")
    
    # Repopulate the Priority Queue from persistent reminders.json on startup
    try:
        from app.tools.reminders import list_reminders
        from datetime import datetime, timezone
        active_reminders = list_reminders()
        now_ts = int(time.time())
        for rem in active_reminders:
            try:
                if "trigger_at" in rem:
                    trigger_dt = datetime.strptime(rem["trigger_at"], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)
                    trigger_time = int(trigger_dt.timestamp())
                else:
                    # reminders.json created_at is stored in UTC: YYYY-MM-DDTHH:MM:SSZ
                    created_dt = datetime.strptime(rem["created_at"], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)
                    created_ts = int(created_dt.timestamp())
                    trigger_time = created_ts + (rem.get("remind_minutes_before", 30) * 60)
                
                # Only enqueue if it hasn't expired yet
                if trigger_time > now_ts:
                    global_reminder_queue.push(
                        trigger_time=trigger_time,
                        reminder_id=rem["id"],
                        task=rem["task"]
                    )
                    logger.info(f"Repopulated reminder '{rem['task']}' in queue to trigger at {trigger_time}.")
            except Exception as ex:
                logger.error(f"Failed to parse stored reminder timestamp: {ex}")
    except Exception as ex:
        logger.error(f"Failed to load persistent reminders on scheduler startup: {ex}")
        
    while True:
        try:
            if not global_reminder_queue.empty():
                top = global_reminder_queue.peek()
                if top.get("status") == "success":
                    now = int(time.time())
                    trigger_time = top["trigger_time"]
                    if now >= trigger_time:
                        # Pop the reminder as it is due!
                        due_reminder = global_reminder_queue.pop()
                        if due_reminder.get("status") == "success":
                            logger.info(f"[REMINDER DUE] [ID: {due_reminder['reminder_id']}] Task: {due_reminder['task']}")
                            print(f"\n[SCHEDULER ALERT] Reminder Due! Task: '{due_reminder['task']}' (ID: {due_reminder['reminder_id']})\n", flush=True)
                            
                            # Update delivered status in database
                            from app.db.session import SessionLocal
                            from app.db.models import Reminder
                            import uuid
                            db = SessionLocal()
                            try:
                                rem_uuid = uuid.UUID(due_reminder['reminder_id'])
                                db.query(Reminder).filter(Reminder.reminder_id == rem_uuid).update({"delivered": True})
                                db.commit()
                            except Exception as ex:
                                db.rollback()
                                logger.error(f"Failed to mark reminder as delivered in DB: {ex}")
                            finally:
                                db.close()
            await asyncio.sleep(2)  # Tick every 2 seconds
        except asyncio.CancelledError:
            logger.info("Reminder Scheduler background task loop stopped.")
            break
        except Exception as e:
            logger.error(f"Error in Reminder Scheduler: {e}")
            await asyncio.sleep(5)

# In-memory dictionary caching active proactive suggestions by user_id string
proactive_suggestions = {}

async def start_proactive_calendar_checker():
    """Background loop that polls Google Calendar events for active users to detect overrunning meetings."""
    import uuid
    from datetime import datetime, timezone, timedelta
    from app.db.session import SessionLocal
    from app.db.models import User, Session as DBSession
    from app.tools.calendar import (
        parse_google_datetime,
        active_user_token,
        active_user_id,
        active_user_email
    )
    
    logger.info("Starting Proactive Calendar Checker background task loop.")
    await asyncio.sleep(10)  # Wait for startup to complete
    
    alerted_overruns = set()
    
    while True:
        try:
            db = SessionLocal()
            # Query sessions that were active in the last 15 minutes
            fifteen_mins_ago = datetime.utcnow() - timedelta(minutes=15)
            active_sessions = db.query(DBSession).filter(DBSession.last_active >= fifteen_mins_ago).all()
            
            for sess in active_sessions:
                user = sess.user
                if not user or not user.google_oauth_token:
                    continue
                    
                # Set credentials context variables for the current thread context
                token_token = active_user_token.set(user.google_oauth_token)
                token_id = active_user_id.set(str(user.user_id))
                token_email = active_user_email.set(user.email)
                
                try:
                    # Query events starting 1 hour ago up to 1 hour from now
                    from app.tools.calendar import get_calendar_service
                    service = get_calendar_service()
                    now_dt = datetime.utcnow()
                    time_min = (now_dt - timedelta(hours=1)).isoformat() + 'Z'
                    time_max = (now_dt + timedelta(hours=1)).isoformat() + 'Z'
                    
                    events_result = service.events().list(
                        calendarId='primary',
                        timeMin=time_min,
                        timeMax=time_max,
                        singleEvents=True,
                        orderBy='startTime'
                    ).execute()
                    events = events_result.get('items', [])
                    
                    # Filter out cancelled events
                    events = [e for e in events if e.get('status') != 'cancelled']
                    
                    # Check for overrun conflicts
                    for current_event in events:
                        start_str = current_event.get('start', {}).get('dateTime') or current_event.get('start', {}).get('date')
                        end_str = current_event.get('end', {}).get('dateTime') or current_event.get('end', {}).get('date')
                        if not start_str or not end_str:
                            continue
                            
                        current_start = parse_google_datetime(start_str)
                        current_end = parse_google_datetime(end_str)
                        
                        # Check if current meeting scheduled end has passed:
                        # e.g., current_end < now_dt <= current_end + 15 minutes
                        if current_start <= now_dt and current_end < now_dt <= (current_end + timedelta(minutes=15)):
                            # Check if there is a next meeting starting soon:
                            # starts in the range [now_dt - 5 mins, now_dt + 15 mins]
                            for next_event in events:
                                if next_event['id'] == current_event['id']:
                                    continue
                                next_start_str = next_event.get('start', {}).get('dateTime') or next_event.get('start', {}).get('date')
                                if not next_start_str:
                                    continue
                                next_start = parse_google_datetime(next_start_str)
                                
                                if (now_dt - timedelta(minutes=5)) <= next_start <= (now_dt + timedelta(minutes=15)):
                                    # Overrun conflict detected!
                                    conflict_key = (str(user.user_id), current_event['id'], next_event['id'])
                                    if conflict_key not in alerted_overruns:
                                        msg = f"Your meeting '{current_event.get('summary', 'Current Meeting')}' has run over. Next meeting '{next_event.get('summary', 'Upcoming Meeting')}' is starting soon."
                                        
                                        user_id_str = str(user.user_id)
                                        if user_id_str not in proactive_suggestions:
                                            proactive_suggestions[user_id_str] = []
                                        
                                        proactive_suggestions[user_id_str].append({
                                            "id": f"overrun_{uuid.uuid4()}",
                                            "message": msg,
                                            "created_at": datetime.utcnow().isoformat() + 'Z'
                                        })
                                        
                                        alerted_overruns.add(conflict_key)
                                        logger.info(f"[PROACTIVE OVERRUN ALERT] User: {user.email}, Alert: {msg}")
                                        print(f"\n[PROACTIVE OVERRUN ALERT] User: {user.email}, Alert: {msg}\n", flush=True)
                                        
                        # Check for back-to-back meetings (Rule 2)
                        for next_event in events:
                            if next_event['id'] == current_event['id']:
                                continue
                            next_start_str = next_event.get('start', {}).get('dateTime') or next_event.get('start', {}).get('date')
                            if not next_start_str:
                                continue
                            next_start = parse_google_datetime(next_start_str)
                            
                            # Check if subsequent meeting starts exactly when current ends (within 60 seconds)
                            if abs((next_start - current_end).total_seconds()) < 60:
                                b2b_key = (str(user.user_id), "b2b", current_event['id'], next_event['id'])
                                if b2b_key not in alerted_overruns:
                                    msg = f"You have back-to-back meetings ('{current_event.get('summary', 'Meeting A')}' and '{next_event.get('summary', 'Meeting B')}') with no buffer. Recommend setting a 5-minute buffer?"
                                    
                                    user_id_str = str(user.user_id)
                                    if user_id_str not in proactive_suggestions:
                                        proactive_suggestions[user_id_str] = []
                                        
                                    proactive_suggestions[user_id_str].append({
                                        "id": f"b2b_{uuid.uuid4()}",
                                        "message": msg,
                                        "created_at": datetime.utcnow().isoformat() + 'Z'
                                    })
                                    alerted_overruns.add(b2b_key)
                                    logger.info(f"[PROACTIVE B2B ALERT] User: {user.email}, Alert: {msg}")
                                    print(f"\n[PROACTIVE B2B ALERT] User: {user.email}, Alert: {msg}\n", flush=True)
                                        
                except Exception as check_ex:
                    logger.error(f"Error checking overrun for user {user.email}: {check_ex}")
                finally:
                    # Reset context variables
                    active_user_token.reset(token_token)
                    active_user_id.reset(token_id)
                    active_user_email.reset(token_email)
            db.close()
        except Exception as ex:
            logger.error(f"Error in Proactive Calendar Checker: {ex}")
        await asyncio.sleep(15)  # Run every 15 seconds
