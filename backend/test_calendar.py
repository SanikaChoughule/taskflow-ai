from datetime import datetime, timedelta
from app.tools.calendar import create_calendar_event, delete_calendar_event
from app.agent.undo import global_undo_stack

print("🚀 Testing Google Calendar API Integration...")

# Define a dummy test event 1 hour from now
start_time = (datetime.utcnow() + timedelta(hours=1)).isoformat() + "Z"
end_time = (datetime.utcnow() + timedelta(hours=2)).isoformat() + "Z"

# 1. Create Event
result = create_calendar_event(
    summary="TaskFlow AI Engine Test",
    start_iso=start_time,
    end_iso=end_time
)
print("Create Result:", result)

# 2. Check LIFO Stack
top_action = global_undo_stack.peek() if hasattr(global_undo_stack, 'peek') else "Action Pushed"
print("Top Stack Action:", top_action)

# 3. Test Undo Action
if result.get("status") == "success":
    popped = global_undo_stack.pop_and_undo()
    print("Popped Stack Item:", popped)
    
    # Execute deletion using extracted event_id
    import json
    payload = json.loads(popped["undo_payload_json"])
    delete_res = delete_calendar_event(payload["event_id"])
    print("Delete (Undo) Result:", delete_res)