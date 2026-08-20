# Implementation Plan: Phase 1 - Core Backend & Integrations

This plan details the steps to deliver a fully functional, integrated backend for the TaskFlow AI application before proceeding to the frontend or database.

## User Review Required

> [!IMPORTANT]
> - **C++ Extension Compile Fallback**: Since compiling Python C++ extensions on Windows without MSVC can fail, the setup script is modified to support MinGW compilation, and a robust Python fallback is added in `app/agent/undo.py`. If compilation is not run or fails, the stack will fall back gracefully to a pure Python implementation with identical API structure, ensuring the backend is functional out-of-the-box.
> - **Google Calendar Auth**: The calendar tools use absolute path resolution relative to the backend directory to find `token.json` and `credentials.json`, ensuring server execution works regardless of the directory from which the FastAPI server is started.
> - **Actual Undo Execution**: The current execution engine only pops the last action from the stack but does not execute the inverse tool (e.g. deleting the calendar event). We will implement actual undo behavior by calling the deletion tools.

---

## Proposed Changes

### Setup and Compilation

#### [MODIFY] [setup.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/setup.py)
- Change `setup.py` to use a custom `build_ext` class that detects the compiler type.
- If compiling with MinGW (`mingw32`), strip MSVC-specific options like `/std:c++latest`, `/EHsc`, and `/bigobj`, ensuring successful builds with local `g++`.

---

### Agent & Undo Engine

#### [MODIFY] [undo.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/undo.py)
- Import `app.agent.dsa_engine` dynamically.
- Implement a pure Python fallback class `UndoStackCPP` containing matching `push`, `pop_and_undo`, and `peek` methods.
- Instantiate the global stack using the loaded/fallback class.

#### [MODIFY] [models.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/models.py)
- Add `task` and `remind_minutes_before` optional fields to `EventParameters` (representing general tool action parameters) to support the reminder engine.

#### [MODIFY] [parser.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/parser.py)
- Set LLM temperature to `0.0` to ensure highly deterministic intent parsing.
- Refine the `SYSTEM_PROMPT` to explicitly clarify instructions for each action type (`create_calendar_event`, `set_reminder`, `undo`, `conversation`).

#### [MODIFY] [engine.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/engine.py)
- Update `execute_user_request` to support `set_reminder` action execution.
- Implement the actual undo action execution:
  - Parse the inverse action.
  - If `action_name` matches `create_event:<summary>`, delete the calendar event using the `event_id` in the payload.
  - If `action_name` matches `create_reminder:<task>`, delete the reminder using the `reminder_id` in the payload.

---

### Backend Tools

#### [MODIFY] [calendar.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/tools/calendar.py)
- Dynamically resolve absolute paths to `token.json` and `credentials.json` relative to the backend directory, allowing calendar commands to run from the workspace root.
- Add helper method `list_calendar_events()` to fetch upcoming calendar events for frontend dashboard viewing.

#### [NEW] [reminders.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/tools/reminders.py)
- Create a lightweight persistent JSON-based reminder utility (`reminders.json` in backend root folder).
- Implement `create_reminder(task, remind_minutes_before)` (which also registers the undo payload).
- Implement `delete_reminder(reminder_id)`.
- Implement `list_reminders()`.

---

### API Delivery

#### [MODIFY] [main.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/main.py)
- Add `/api/calendar/events` GET endpoint to fetch upcoming Google Calendar events.
- Add `/api/reminders` GET, POST, and DELETE endpoints to manage task reminders.
- Ensure all endpoints integrate seamlessly with the backend modules and return structured JSON responses.

---

# Implementation Plan: Phase 2 - Hybrid LLM + Classical DSA Engine

This plan outlines the design and implementation of the missing core data structures (HashMap, Graph, and Priority Queue) in the backend to deliver the hybrid "intelligence" engine of TaskFlow AI.

## Phase 2 Proposed Changes

### 1. HashMap Router

#### [MODIFY] [engine.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/engine.py)
- Refactor the action execution handler. Replace the `if/elif/else` conditional chain with a registry HashMap/dictionary structure mapping action string IDs directly to tool-handler functions.
- Define a dictionary `ACTION_REGISTRY: Dict[str, Callable]` to load handlers dynamically.

---

### 2. Task Dependency Graph (DAG)

#### [NEW] [dag_planner.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/dag_planner.py)
- Define a `TaskNode` class representing a sub-task (e.g., action name, parameters, execution state).
- Implement a `TaskDAG` class that models tasks as a directed acyclic graph.
- Implement a topological sort algorithm to order and validate execution sequences (detecting cycles).
- Add execution logic that runs independent nodes in parallel (e.g. using `asyncio.gather` or concurrent workers) while enforcing sequential dependencies for linked nodes.

#### [MODIFY] [parser.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/parser.py)
- Update the system instructions and parser prompt schema to support returning a list of tasks with dependencies (e.g., `dependencies: List[str]`) when a prompt decomposes into multiple tasks.

---

### 3. Priority Queue Scheduler

#### [NEW] [scheduler.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/scheduler.py)
- Implement a thread-safe `PriorityQueue` (Min-Heap) that orders reminders by target execution timestamp.
- Implement a background loop/worker that periodically runs in the FastAPI application lifespan, peeks at the top of the queue in $O(1)$ time, and triggers execution or desktop/console notifications if the current time matches/passes the target timestamp.

#### [MODIFY] [reminders.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/tools/reminders.py)
- Integrate the priority queue to manage active in-memory reminders dynamically, flushing them to/from `reminders.json` for persistence.

#### [MODIFY] [main.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/main.py)
- Register the scheduler background loop as a startup lifespan task in FastAPI.

---

## Verification Plan

### Automated Tests
- Build C++ extension using:
  `venv\Scripts\python setup.py build_ext --inplace --compiler=mingw32`
- Run parser tests:
  `venv\Scripts\python test_parser.py`
- Run calendar integration tests:
  `venv\Scripts\python test_calendar.py`
- Run engine execution tests (including full event scheduling and undo):
  `venv\Scripts\python test_engine.py`
  - Create `test_dag.py` to verify:
  - Cycle detection in task dependencies.
  - Correct topological sorting order.
  - Safe concurrent execution of independent sub-tasks.
- Create `test_scheduler.py` to verify:
  - In-order insertion and extraction from the Priority Queue heap.
  - Ticking of the scheduler worker and timely triggering of alerts.

### Manual Verification
- Start the FastAPI backend server:
  `venv\Scripts\uvicorn app.main:app --reload`
- Call the `/api/chat` endpoint with scheduling prompts.
- Verify that Google Calendar is updated.
- Call `/api/chat` with undo requests and verify events are deleted.
- Test `/api/calendar/events` and `/api/reminders` API endpoints to ensure correct JSON payload delivery.
- Execute multi-step user prompts via `/api/chat` and verify that the console logs show the tasks executed in the exact dependency order.

---

# Implementation Plan: Phase 3 - Frosted-Glass Gemini Overlay & Voice Assistant

This phase implements a premium, interactive Google Gemini/Assistant-style overlay served directly by the FastAPI backend. It features voice wake-up, speech recognition, speech synthesis, and floating visual triggers for a true laptop assistant experience.

## User Review Required

> [!IMPORTANT]
> - **Gemini-Style UI Overlay**: The interface is styled as a full-screen frosted glass overlay with a blurred background. The assistant input and glowing soundwave animations float at the bottom center.
> - **Two Launch Options**:
>   1. **Voice Wake Word**: A background Python daemon `voice_wake.py` runs on the laptop listening for *"Hey TaskFlow AI"*. When detected, it opens the assistant page in the browser automatically.
>   2. **Floating Circle Button**: A small, glowing, glassmorphic circular orb floats in the bottom-right corner of the screen. Clicking it opens the full overlay.
> - **Voice & Type Command**: The UI supports both voice capture (Web Speech API) with animated soundwaves and typing commands manually.
> - **Task Allotted Display**: When a calendar event or reminder is created, the assistant displays a clear, elegant floating card containing all details and a direct Google Calendar link.
> - **Speech Synthesis**: The assistant speaks responses back using the browser's speech synthesis.

---

## Proposed Changes

### Core API & Frontend Setup

#### [MODIFY] [main.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/main.py)
- Add route `GET /dashboard` returning the premium styled Gemini-style HTML template (`HTMLResponse`) containing the voice assistant, glowing soundwaves, floating trigger widget, and task status cards.

#### [NEW] [voice_wake.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/voice_wake.py)
- Create a lightweight daemon script using Python's `speech_recognition` package (or basic mic threshold check if packages aren't fully installed) that runs in the background.
- Upon hearing *"Hey TaskFlow AI"*, it executes `webbrowser.open("http://localhost:8000/dashboard")` to pop up the assistant.

---

## Verification Plan

### Manual & UI Verification
- Start the server using `venv\Scripts\python.exe -m uvicorn app.main:app --port 8000`.
- Start the voice wake daemon using `.venv\Scripts\python.exe voice_wake.py`.
- Verify the following:
  1. Saying *"Hey TaskFlow AI"* automatically launches the browser and opens the overlay.
  2. Clicking the floating circular orb opens/closes the frosted glass overlay.
  3. Clicking the mic button triggers speech recognition with a pulsing, colorful soundwave.
  4. Scheduling a meeting or reminder shows a "Task Allotted" success card.
  5. Typing or saying *"Wait, undo that"* deletes the event/reminder and updates the display.

---

# Implementation Plan: Phase 4 - Conflict & Risk Detection

This plan details the addition of calendar overlap checking before event booking, conflict resolution dialog flows (double-booking vs rescheduling), and progressive UI loading text.

## Phase 4 User Review Required

> [!NOTE]
> - Conflict checking relies on querying existing events for the day of the requested meeting. Overlaps are detected mathematically (UTC normalized).
> - If an overlap is detected, the calendar booking is deferred, dependent tasks (like reminders) are skipped in the graph, and the user is prompted for resolution.
> - Bypassing a conflict is handled by passing `double_book: true` from the LLM parser when the user explicitly requests to double-book.

## Phase 4 Proposed Changes

---

### Backend Logic

#### [MODIFY] [models.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/models.py)
- Add `double_book: Optional[bool] = False` parameter to the `EventParameters` pydantic model.

#### [MODIFY] [calendar.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/tools/calendar.py)
- Implement `parse_google_datetime(dt_str: str) -> datetime` to convert Google Calendar API timezone-offset datetimes into naive UTC datetimes.
- Implement `check_calendar_conflict(start_iso: str, end_iso: str) -> dict` to fetch primary calendar events for the target day and check for overlaps (`exist_start < req_end` and `exist_end > req_start`).

#### [MODIFY] [engine.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/engine.py)
- Update `handle_create_calendar_event` to execute `check_calendar_conflict` when `params.double_book` is false.
- If a conflict is found, return a dictionary with status `"conflict"`, the conflict reply message, and the details (end time of the conflicting event for the alternative timing prompt).

#### [MODIFY] [dag_planner.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/dag_planner.py)
- Update task node dependency waiting loop in `execute_node` to abort and fail/skip if any parent dependency finishes with a status of `"conflict"`.

#### [MODIFY] [parser.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/parser.py)
- Update `SYSTEM_PROMPT` to add:
  - **Calendar Conflict Rule**: If the user explicitly asks to "double book", "schedule anyway", or "overwrite", set the `double_book` parameter of `create_calendar_event` to `true`.
  - **Conflict Response Template**: Instruct the LLM on how to construct the conflict reply message, matching the style: *"You're booked until [end_time]. Want [event_summary] at [alternative_time] instead, or double-book?"*

---

### Frontend UI

#### [MODIFY] [dashboard.html](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/templates/dashboard.html)
- Update `submitPrompt` to use `setInterval` to progressively rotate loading messages (e.g., *"Processing request..."*, *"Checking calendar conflicts..."*, *"Analyzing schedule details..."*) inside the assistant thinking bubble.
- Clear the interval upon receiving the response.

## Phase 4 Verification Plan

### Automated Tests
- Create a test script `backend/test_conflicts.py` that mocks calendar events and asserts that conflict checking returns the correct conflict messages and succeeds with the `double_book` bypass flag.

### Manual Verification
1. Open the assistant dashboard.
2. Schedule a meeting: *"Schedule a sync with Sanika tomorrow from 3 PM to 4 PM IST."* (It will book successfully).
3. Schedule a conflicting meeting: *"Schedule a sync with Sherwin tomorrow at 3 PM IST."*
4. Verify that:
   - The assistant displays progressive thinking text: *"Checking calendar conflicts..."*
   - The assistant refuses to schedule and asks: *"You're booked until 4:00 PM IST. Want sync with Sherwin at 4:00 PM IST instead, or double-book?"*
   - No allotted cards are generated.
5. Reply: *"double book"*
6. Verify that:
   - The meeting is successfully scheduled alongside the conflict.
   - The allotted card for Sherwin is generated.

---

# Implementation Plan: Phase 5 - Google Login Page with Gemini-Style Effects

This plan details the addition of a Google Sign-In login page as the entry point for TaskFlow AI, featuring a premium glassmorphic login card, glowing text, and an animated Gemini-style Google Login button.

## Phase 5 User Review Required

> [!NOTE]
> - A new route `GET /login` will serve the Google Login page (`login.html`).
> - The route `GET /dashboard` will check for the `logged_in=true` cookie. If missing, it redirects to `/login`.
> - The root route `GET /` will redirect to `/dashboard` (and consequently to `/login` if unauthenticated).
> - Logging out will clear the cookie and redirect back to `/login`.

## Phase 5 Proposed Changes

---

### Backend Logic & Routes

#### [MODIFY] [main.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/main.py)
- Import `RedirectResponse` and `Cookie` from fastapi.
- Add `GET /login` endpoint returning `login.html`.
- Add `GET /` endpoint redirecting to `/dashboard`.
- Update `GET /dashboard` endpoint to inspect the `logged_in` cookie. If not present or not equal to `true`, return a `RedirectResponse` to `/login`.

#### [NEW] [login.html](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/templates/login.html)
- Create a new template file containing:
  - Frosted glass login card centered on the screen.
  - A glowing, rotating Gemini-style color gradient background ring.
  - A **Sign In with Google** button with a Google logo and an animated glowing gradient border that cycles colors smoothly.
  - JavaScript logic to set the `logged_in=true` cookie on click and redirect to `/dashboard`.

---

### Frontend UI

#### [MODIFY] [dashboard.html](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/templates/dashboard.html)
- Add a premium **Logout** button styled as a glassmorphic circular icon in the top-right corner of the launch screen.
- Implement logout JavaScript handler that clears the cookie and redirects to `/login`.

## Phase 5 Verification Plan

### Manual Verification
1. Navigate to `http://127.0.0.1:8000/`.
2. Verify that it automatically redirects to the new login page (`/login`).
3. Verify that the login page matches the warm cream/amber frosted theme and displays a glowing Gemini-style border around the Google button.
4. Click the Google button and verify that it redirects to `/dashboard`.
5. On the dashboard, click the Logout button in the top-right corner.
6. Verify that it logs you out and redirects back to `/login`.

---

# Implementation Plan: Phase 6 - Relational Database Integration

This plan details migrating the project from file-based (JSON) storage and in-memory stacks to a full SQLite SQL database, leveraging the normalized schema from the Entity-Relationship Diagram (ERD).

## Phase 6 User Review Required

> [!NOTE]
> - Database engine will connect to `sqlite:///taskflow_ai.db` locally, easily swappable to PostgreSQL.
> - Reminders, tasks, calendar events, sessions, and action logs will be managed by SQLAlchemy ORM.
> - Existing tokens and calendar services will continue to run seamlessly using credentials from the active user's DB record.

## Phase 6 Proposed Changes

---

### Database Configurations & Models

#### [MODIFY] [session.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/db/session.py)
- Update connection engine to use SQLite database `taskflow_ai.db` as default.
- Enable `check_same_thread=False` for thread-safety in uvicorn.

---

### Backend Logic & Persistence Upgrade

#### [MODIFY] [reminders.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/tools/reminders.py)
- Replace JSON reads/writes with SQLAlchemy database queries referencing the `Reminder` model.
- Port helper functions `_load_reminders` and `_save_reminders` to DB session transaction blocks.

#### [MODIFY] [undo.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/undo.py)
- Configure the Python fallback stack to write entries into the `ActionLog` table.
- Read historical logs from DB to pop and execute undo actions.

#### [MODIFY] [engine.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/engine.py)
- Integrate DB transaction session in `execute_user_request`.
- Insert a core `Task` record for every request, self-referencing sub-tasks for dependency mapping.
- Relate created events/reminders back to the `Task` record.

#### [MODIFY] [main.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/main.py)
- Import `Base` from `models.py` and call `Base.metadata.create_all(bind=engine)` inside the lifespan startup context to automatically initialize the SQLite DB.
- Set up DB dependency injection for Chat and Reminder API endpoints.

## Phase 6 Verification Plan

### Automated Verification
- Rerun conflict tests:
  ```powershell
  venv\Scripts\python.exe test_conflicts.py
  ```

### Manual Verification
1. Open the browser to `http://127.0.0.1:8000/`.
2. Complete Google Login, redirecting to the dashboard.
3. Schedule a calendar event: *"Schedule Scrum in 5 days from 4 PM to 5 PM UTC and remind me 30 mins before."*
4. Verify that:
   - The SQLite database file `taskflow_ai.db` is successfully created.
   - Both events and reminders are populated in the database.
   - Cards render successfully.
5. Click **Undo** and verify that the database record is removed and the event is deleted from Google Calendar.

---

# Implementation Plan: Phase 7 - Advanced Customizations & Multi-User Context Routing

This plan details implementing custom interface scaling, multi-user context isolation, dynamic suggestion states, and atomic transaction rollbacks for TaskFlow AI.

## Phase 7 Proposed Changes

### UI & Aesthetics Polish
1. **Centered Splash Typography**: Move background `.launch-orb` to `position: absolute` behind the branding titles to guarantee vertical alignment.
2. **Symmetric Input Bar & Speaker Relocation**: Relocate voice toggle (`#speak-toggle`) to the left of the text input container, removing the legacy `+` markup.
3. **Settings Modal Card & Sign Out**: Integrate Settings modal card holding the theme toggle and Cookie-clearing sign out actions.
4. **Warm Cream Light Theme**: Declare light mode variables providing a warm oatmeal and amber backdrop.

### Database & Autocomplete
5. **Database Schema Auto-Migrations**: Check and execute an SQL column migration appending `profile_pic` to the `users` table on server startup.
6. **Animated Search Keyword Chips**: Position hashtag chips under the search bar that animate on focus and populate search values on click.
7. **Conversations Drawer Sidebar**: Support a slide-out hamburger navigation drawer holding past conversation indices.

### Contrast & Redirections
8. **Contrast Visibility on Task Cards**: Replace static card text colors with `var(--text-main)` and `var(--text-muted)` to match active themes.
9. **Google Calendar View Redirect (`view_calendar`)**: Register `"view_calendar"` tool action mapping to dynamic calendar launches.
10. **Calendar Conflict Filter Bugfix**: Skip cancelled/deleted events (`status == 'cancelled'`) during Google conflict validation.

### Session Context & Reversals
11. **User Session-Bound OAuth Mapping**: Bind execution threads to the database credentials of the active user session using `contextvars.ContextVar`.
12. **User-Specific Email URL Parameters**: Route redirects and links using `r?authuser={email}` parameters.
13. **Grouped / Atomic Undo Operations**: Revert sibling sub-tasks sharing the same `parent_task_id` atomically in a single undo transaction.

## Phase 7 Verification Plan

### Automated Verification
- Verify grouped undos and timezone constraints using test files.

### Manual Verification
1. Log in with a Google account and type *"take me to the calendar"*. Verify that the calendar opens in a new tab with the correct `authuser` parameter matching the email.
2. Schedule a meeting and type *"undo"*. Verify that both the calendar event and the local reminder card disappear from the screen and the DB together in a single command.

---

# Implementation Plan: Phase 8 - Proactive Calendar Overrun Checker

This phase implements a background service that periodically polls the active user's Google Calendar to detect if a current meeting has overrun its scheduled time while a subsequent meeting is starting soon, proactively alerting the user on their dashboard.

## Phase 8 User Review Required

> [!NOTE]
> - **In-Memory Suggestion Store**: Active alerts are cached in an in-memory dictionary `proactive_suggestions` mapped by `user_id` inside `scheduler.py` to prevent database lock issues during periodic background polling threads.
> - **Notification-Only Display**: The frontend notification card will display the overrun alert details and a dismiss button. The actual email drafting action is moved to future enhancements.

## Phase 8 Proposed Changes

### 1. Overrun Detection Background Service
#### [MODIFY] [scheduler.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/scheduler.py)
- Implement `proactive_suggestions` global storage dict.
- Implement `start_proactive_calendar_checker(app)`:
  - Run every 15 seconds.
  - Query active user database sessions from SQLite.
  - Set thread context credentials (`active_user_token`, `active_user_id`, `active_user_email`) dynamically.
  - Poll the active user's primary calendar events.
  - Detect overrun cases: an event `current_event` has passed its end time (`end_time < now <= end_time + 15 mins`), and a `next_event` is scheduled to start soon (`now - 5 mins <= start_time_next <= now + 15 mins`).
  - Cache unique suggestions inside `proactive_suggestions[user_id]` using `(user_id, current_event_id, next_event_id)` keys to prevent duplicate suggestions.

### 2. API Integrations
#### [MODIFY] [main.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/main.py)
- Start `start_proactive_calendar_checker` in the lifespan startup block.
- Add `GET /api/proactive-suggestions` returning active alerts for the current logged-in session, popping them upon retrieval to ensure one-time delivery.

### 3. Frontend Polling & Alert UI
#### [MODIFY] [dashboard.html](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/templates/dashboard.html)
- Add a periodic AJAX/fetch loop (polling `/api/proactive-suggestions` every 10 seconds).
- Render incoming overrunning suggestions as a floating toast alert card inside the chat workspace with a close/dismiss button.

## Phase 8 Verification Plan

### Manual Verification
1. Create a simulated meeting sequence on the logged-in Google Calendar:
   - Meeting A (running late): Scheduled to end 5 minutes ago.
   - Meeting B (upcoming): Scheduled to start now.
2. Open the dashboard and wait up to 15 seconds. Verify that a proactive card appears showing: *"Your meeting Meeting A has run over. Meeting B is starting soon."*
3. Click **Dismiss** and verify that the card disappears.

---

# Implementation Plan: Phase 9 - Unified Action Ledger, undo_last_action() and Testing Suite

This phase aligns the database schema and log wrapping logic to match the exact `actions` ledger specification, introduces a unified `undo_last_action()` routing process, and implements an independent testing suite for verification.

## Phase 9 Proposed Changes

### 1. Unified Actions Schema
#### [MODIFY] [models.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/db/models.py)
- Define the new `Action` table mapping:
  * `id` (primary key Uuid)
  * `timestamp` (UTC DateTime)
  * `action_type` (String)
  * `action_payload` (Text JSON)
  * `reason` (Text plain language)
  * `undo_action` (Text JSON)
  * `status` (String: active/undone)
  * `user_id` (Uuid index for multi-user security)
  * `parent_task_id` (Uuid index for atomic grouped undo tracking)

### 2. Action Logger Helper
#### [NEW] [actions_logger.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/actions_logger.py)
- Create `log_action(action_type, action_payload, reason, undo_action_payload, parent_task_id)`:
  - Fetch user ID from context.
  - Insert record into the `actions` table with status `"active"`.
- Create `undo_last_action()`:
  - Find the most recent active action for the current user.
  - If it belongs to a grouped parent task, retrieve all active actions sharing that `parent_task_id`.
  - Process undo reversals sequentially (deleting Google Calendar events and local reminders).
  - Update their status to `"undone"`.
  - Compile and return a plain-language confirmation.

### 3. Execution Logging
#### [MODIFY] [engine.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/engine.py)
- Update creation routes to capture and pass original payloads, reasons, and target task grouping keys to `log_action`.
- Refactor the `handle_undo` action route to invoke `undo_last_action()` and return its result.

### 4. Lifespan Schema Auto-Creation
#### [MODIFY] [main.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/main.py)
- Run DB auto-migration check to instantiate the `actions` table on startup.

### 5. Unified Testing Suite
#### [NEW] [test_core_features.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/test_core_features.py)
- Implement three unit tests:
  1. **Undo Reversal Verification**: Creates a calendar event + reminder, checks that they are logged to `actions` with status `"active"`, invokes `undo_last_action()`, and verifies that both records are marked `"undone"` and deleted from the calendar/reminder stores.
  2. **Conflict Detection Accuracy**: Asserts that `check_calendar_conflict` correctly flags overlapping times vs. free time windows.
  3. **Proactive Overrun Rule Matches**: Validates that the background checker triggers suggestion warnings *only* when overrun conditions are met.

## Phase 9 Verification Plan

### Automated Verification
- Run the unified testing suite:
  ```powershell
  venv\Scripts\python.exe test_core_features.py
  ```

---

# Implementation Plan: Phase 10 - Proactive Buffer Suggestion Rule (Rule 2)

This phase implements Rule 2 (Back-to-back meeting checking with zero buffer) in the background Proactive Checker loop, and adds a test case validating the rule match.

## Phase 10 Proposed Changes

### 1. Back-to-Back Meeting Logic
#### [MODIFY] [scheduler.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/scheduler.py)
- Inside the `start_proactive_calendar_checker` background loop, check if any event's end time matches the start time of another subsequent event (difference < 60 seconds).
- Trigger a suggestion: *"You have back-to-back meetings ('[Meeting A]' and '[Meeting B]') with no buffer. Recommend setting a 5-minute buffer?"*
- Prevent duplicate alerts using a distinct session conflict key `(user_id, "b2b", event_a_id, event_b_id)`.

### 2. Expanded Test Suite
#### [MODIFY] [test_core_features.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/test_core_features.py)
- Add a new test case `test_proactive_back_to_back` to `test_core_features.py` that configures two back-to-back events (e.g. 15:00-16:00 and 16:00-17:00).
- Run the checker, assert that a back-to-back notification alert is cached, and verify no alerts are generated when meetings have a healthy buffer gap.

## Phase 10 Verification Plan

### Automated Verification
- Run the testing suite:
  ```powershell
  venv\Scripts\python.exe test_core_features.py
  ```

---

# Implementation Plan: Phase 11 - UI Polish & Interactive Control Upgrades

This phase implements three major frontend and backend upgrades to the chat interface to make it feel extremely premium, responsive, and state-of-the-art:
1. **Interactive Option Selection Cards**: Convert conflict responses into clickable, glassmorphic buttons inside the chat bubble (e.g. `[ Reschedule to 8:30 PM ]` and `[ Double-book ]`) to eliminate the need for manual typing.
2. **Visual LLM 'Thinking' & Planning State**: Add a morphing glassmorphic gradient wave/pulse animation above the input bar when the agent is planning/executing tasks.
3. **Interactive Action Trail Ledger in Drawer**: Add a dedicated visual log tab inside the sidebar drawer showing the full history of executed actions (Active/Undone) with inline 'Undo' buttons for each.

## Phase 11 Proposed Changes

### 1. Action Trail Database & Undo Routes
#### [MODIFY] [actions_logger.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/actions_logger.py)
- Create `undo_specific_action(action_id: uuid.UUID)` to reverse a specific action entry and its grouped parent task siblings, modifying database statuses atomically.

#### [MODIFY] [main.py](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/main.py)
- Create `GET /api/actions` to query the SQLite `actions` table for the logged-in user, sorted chronologically descending.
- Create `POST /api/actions/{action_id}/undo` to execute `undo_specific_action(action_id)` and return JSON status messages.

### 2. User Interface Enhancements
#### [MODIFY] [dashboard.html](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/templates/dashboard.html)
- **Thinking Wave**: Add a `.thinking-wave` container above the input bar styled with premium CSS animations (gradient shift, scale pulse). Show/hide during AJAX chat requests.
- **Interactive Options Parser**: Parse assistant responses that mention conflict options. Generate inline click-handlers inside the message bubble that auto-submit decisions (e.g., "8:30 PM" or "double book") to the chat.
- **Action Trail Ledger in Drawer**: Append an "Action History" scrollable panel inside the left sidebar drawer. Implement a JS function `loadActionHistory()` that runs on load, drawer expansion, and post-action events. Render actions with status tags (`Active` vs `Undone`) and inline `Undo` buttons that call `/api/actions/{action_id}/undo` via AJAX.

## Phase 11 Verification Plan

### Manual Verification
- Schedule a conflicting meeting, verify that clickable option buttons appear inside the chat bubble, and click one to resolve the conflict.
- Open the drawer sidebar and verify that all calendar/reminder creations are rendered in the visual Action History list.
- Click 'Undo' on a specific item in the ledger and verify that it deletes the target item from Google Calendar/reminders, and marks it as "undone" in the sidebar list.
- Verify the presence of the glassmorphic wave thinking indicator when typing a message and waiting for the AI response.

---

# Implementation Plan: Phase 12 - Premium Dark Mode Customization & UI Polish

This phase implements a premium, comprehensive redesign of the Light Mode and Dark Mode styling systems. It transitions the default Dark Mode from a basic black theme to a rich, espresso-charcoal gradient theme with subtle amber radial glows, glowing outlines, tactile inner shadows, matte-black gradient sidebars, and translucent glassmorphic cards (10-20% opacity with backdrop blur) to highlight automation and AI actions.

## Phase 12 User Review Required

> [!IMPORTANT]
> - **Unified Theme Variable Architecture**: Theme variables are expanded to style cards, sidebars, active highlights, buttons, and AI feedback panels cleanly via CSS variable overrides.
> - **Rich Dark Mode Gradient Background**: The application background changes from solid dark gray to a deep charcoal/espresso gradient (`#12100f` base) with a warm central glow (`rgba(255, 145, 0, 0.07)`).
> - **Glassmorphic AI Cards**: Assistant bubbles and task cards will appear as translucent glass panels (18% opacity) with a beautiful glowing orange border (`rgba(255, 145, 0, 0.35)`) and a subtle orange drop-shadow aura to highlight automation.
> - **Sidebar Redesign**: The left sidebar in Dark Mode is changed to a matte black gradient (`linear-gradient(180deg, #0f0f11 0%, #070708 100%)`) with active icons highlighted using a warm glowing orange background aura and border.
> - **Tactile Icons and Buttons**: Buttons and chips (e.g. suggestion chips, model selectors, action items) use glowing outlines and inner shadows in Dark Mode, and flat warm-beige/cream neumorphic aesthetics in Light Mode.

## Phase 12 Proposed Changes

### User Interface Customizations
#### [MODIFY] [dashboard.html](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/templates/dashboard.html)
- **CSS Variables (:root / body.light-theme)**:
  - Add `--bg-gradient` representing the body radial gradient backgrounds.
  - Define panel styles (`--card-bg`, `--drawer-bg`, `--input-bg`, `--border-color`, `--gradient-center`) for rich graphite/glass look.
  - Define button styling variables (`--btn-border`, `--btn-shadow`, `--btn-hover-border`, `--btn-hover-shadow`) to handle glowing outline borders and inner shadows in dark mode, and soft borders in light mode.
  - Define AI card variables (`--ai-card-bg`, `--ai-card-border`, `--ai-card-shadow`) to implement semi-transparent glass with orange glow borders and dropshadow glows in dark mode, and solid cream panels in light mode.
  - Define sidebar highlight variables (`--sidebar-active-color`, `--sidebar-active-bg`, `--sidebar-active-border`, `--sidebar-active-shadow`).
  - Define custom undo button colors (`--undo-btn-text`, `--undo-btn-bg`, `--undo-btn-hover-bg`, `--undo-btn-hover-text`).
- **Layout & Selectors**:
  - Update `body` rule to use `var(--bg-primary)` and `var(--bg-gradient)`.
  - Update `.sidebar` rule to use `var(--sidebar-bg)`.
  - Style `.sidebar-icon` with `border: 1px solid transparent` and apply sidebar hover/active variables.
  - Update buttons (`.action-icon-btn`, `.gemini-input-btn`, `.suggest-chip`, `.model-selector`, `.chat-option-btn`) to use `--btn-border` and `--btn-shadow` rules, along with their hover state enhancements.
  - Update `.bubble.assistant-bubble` and `.task-card` to use `--ai-card-bg`, `--ai-card-border`, `--ai-card-shadow`, and `backdrop-filter: blur(12px)`.
  - Update `.proactive-toast` to use `--ai-card-bg`, `--ai-card-border`, `--ai-card-shadow` and clean up legacy `.dark-mode` CSS references.
  - Update tagline color to `var(--text-muted)` so it remains readable on dark backgrounds.
  - Change thinking wave bars to use `var(--gemini-gradient)`.

## Phase 12 Verification Plan

### Manual Verification
- Start the server:
  `venv\Scripts\python.exe -m uvicorn app.main:app --port 8000`
- Open the dashboard in the browser: `http://localhost:8000/dashboard`
- Click the settings gear icon to open the settings modal.
- Verify that toggling **Light Theme Mode** checkbox switches the UI background, sidebar, buttons, and cards between the ivory/cream style and the dark graphite/espresso glow styles.
- Verify that in Dark Mode:
  - The background has a warm espresso/charcoal radial gradient.
  - The sidebar is matte black, and active icons have an orange border and glow.
  - Assistant bubbles and task cards have a translucent glassmorphic look (blur + border glow).
  - Buttons and chips have glowing amber outlines and shadows.
- Verify that in Light Mode:
  - The background is a warm ivory gradient.
  - The sidebar is matte beige.
  - Assistant bubbles and task cards are solid beige.
  - Buttons and chips are flat with soft borders.