import os
import json
from pathlib import Path
from dotenv import load_dotenv
from groq import Groq

# 1. Load environment variables from backend/.env
env_path = Path(__file__).resolve().parent.parent / ".env"
load_dotenv(dotenv_path=env_path)

# 2. Initialize Groq client
client = Groq(api_key=os.getenv("GROQ_API_KEY"))

# 3. Define tool execution functions
def schedule_meeting(contact_name: str, date_time: str) -> str:
    """Schedules a meeting on Google Calendar."""
    # Put your Google Calendar execution logic here
    return f"Successfully scheduled meeting with {contact_name} for {date_time}."

def set_reminder(task: str, remind_minutes_before: int = 30) -> str:
    """Sets a task reminder."""
    # Put your Reminders engine logic here
    return f"Reminder set: '{task}' ({remind_minutes_before} minutes prior)."

# Map tool names to actual functions
AVAILABLE_TOOLS = {
    "schedule_meeting": schedule_meeting,
    "set_reminder": set_reminder,
}

# 4. Define JSON Schemas for Groq Tool Calling
tools = [
    {
        "type": "function",
        "function": {
            "name": "schedule_meeting",
            "description": "Schedules a calendar meeting with a contact.",
            "parameters": {
                "type": "object",
                "properties": {
                    "contact_name": {
                        "type": "string",
                        "description": "Name of person to meet",
                    },
                    "date_time": {
                        "type": "string",
                        "description": "Date and time of meeting",
                    },
                },
                "required": ["contact_name", "date_time"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "set_reminder",
            "description": "Sets a task reminder.",
            "parameters": {
                "type": "object",
                "properties": {
                    "task": {
                        "type": "string",
                        "description": "Description of the reminder task",
                    },
                    "remind_minutes_before": {
                        "type": "integer",
                        "description": "Lead time in minutes before event",
                        "default": 30,
                    },
                },
                "required": ["task"],
            },
        },
    },
]

# 5. Main Agent Command Handler
def run_taskflow_command(user_prompt: str):
    messages = [
        {
            "role": "system",
            "content": "You are TaskFlow AI. Parse instructions and call tools as needed.",
        },
        {"role": "user", "content": user_prompt},
    ]

    response = client.chat.completions.create(
        model="llama-3.3-70b-versatile",
        messages=messages,
        tools=tools,
        tool_choice="auto",
    )

    response_message = response.choices[0].message
    tool_calls = response_message.tool_calls

    if tool_calls:
        results = []
        for tool_call in tool_calls:
            function_name = tool_call.function.name
            function_args = json.loads(tool_call.function.arguments)
            function_to_call = AVAILABLE_TOOLS[function_name]

            # Execute tool
            output = function_to_call(**function_args)
            results.append(output)

        return "\n".join(results)

    return response_message.content


# 6. Test execution
if __name__ == "__main__":
    prompt = "Schedule a meeting with Rahul tomorrow at 3 PM and remind me 30 minutes before"
    output = run_taskflow_command(prompt)
    print("\n--- TaskFlow Output ---")
    print(output)