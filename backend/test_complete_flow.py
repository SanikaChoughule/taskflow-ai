import sys
import requests
import json
import time

sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8000"

def print_section(title):
    print("\n" + "=" * 60)
    print(f" {title} ")
    print("=" * 60)

def main():
    print_section("STARTING COMPLETE TASKFLOW AI INTEGRATION TEST")

    # Step 1: Create a Calendar Event and a Reminder via Compound Intent
    prompt = "Schedule a Scrum meeting tomorrow from 11am to 12pm UTC, and set a reminder to prep 10 minutes before."
    print(f"👉 Sending prompt to agent: '{prompt}'\n")
    
    chat_res = requests.post(f"{BASE_URL}/api/chat", json={"prompt": prompt})
    if chat_res.status_code != 200:
        print(f"❌ Chat request failed: {chat_res.text}")
        return
    
    print("🤖 Agent Response:")
    print(json.dumps(chat_res.json(), indent=2, ensure_ascii=False))

    # Step 2: Verify active calendar events
    print_section("VERIFYING CALENDAR EVENTS & REMINDERS")
    
    calendar_res = requests.get(f"{BASE_URL}/api/calendar/events")
    if calendar_res.status_code == 200:
        events = calendar_res.json().get("events", [])
        print(f"📅 Active Calendar Events (Found {len(events)}):")
        for event in events:
            print(f"  - {event.get('summary')} ({event.get('start', {}).get('dateTime') or event.get('start', {}).get('date')})")
    else:
        print(f"❌ Failed to fetch calendar events: {calendar_res.text}")

    # Step 3: Verify active reminders
    reminders_res = requests.get(f"{BASE_URL}/api/reminders")
    if reminders_res.status_code == 200:
        reminders = reminders_res.json().get("reminders", [])
        print(f"🔔 Active Reminders (Found {len(reminders)}):")
        for reminder in reminders:
            print(f"  - Task: {reminder.get('task')}, Offset: {reminder.get('remind_minutes_before')} mins")
    else:
        print(f"❌ Failed to fetch reminders: {reminders_res.text}")

    # Step 4: Undo the actions
    print_section("UNDOING THE LAST ACTIONS")
    undo_prompt = "Wait, undo that last action"
    print(f"👉 Sending undo prompt to agent: '{undo_prompt}'\n")
    
    undo_res = requests.post(f"{BASE_URL}/api/chat", json={"prompt": undo_prompt})
    if undo_res.status_code != 200:
        print(f"❌ Undo request failed: {undo_res.text}")
        return
    
    print("🤖 Agent Response:")
    print(json.dumps(undo_res.json(), indent=2, ensure_ascii=False))

    # Step 5: Re-verify clean status
    print_section("VERIFYING CLEANUP AFTER UNDO")
    
    calendar_res = requests.get(f"{BASE_URL}/api/calendar/events")
    if calendar_res.status_code == 200:
        events = calendar_res.json().get("events", [])
        print(f"📅 Active Calendar Events (Found {len(events)}):")
        for event in events:
            print(f"  - {event.get('summary')} ({event.get('start', {}).get('dateTime') or event.get('start', {}).get('date')})")
    else:
        print("❌ Failed to fetch calendar events after undo")

    reminders_res = requests.get(f"{BASE_URL}/api/reminders")
    if reminders_res.status_code == 200:
        reminders = reminders_res.json().get("reminders", [])
        print(f"🔔 Active Reminders (Found {len(reminders)}):")
        for reminder in reminders:
            print(f"  - Task: {reminder.get('task')}, Offset: {reminder.get('remind_minutes_before')} mins")
    else:
        print("❌ Failed to fetch reminders after undo")

    print_section("TEST COMPLETED SUCCESSFULLY")

if __name__ == "__main__":
    main()
