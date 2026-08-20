from typing import Dict, List, Set, Any
import asyncio
import logging

logger = logging.getLogger(__name__)

class TaskNode:
    def __init__(self, task_id: str, action: str, parameters: dict):
        self.task_id = task_id
        self.action = action
        self.parameters = parameters
        self.dependencies: List[str] = []
        self.dependents: List[str] = []
        self.state = "pending"  # pending, running, completed, failed
        self.result = None

class TaskDAG:
    def __init__(self):
        self.nodes: Dict[str, TaskNode] = {}

    def add_node(self, task_id: str, action: str, parameters: dict) -> TaskNode:
        node = TaskNode(task_id, action, parameters)
        self.nodes[task_id] = node
        return node

    def add_dependency(self, parent_id: str, child_id: str):
        if parent_id in self.nodes and child_id in self.nodes:
            # child_id depends on parent_id
            if parent_id not in self.nodes[child_id].dependencies:
                self.nodes[child_id].dependencies.append(parent_id)
            if child_id not in self.nodes[parent_id].dependents:
                self.nodes[parent_id].dependents.append(child_id)

    def has_cycle(self) -> bool:
        # DFS cycle detection using node colors: 0=unvisited, 1=visiting, 2=visited
        visited: Dict[str, int] = {node_id: 0 for node_id in self.nodes}

        def dfs(node_id: str) -> bool:
            visited[node_id] = 1
            for neighbor in self.nodes[node_id].dependents:
                if visited[neighbor] == 1:
                    return True
                elif visited[neighbor] == 0:
                    if dfs(neighbor):
                        return True
            visited[node_id] = 2
            return False

        for node_id in self.nodes:
            if visited[node_id] == 0:
                if dfs(node_id):
                    return True
        return False

    def get_topological_order(self) -> List[str]:
        # Returns topological sorting of node IDs
        visited: Set[str] = set()
        stack: List[str] = []

        def dfs(node_id: str):
            visited.add(node_id)
            for neighbor in self.nodes[node_id].dependents:
                if neighbor not in visited:
                    dfs(neighbor)
            stack.insert(0, node_id)

        for node_id in self.nodes:
            if node_id not in visited:
                dfs(node_id)
        return stack

    async def execute_node(self, node_id: str, execute_fn) -> Any:
        node = self.nodes[node_id]
        node.state = "running"
        try:
            logger.info(f"Executing task node: {node_id} ({node.action})")
            
            # Wait for all direct parent dependencies to complete successfully
            for dep_id in node.dependencies:
                dep_node = self.nodes[dep_id]
                while dep_node.state != "completed":
                    if dep_node.state == "failed":
                        raise RuntimeError(f"Dependency task '{dep_id}' failed. Skipping current task.")
                    await asyncio.sleep(0.05)
                
                # Abort if the parent task had a scheduling conflict
                if dep_node.result and isinstance(dep_node.result, dict) and dep_node.result.get("status") == "conflict":
                    raise RuntimeError(f"Dependency task '{dep_id}' had a calendar conflict. Skipping current task.")

            # Invoke the execution handler
            res = await execute_fn(node.action, node.parameters)
            node.result = res
            node.state = "completed"
            logger.info(f"Task node {node_id} completed successfully.")
            return res
        except Exception as e:
            node.state = "failed"
            logger.error(f"Task node {node_id} failed: {e}")
            raise e

    async def execute_dag(self, execute_fn) -> Dict[str, Any]:
        if self.has_cycle():
            raise ValueError("Cannot execute task graph because it contains cycles.")

        # Run independent nodes immediately; dependencies wait asynchronously
        tasks = []
        for node_id in self.nodes:
            tasks.append(self.execute_node(node_id, execute_fn))

        await asyncio.gather(*tasks, return_exceptions=True)
        
        # Check if any tasks failed
        failed_tasks = [nid for nid, node in self.nodes.items() if node.state == "failed"]
        if failed_tasks:
            logger.error(f"Graph execution completed with failures in nodes: {failed_tasks}")
            
        return {
            "status": "success" if not failed_tasks else "partial_failure",
            "results": {node_id: self.nodes[node_id].result for node_id in self.nodes},
            "failures": failed_tasks
        }
