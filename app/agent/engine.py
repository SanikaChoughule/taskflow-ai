from app.agent.parser import parse_user_intent
from app.agent.models import AgentResponse
from app.tools.calendar import create_calendar_event
from app.agent.undo import global_undo_stack

def execute_user_request(user_prompt: str) -> dict:
    """Parses user input using Gemini and executes the requested action."""
    # 1. Parse intent
    agent_res: AgentResponse = parse_user_intent(user_prompt)

    # 2. Execute Action based on parser decision
    if agent_res.action == "create_calendar_event":
        params = agent_res.parameters
        if not params.summary or not params.start_iso or not params.end_iso:
            return {
                "status": "error",
                "reply": "I couldn't extract all necessary event details (title or time). Could you specify them?"
            }
        
        tool_result = create_calendar_event(
            summary=params.summary,
            start_iso=params.start_iso,
            end_iso=params.end_iso
        )
        return {
            "status": "success",
            "action": "create_calendar_event",
            "reply": agent_res.reply_message,
            "details": tool_result
        }

    elif agent_res.action == "undo":
        tool_result = global_undo_stack.pop_and_undo()
        if tool_result.get("status") == "empty_stack":
            return {
                "status": "info",
                "action": "undo",
                "reply": "There are no previous actions to undo."
            }
        return {
            "status": "success",
            "action": "undo",
            "reply": tool_result.get("message", "Successfully undone the last action.")
        }

    else:
        # Default conversational response
        return {
            "status": "success",
            "action": "conversation",
            "reply": agent_res.reply_message
        }