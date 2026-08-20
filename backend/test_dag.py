import sys
import asyncio
import time

sys.stdout.reconfigure(encoding='utf-8')

from app.agent.dag_planner import TaskDAG

print("🧪 Testing Task Dependency Graph (DAG)...")

async def dummy_execute(action: str, parameters: dict) -> dict:
    duration = parameters.get("duration", 0.5)
    print(f"  [START] Node {action} (simulated delay: {duration}s)")
    await asyncio.sleep(duration)
    print(f"  [DONE] Node {action}")
    return {"status": "success", "action": action, "data": parameters}

async def run_tests():
    # --- Test 1: Cycle Detection ---
    print("\n--- Test 1: Cycle Detection ---")
    dag = TaskDAG()
    dag.add_node("A", "taskA", {})
    dag.add_node("B", "taskB", {})
    dag.add_dependency("A", "B")
    dag.add_dependency("B", "A")  # Creates a cycle A -> B -> A
    
    has_cycle = dag.has_cycle()
    print(f"Cycle detected (expected True): {has_cycle}")
    assert has_cycle is True

    # --- Test 2: Topological Sorting ---
    print("\n--- Test 2: Topological Sorting ---")
    dag = TaskDAG()
    dag.add_node("A", "taskA", {})
    dag.add_node("B", "taskB", {})
    dag.add_node("C", "taskC", {})
    
    # A must happen before B, B must happen before C
    dag.add_dependency("A", "B")
    dag.add_dependency("B", "C")
    
    order = dag.get_topological_order()
    print(f"Topological order (expected ['A', 'B', 'C']): {order}")
    assert order == ["A", "B", "C"]

    # --- Test 3: Parallel / Dependency Execution ---
    print("\n--- Test 3: Parallel & Dependency Execution ---")
    dag = TaskDAG()
    # Task A and Task B are independent (can run in parallel)
    # Task C depends on both Task A and Task B
    dag.add_node("A", "taskA", {"duration": 1.0})
    dag.add_node("B", "taskB", {"duration": 1.0})
    dag.add_node("C", "taskC", {"duration": 0.2})
    
    dag.add_dependency("A", "C")
    dag.add_dependency("B", "C")
    
    start_time = time.time()
    result = await dag.execute_dag(dummy_execute)
    end_time = time.time()
    
    total_duration = end_time - start_time
    print(f"Execution completed in {total_duration:.2f} seconds.")
    print("Execution results:")
    for nid, res in result["results"].items():
         print(f"  Node {nid}: {res}")
         
    # Since A and B run in parallel (1.0s) and C runs after (0.2s), total time should be around 1.2s (not 2.2s!)
    print(f"Is parallel execution successful (expected duration < 1.5s)? {total_duration < 1.5}")
    assert total_duration < 1.5
    assert result["status"] == "success"

if __name__ == "__main__":
    asyncio.run(run_tests())
