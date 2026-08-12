from typing import Callable, Any, Dict, List

class UndoStack:
    def __init__(self):
        self._stack: List[Dict[str, Any]] = []

    def push(self, action_name: str, undo_payload_json: str):
        self._stack.append({
            "action_name": action_name,
            "undo_payload_json": undo_payload_json
        })
        print(f"📌 [STACK PUSH] Registered action: {action_name}")

    def pop_and_undo(self) -> Dict[str, Any]:
        if not self._stack:
            return {"status": "empty", "message": "No actions to undo."}
        
        last_action = self._stack.pop()
        print(f"⏪ [STACK POP] Popped action: {last_action['action_name']}")
        return {
            "status": "success",
            "action_name": last_action["action_name"],
            "undo_payload_json": last_action["undo_payload_json"]
        }

global_undo_stack = UndoStack()