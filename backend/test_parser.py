from app.agent.parser import parse_user_intent

print("🧪 Testing Gemini Parser...")

res1 = parse_user_intent("Schedule a Team Sync meeting tomorrow from 10am to 11am UTC")
print("\n--- Test 1 (Create Event) ---")
print("Action:", res1.action)
print("Thought:", res1.thought)
print("Params:", res1.parameters)
print("Reply:", res1.reply_message)

res2 = parse_user_intent("Oops, undo that last creation")
print("\n--- Test 2 (Undo) ---")
print("Action:", res2.action)
print("Reply:", res2.reply_message)