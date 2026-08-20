import sys
import json
from app.tools.calendar import list_calendar_events

# Ensure output is printed in utf-8
sys.stdout.reconfigure(encoding='utf-8')

print("📅 Fetching Google Calendar Events...")
events = list_calendar_events()
print(f"Found {len(events)} events:")
for event in events:
    print(f"- Summary: {event.get('summary')}")
    print(f"  Start: {event.get('start', {}).get('dateTime') or event.get('start', {}).get('date')}")
    print(f"  End: {event.get('end', {}).get('dateTime') or event.get('end', {}).get('date')}")
    print(f"  Link: {event.get('htmlLink')}")
    print()
