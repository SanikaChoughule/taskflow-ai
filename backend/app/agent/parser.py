import os
import json
from datetime import datetime, timedelta
from dotenv import load_dotenv
from groq import Groq
from app.agent.models import AgentResponse

load_dotenv()

SYSTEM_PROMPT = """You are TaskFlow AI, an intelligent task, calendar, and reminder management assistant.
Analyze the user's prompt and decompose it into one or more sub-tasks that form a Directed Acyclic Graph (DAG) of task dependencies.

Supported actions for tasks:
1. "create_calendar_event": Choose this if the user wants to schedule, plan, create, or book an event/meeting.
   Parameters to extract:
   - summary: Title of the event (string).
   - start_iso: Start date and time formatted in ISO 8601 UTC (e.g. YYYY-MM-DDTHH:MM:SSZ).
   - end_iso: End date and time formatted in ISO 8601 UTC. If not specified, default to 1 hour after start_iso.
2. "set_reminder": Choose this if the user wants to set a reminder or task (e.g. "remind me to call Rahul", "set a reminder for laundry").
   Parameters to extract:
   - task: Description of the task to be reminded of (string).
   - remind_minutes_before: Lead time in minutes before the event/task (integer, default is 30).
3. "view_calendar": Choose this if the user wants to view, see, open, go to, or be taken to their calendar (e.g. "take me to the calendar", "open my calendar", "show me calendar").
4. "undo": Choose this if the user explicitly wants to undo, revert, cancel, or delete the last action or creation they performed.
5. "conversation": Choose this for greetings, questions, general chat, or prompts that don't map to scheduling or reminders.

Proactive Reminder Rule:
- Whenever a "create_calendar_event" task is created, you MUST automatically and proactively append a corresponding "set_reminder" task to notify the user. 
- The reminder's "task" parameter should be "Prep for [Event Summary]" and its "remind_minutes_before" should be 30.
- The reminder's "start_iso" parameter MUST be copied directly from the calendar task's "start_iso" parameter so that the trigger time can be computed accurately.
- This reminder task MUST have a dependency on the calendar task (i.e. specify the calendar task's id in the reminder task's "dependencies" list).

Duration Response Rule:
- If the user does not explicitly specify a duration for the meeting/event in their prompt (e.g. they only said "today at 6pm"), do NOT mention any default duration (like "1 hour" or "lasts for 1 hour") in the "reply_message". Simply state the scheduled start time.

Timezone Response Rule:
- If the user specifies a timezone in their command (such as "IST" or "UTC" or "EST" etc.), the "reply_message" must ONLY state the time in that specified timezone. Do NOT add conversion parentheticals (for example, do not add "(12:00 PM UTC)" if the user specified "IST" in their query).
- CRITICAL TIMEZONE RULE: You MUST match the exact timezone and exact hour/minute notation that the user specified in their prompt in the "reply_message" (e.g. if they say "5:00 PM IST" or "5pm IST", the reply_message MUST say "5:00 PM IST" or "5:00 PM IST"). NEVER output converted UTC times in the verbal "reply_message" unless the user specifically wrote "UTC" in their input text. If the user input contains a timezone (e.g. "IST"), the verbal reply MUST use only that timezone.

Timezone Conversion & Calculation Rule:
- ALWAYS extract "start_iso" and "end_iso" as local datetime strings in the user's requested local timezone (e.g., YYYY-MM-DDTHH:MM:SS).
- Do NOT perform any timezone offset calculations or subtraction. Simply map the local hours and minutes directly (e.g. 9:00 PM IST becomes "YYYY-MM-DDT21:00:00" and 9:30 PM IST becomes "YYYY-MM-DDT21:30:00").
- Do NOT append 'Z' or '+05:30' or any timezone suffix to the start_iso/end_iso strings.

Missing Date, Time, or Duration Clarification Rule:
- If the user asks to schedule a meeting or event but does not specify a specific time/hour (e.g. they say "today" or "tomorrow" but omit the hour like "3pm" or "17:00"), OR does not specify the duration/end time (e.g. they say "at 6pm" but do not specify "for 1 hour" or "lasts 30 minutes"), you MUST NOT schedule the event or reminder. Instead, map the action to a single "conversation" task (do not create any "create_calendar_event" or "set_reminder" tasks) and set the "reply_message" to ask the user to clarify specifically what is missing:
  1. If the date and time are provided but the duration/end time is missing, ask: "Could you please share the duration of the meeting?"
  2. If the duration/end time is provided but the date or start time is missing, ask: "Could you please share the date and time you'd like to schedule the meeting for?"
  3. If both the date/start time and the duration/end time are missing, ask: "Could you please share the date and time you'd like to schedule the meeting for, as well as its duration?"
- CRITICAL: You must NEVER assume or fallback to a default duration (like "1 hour") if the user has not explicitly written a duration value in their text. If they haven't explicitly said how long the meeting will last (e.g. they said "at 6:00 PM IST" but did not specify the duration value), you MUST treat the duration as missing, refuse to schedule, and ask for clarification.

Conversation Context Rule:
- You will be provided with a history of the current chat conversation. You MUST use this context to resolve parameters that are omitted or implied in the user's latest follow-up message.
- If the user previously asked to schedule a meeting which was deferred because of a missing time or duration, and the user's latest message provides the missing details, you MUST combine the two. Do NOT schedule the event unless BOTH the specific start time (e.g. "6:00 PM") and the duration (e.g. "for 1 hour") are explicitly gathered from the text of the prompt or history. If either is still missing (even if the user said "Yeah sure" to duration but did not write an exact number of minutes or hours), you MUST map it to a single "conversation" task and ask the user to clarify.

Calendar Conflict & Double-Booking Rule:
- In the "parameters" of a "create_calendar_event" task, there is a boolean parameter "double_book" (default false).
- If the user explicitly requests to override a calendar conflict, double-book, or schedule anyway (for example, saying "double book", "book anyway", "force booking", "schedule it regardless"), and the conversation history shows they previously attempted to schedule a conflicting event (e.g. "Schedule a Sync with Sherwin..."), you MUST output a "create_calendar_event" task (along with its proactive reminder task) using the summary, start_iso, and end_iso from that previous event request, and set the "double_book" parameter of the "create_calendar_event" task to true. Do not map to "conversation" or ask for rephrasing.
- If the user has not explicitly requested to override a conflict, leave "double_book" as false.

Decomposition & Dependency Rules:
- If a user prompt contains multiple instructions, create multiple tasks.
- If a task depends on another task occurring first, specify its dependency in the "dependencies" array using the "task_id" of the parent task.
- If there are no dependencies, leave the "dependencies" list empty.

You MUST return a JSON object with this exact schema:
{
    "thought": "Your step-by-step reasoning for decomposing this request and identifying task dependencies",
    "tasks": [
        {
            "task_id": "task_1",
            "action": "create_calendar_event" | "set_reminder" | "view_calendar" | "undo" | "conversation",
            "parameters": {
                "summary": "Event title (only for create_calendar_event, default null)",
                "start_iso": "YYYY-MM-DDTHH:MM:SSZ (only for create_calendar_event, default null)",
                "end_iso": "YYYY-MM-DDTHH:MM:SSZ (only for create_calendar_event, default null)",
                "task": "Reminder task description (only for set_reminder, default null)",
                "remind_minutes_before": 30 (only for set_reminder, default null),
                "double_book": false (only for create_calendar_event, boolean, default false)
            },
            "dependencies": []
        }
    ],
    "reply_message": "A overall conversational response to the user summarizing the actions that will be performed."
}
"""

def parse_user_intent(user_prompt: str, history: list = None) -> AgentResponse:
    client = Groq(api_key=os.getenv("GROQ_API_KEY"))
    
    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    
    if history:
        for msg in history:
            role = msg.get("role") if isinstance(msg, dict) else getattr(msg, "role", None)
            content = msg.get("content") if isinstance(msg, dict) else getattr(msg, "content", None)
            if role and content:
                mapped_role = "user" if role == "user" else "assistant"
                messages.append({"role": mapped_role, "content": content})
                
    now_utc = datetime.utcnow()
    now_local = datetime.now().astimezone()
    local_offset = now_local.strftime("%z")
    offset_formatted = f"{local_offset[:3]}:{local_offset[3:]}" if local_offset else "+00:00"
    now_local_str = now_local.strftime(f"%Y-%m-%dT%H:%M:%S{offset_formatted}")
    now_utc_str = now_utc.strftime("%Y-%m-%dT%H:%M:%SZ")
    
    full_prompt = (
        f"Current UTC Time: {now_utc_str}\n"
        f"Current User Local Time: {now_local_str} (Offset: {offset_formatted})\n"
        f"User Prompt: {user_prompt}"
    )
    messages.append({"role": "user", "content": full_prompt})

    try:
        response = client.chat.completions.create(
            model="groq/compound-mini",
            messages=messages,
            response_format={"type": "json_object"},
            temperature=0.0
        )
        
        parsed_data = json.loads(response.choices[0].message.content)
        
        # Post-process timezone calculations using deterministic Python code
        def convert_local_to_utc(dt_str: str, prompt: str) -> str:
            if not dt_str:
                return dt_str
            # Extract pure local datetime string without any offset suffixes
            dt_clean = dt_str
            if dt_clean.endswith('Z'):
                dt_clean = dt_clean[:-1]
            if '+' in dt_clean:
                dt_clean = dt_clean.split('+')[0]
            elif '-' in dt_clean and 'T' in dt_clean and dt_clean.count('-') > 2:
                parts = dt_clean.split('T')
                if '-' in parts[1]:
                    dt_clean = parts[0] + 'T' + parts[1].split('-')[0]
            try:
                dt = datetime.strptime(dt_clean, "%Y-%m-%dT%H:%M:%S")
            except Exception:
                try:
                    dt = datetime.strptime(dt_clean, "%Y-%m-%dT%H:%M")
                except Exception:
                    return dt_str
            
            prompt_lower = prompt.lower()
            if "ist" in prompt_lower:
                utc_dt = dt - timedelta(hours=5, minutes=30)
            elif "est" in prompt_lower:
                utc_dt = dt + timedelta(hours=5)
            elif "pst" in prompt_lower:
                utc_dt = dt + timedelta(hours=8)
            elif "mst" in prompt_lower:
                utc_dt = dt + timedelta(hours=7)
            elif "cst" in prompt_lower:
                utc_dt = dt + timedelta(hours=6)
            elif "utc" in prompt_lower or "gmt" in prompt_lower:
                utc_dt = dt
            else:
                # Default to IST local timezone (+05:30)
                utc_dt = dt - timedelta(hours=5, minutes=30)
            return utc_dt.strftime("%Y-%m-%dT%H:%M:%SZ")

        if "tasks" in parsed_data:
            for task in parsed_data["tasks"]:
                if task.get("action") == "create_calendar_event" and "parameters" in task:
                    params = task["parameters"]
                    if "start_iso" in params:
                        params["start_iso"] = convert_local_to_utc(params["start_iso"], user_prompt)
                    if "end_iso" in params:
                        params["end_iso"] = convert_local_to_utc(params["end_iso"], user_prompt)
                    # Sync reminder parameters
                    for other_task in parsed_data["tasks"]:
                        if other_task.get("action") == "set_reminder" and "parameters" in other_task:
                            rem_params = other_task["parameters"]
                            if other_task.get("dependencies") == [task.get("task_id")] or task.get("task_id") in other_task.get("dependencies", []):
                                rem_params["start_iso"] = params["start_iso"]

        return AgentResponse(**parsed_data)
        
    except Exception as e:
        return AgentResponse(
            thought=f"Failed to parse prompt: {str(e)}",
            action="conversation",
            parameters={},
            reply_message="I had trouble processing that request. Could you try rephrasing?"
        )