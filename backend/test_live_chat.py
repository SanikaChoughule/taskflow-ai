import sys
import requests
import json
import time

sys.stdout.reconfigure(encoding='utf-8')

prompt = "Schedule a Team meeting tomorrow from 3pm to 4pm UTC, and set a reminder to prep 0 minutes before."

print(f"🚀 Sending compound prompt to live server:\n'{prompt}'\n")

try:
    res = requests.post("http://localhost:8000/api/chat", json={"prompt": prompt})
    print("--- Live Server Response ---")
    print(json.dumps(res.json(), indent=2, ensure_ascii=False))
except Exception as e:
    print(f"Failed to communicate with live server: {e}")

print("\n⌛ Waiting 5 seconds to let the background scheduler fire the reminder...")
time.sleep(5)
