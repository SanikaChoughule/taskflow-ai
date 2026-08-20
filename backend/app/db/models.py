import uuid
from datetime import datetime
from sqlalchemy import Column, String, Integer, Boolean, ForeignKey, DateTime, Text, JSON, Uuid
from sqlalchemy.orm import relationship, declarative_base

Base = declarative_base()

class User(Base):
    __tablename__ = "users"

    user_id = Column(Uuid, primary_key=True, default=uuid.uuid4)
    name = Column(String(255), nullable=False)
    email = Column(String(255), unique=True, nullable=False, index=True)
    google_oauth_token = Column(Text, nullable=True)
    timezone = Column(String(100), default="UTC")
    profile_pic = Column(String(500), nullable=True)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow, nullable=False)

    tasks = relationship("Task", back_populates="user", cascade="all, delete-orphan")
    sessions = relationship("Session", back_populates="user", cascade="all, delete-orphan")


class Task(Base):
    __tablename__ = "tasks"

    task_id = Column(Uuid, primary_key=True, default=uuid.uuid4)
    user_id = Column(Uuid, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False, index=True)
    parent_task_id = Column(Uuid, ForeignKey("tasks.task_id", ondelete="SET NULL"), nullable=True, index=True)
    title = Column(String(500), nullable=False)
    intent_type = Column(String(100), nullable=True)
    status = Column(String(50), default="pending", nullable=False, index=True)
    priority_score = Column(Integer, default=0, nullable=False)
    scheduled_time = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow, nullable=False)

    user = relationship("User", back_populates="tasks")
    parent_task = relationship("Task", remote_side=[task_id], backref="sub_tasks")
    reminders = relationship("Reminder", back_populates="task", cascade="all, delete-orphan")
    calendar_events = relationship("CalendarEvent", back_populates="task", cascade="all, delete-orphan")
    action_logs = relationship("ActionLog", back_populates="task", cascade="all, delete-orphan")


class Reminder(Base):
    __tablename__ = "reminders"

    reminder_id = Column(Uuid, primary_key=True, default=uuid.uuid4)
    task_id = Column(Uuid, ForeignKey("tasks.task_id", ondelete="CASCADE"), nullable=False, index=True)
    trigger_time = Column(DateTime(timezone=True), nullable=False, index=True)
    channel = Column(String(50), default="push", nullable=False)
    delivered = Column(Boolean, default=False, nullable=False)

    task = relationship("Task", back_populates="reminders")


class CalendarEvent(Base):
    __tablename__ = "calendar_events"

    event_id = Column(Uuid, primary_key=True, default=uuid.uuid4)
    task_id = Column(Uuid, ForeignKey("tasks.task_id", ondelete="CASCADE"), nullable=False, index=True)
    google_event_id = Column(String(255), unique=True, nullable=False, index=True)
    start_time = Column(DateTime(timezone=True), nullable=False)
    end_time = Column(DateTime(timezone=True), nullable=False)
    attendees = Column(JSON, default=list, nullable=False)

    task = relationship("Task", back_populates="calendar_events")


class ActionLog(Base):
    __tablename__ = "action_logs"

    log_id = Column(Uuid, primary_key=True, default=uuid.uuid4)
    task_id = Column(Uuid, ForeignKey("tasks.task_id", ondelete="CASCADE"), nullable=False, index=True)
    action_type = Column(String(100), nullable=False)
    explanation_text = Column(Text, nullable=True)
    is_undoable = Column(Boolean, default=True, nullable=False)
    timestamp = Column(DateTime(timezone=True), default=datetime.utcnow, nullable=False, index=True)

    task = relationship("Task", back_populates="action_logs")


class Session(Base):
    __tablename__ = "sessions"

    session_id = Column(Uuid, primary_key=True, default=uuid.uuid4)
    user_id = Column(Uuid, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False, index=True)
    last_active = Column(DateTime(timezone=True), default=datetime.utcnow, nullable=False, index=True)
    device_type = Column(String(100), nullable=True)

    user = relationship("User", back_populates="sessions")


class Action(Base):
    __tablename__ = "actions"

    id = Column(Uuid, primary_key=True, default=uuid.uuid4)
    timestamp = Column(DateTime(timezone=True), default=datetime.utcnow, nullable=False, index=True)
    action_type = Column(String(100), nullable=False)
    action_payload = Column(Text, nullable=True)
    reason = Column(Text, nullable=True)
    undo_action = Column(Text, nullable=True)
    status = Column(String(50), default="active", nullable=False, index=True)
    user_id = Column(Uuid, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=True, index=True)
    parent_task_id = Column(Uuid, nullable=True, index=True)
