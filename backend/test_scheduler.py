import sys
import time
import asyncio

sys.stdout.reconfigure(encoding='utf-8')

from app.agent.scheduler import global_reminder_queue

print("🧪 Testing Priority Queue Heap & Scheduler...")

def run_tests():
    # Clear the queue to ensure a clean state
    while not global_reminder_queue.empty():
        global_reminder_queue.pop()

    now = int(time.time())
    
    # Push three reminders with different trigger times (out of order)
    # The queue should always return the one with the smallest timestamp first
    print("\n--- Test 1: Min-Priority Queue Ordering ---")
    global_reminder_queue.push(now + 100, "rem_100", "task 100 seconds later")
    global_reminder_queue.push(now + 10, "rem_10", "task 10 seconds later")
    global_reminder_queue.push(now + 50, "rem_50", "task 50 seconds later")

    # Size should be 3
    print(f"Queue size (expected 3): {global_reminder_queue.size()}")
    assert global_reminder_queue.size() == 3

    # Peek should return the 10-second task
    top = global_reminder_queue.peek()
    print(f"Top reminder (expected rem_10): {top.get('reminder_id')} ({top.get('task')})")
    assert top.get("reminder_id") == "rem_10"

    # Pop should return elements in order: 10 -> 50 -> 100
    p1 = global_reminder_queue.pop()
    p2 = global_reminder_queue.pop()
    p3 = global_reminder_queue.pop()

    print(f"First popped (expected rem_10): {p1.get('reminder_id')}")
    print(f"Second popped (expected rem_50): {p2.get('reminder_id')}")
    print(f"Third popped (expected rem_100): {p3.get('reminder_id')}")

    assert p1.get("reminder_id") == "rem_10"
    assert p2.get("reminder_id") == "rem_50"
    assert p3.get("reminder_id") == "rem_100"
    print("Priority Queue ordering verification successful!")

if __name__ == "__main__":
    run_tests()
