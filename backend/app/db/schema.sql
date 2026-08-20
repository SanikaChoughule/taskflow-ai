-- PostgreSQL Database Schema for TaskFlow AI (Multi-User, ACID compliant)

-- Drop tables in reverse order of dependencies
DROP TABLE IF EXISTS action_logs CASCADE;
DROP TABLE IF EXISTS calendar_events CASCADE;
DROP TABLE IF EXISTS reminders CASCADE;
DROP TABLE IF EXISTS sessions CASCADE;
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 1. USERS Table
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    google_oauth_token TEXT,
    timezone VARCHAR(100) DEFAULT 'UTC',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Index for fast user lookup by email
CREATE INDEX idx_users_email ON users(email);

-- 2. TASKS Table
CREATE TABLE tasks (
    task_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    parent_task_id UUID REFERENCES tasks(task_id) ON DELETE SET NULL, -- Self-referencing FK for DAG dependencies
    title VARCHAR(500) NOT NULL, -- title / raw_command
    intent_type VARCHAR(100),
    status VARCHAR(50) DEFAULT 'pending' NOT NULL,
    priority_score INTEGER DEFAULT 0 NOT NULL,
    scheduled_time TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_tasks_user_id ON tasks(user_id);
CREATE INDEX idx_tasks_parent_task_id ON tasks(parent_task_id);
CREATE INDEX idx_tasks_status ON tasks(status);

-- 3. REMINDERS Table
CREATE TABLE reminders (
    reminder_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    trigger_time TIMESTAMP WITH TIME ZONE NOT NULL,
    channel VARCHAR(50) DEFAULT 'push' NOT NULL, -- push/voice
    delivered BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE INDEX idx_reminders_task_id ON reminders(task_id);
CREATE INDEX idx_reminders_trigger_time ON reminders(trigger_time) WHERE NOT delivered;

-- 4. CALENDAR_EVENTS Table
CREATE TABLE calendar_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    google_event_id VARCHAR(255) UNIQUE NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    attendees JSONB DEFAULT '[]'::jsonb NOT NULL
);

CREATE INDEX idx_calendar_events_task_id ON calendar_events(task_id);
CREATE INDEX idx_calendar_events_google_event_id ON calendar_events(google_event_id);

-- 5. ACTION_LOGS Table
CREATE TABLE action_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    action_type VARCHAR(100) NOT NULL,
    explanation_text TEXT,
    is_undoable BOOLEAN DEFAULT TRUE NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_action_logs_task_id ON action_logs(task_id);
CREATE INDEX idx_action_logs_timestamp ON action_logs(timestamp);

-- 6. SESSIONS Table
CREATE TABLE sessions (
    session_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    last_active TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    device_type VARCHAR(100)
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_last_active ON sessions(last_active);
