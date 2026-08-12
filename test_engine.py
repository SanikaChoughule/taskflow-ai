from app.agent.engine import execute_user_request

print("🚀 Testing Full TaskFlow AI Engine Flow...")

# 1. Create Event via Natural Language
print("\n--- Test 1: Scheduling Event ---")
res1 = execute_user_request("Schedule a Sync meeting tomorrow from 2pm to 3pm UTC")
print("Response:", res1)

# 2. Undo the Created Event via Natural Language
print("\n--- Test 2: Undoing Event ---")
res2 = execute_user_request("Wait, please undo that")
print("Response:", res2)