import os
import json
from datetime import datetime
from dotenv import load_dotenv
from groq import Groq
from app.agent.models import AgentResponse

load_dotenv()

SYSTEM_PROMPT = """You are TaskFlow AI, an intelligent task and calendar management assistant.
Analyze user inputs and decide which action to trigger.

You MUST return a JSON object with this exact schema:
{
    "thought": "Your step-by-step reasoning",
    "action": "create_calendar_event" | "undo" | "conversation",
    "parameters": {
        "summary": "Event title",
        "start_iso": "YYYY-MM-DDTHH:MM:SSZ",
        "end_iso": "YYYY-MM-DDTHH:MM:SSZ"
    },
    "reply_message": "Conversational message to user"
}
"""

def parse_user_intent(user_prompt: str) -> AgentResponse:
    client = Groq(api_key=os.getenv("GROQ_API_KEY"))
    now_str = datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
    
    full_prompt = f"Current UTC Time: {now_str}\nUser Prompt: {user_prompt}"

    try:
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": full_prompt}
            ],
            response_format={"type": "json_object"}
        )
        
        parsed_data = json.loads(response.choices[0].message.content)
        return AgentResponse(**parsed_data)
        
    except Exception as e:
        return AgentResponse(
            thought=f"Failed to parse prompt: {str(e)}",
            action="conversation",
            parameters={},
            reply_message="I had trouble processing that request. Could you try rephrasing?"
        )