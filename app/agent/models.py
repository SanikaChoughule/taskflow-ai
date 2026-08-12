from pydantic import BaseModel, Field
from typing import Optional

class EventParameters(BaseModel):
    summary: Optional[str] = Field(default=None, description="Title or summary of the calendar event")
    start_iso: Optional[str] = Field(default=None, description="Start date/time in ISO 8601 format (e.g. 2026-08-13T10:00:00Z)")
    end_iso: Optional[str] = Field(default=None, description="End date/time in ISO 8601 format (e.g. 2026-08-13T11:00:00Z)")

class AgentResponse(BaseModel):
    thought: str = Field(description="Step-by-step reasoning behind selecting the action.")
    action: str = Field(description="The action type: 'create_calendar_event', 'undo', or 'conversation'.")
    parameters: EventParameters = Field(default_factory=EventParameters, description="Calendar event parameter fields.")
    reply_message: str = Field(description="A clear, conversational message to display back to the user.")