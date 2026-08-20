from pydantic import BaseModel, Field
from typing import Optional, List

class EventParameters(BaseModel):
    summary: Optional[str] = Field(default=None, description="Title or summary of the calendar event")
    start_iso: Optional[str] = Field(default=None, description="Start date/time in ISO 8601 format (e.g. 2026-08-13T10:00:00Z)")
    end_iso: Optional[str] = Field(default=None, description="End date/time in ISO 8601 format (e.g. 2026-08-13T11:00:00Z)")
    task: Optional[str] = Field(default=None, description="Task details or description for a reminder")
    remind_minutes_before: Optional[int] = Field(default=30, description="Lead time in minutes before reminder/event")
    double_book: Optional[bool] = Field(default=False, description="Set to true if user explicitly requests to double-book or override calendar conflict")
    task_id: Optional[str] = Field(default=None, description="Optional parent task ID associated with this action")

class TaskDefinition(BaseModel):
    task_id: str = Field(description="Unique string identifier for this task (e.g. task_1)")
    action: str = Field(description="The action type: 'create_calendar_event', 'set_reminder', 'undo', or 'conversation'")
    parameters: EventParameters = Field(default_factory=EventParameters, description="Action parameter fields.")
    dependencies: List[str] = Field(default_factory=list, description="List of task_ids this task depends on")

class AgentResponse(BaseModel):
    thought: str = Field(description="Step-by-step reasoning behind selecting the actions.")
    tasks: List[TaskDefinition] = Field(default_factory=list, description="Decomposed tasks to execute.")
    reply_message: str = Field(description="A clear, conversational message to display back to the user.")

    @property
    def action(self) -> str:
        if self.tasks:
            return self.tasks[0].action
        return "conversation"

    @property
    def parameters(self) -> EventParameters:
        if self.tasks:
            return self.tasks[0].parameters
        return EventParameters()