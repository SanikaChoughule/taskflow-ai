import sys
import os
import asyncio
from pathlib import Path

# Set UTF-8 encoding
sys.stdout.reconfigure(encoding='utf-8')

# Ensure backend root is in PATH
sys.path.append(os.path.abspath(os.path.dirname(__file__)))

from app.agent.engine import execute_user_request
from app.tools.calendar import delete_calendar_event

async def main():
    print("🚀 Running Automated Conflict & Risk Detection Tests...")
    
    # 1. Schedule initial event
    print("\n--- Step 1: Scheduling Initial Event (5 days from now 3 PM - 4 PM UTC) ---")
    res1 = await execute_user_request("Schedule a Sync with Sanika in 9 days from 3:00 PM to 4:00 PM UTC")
    print("Response:", res1)
    
    if res1.get("status") != "success":
        print("❌ Failed to book initial event. Aborting test.")
        return
        
    is_compound1 = "results" in res1
    node_data1 = res1["results"]["task_1"] if is_compound1 else res1
    event1_id = node_data1["details"]["event_id"]
    print(f"✅ Initial Event Created successfully (ID: {event1_id})")

    # 2. Try scheduling overlapping event
    print("\n--- Step 2: Scheduling Overlapping Event (5 days from now 3 PM UTC) ---")
    h2 = [
        {"role": "user", "content": "Schedule a Sync with Sanika in 9 days from 3:00 PM to 4:00 PM UTC"},
        {"role": "assistant", "content": res1.get("reply")}
    ]
    
    res2 = await execute_user_request("Schedule a Sync with Sherwin in 9 days at 3:00 PM UTC for 1 hour", h2)
    print("Response:", res2)
    
    if res2.get("status") == "conflict":
        print("✅ Overlap detected correctly! Status returned as 'conflict'.")
        print("Reply Prompt:", res2.get("reply"))
    else:
        print("❌ Overlap NOT detected!")
        # Clean up event1
        delete_calendar_event(event1_id)
        return

    # 3. Schedule the conflicting event using Double Book override
    print("\n--- Step 3: Scheduling with Double-Book Override ---")
    h3 = h2 + [
        {"role": "user", "content": "Schedule a Sync with Sherwin in 9 days at 3:00 PM UTC for 1 hour"},
        {"role": "assistant", "content": res2.get("reply")}
    ]
    
    res3 = await execute_user_request("double book", h3)
    print("Response:", res3)
    
    is_compound3 = "results" in res3
    node_data3 = res3["results"]["task_1"] if is_compound3 else res3
    
    if node_data3.get("status") == "success":
        event2_id = node_data3["details"]["event_id"]
        print(f"✅ Overlap overridden successfully! Event created (ID: {event2_id})")
        # Clean up both events
        print("\n--- Step 4: Cleaning up created test events ---")
        delete_calendar_event(event1_id)
        delete_calendar_event(event2_id)
        print("✅ Cleanup complete.")
    else:
        print("❌ Double book override failed!")
        # Clean up event1
        delete_calendar_event(event1_id)

if __name__ == "__main__":
    asyncio.run(main())
