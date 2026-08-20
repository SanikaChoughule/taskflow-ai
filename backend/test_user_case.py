import sys
import json
from app.agent.engine import execute_user_request

# Ensure output is printed in utf-8
sys.stdout.reconfigure(encoding='utf-8')

print("🚀 Running single test case...")
prompt = "Schedule a meeting with Akanksha Khodakhe at 19:55 pm , today IST"
import asyncio
res = asyncio.run(execute_user_request(prompt))

print("\n--- Event Creation Result ---")
print(json.dumps(res, indent=2, ensure_ascii=False))
