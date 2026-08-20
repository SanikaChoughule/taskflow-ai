# Project Structure & Architecture Notes

This document provides a hierarchical breakdown of the **TaskFlow AI** project structure, detailing the role and purpose of each directory and file in sequential order.

---

## 📁 Root Directory (`taskflow-ai/`)
The main repository workspace containing project configurations, build artifacts, documentation files, and the core backend service.

* **`.venv/`** — Workspace-level Python virtual environment.
* **`.vscode/`** — Visual Studio Code settings, tasks, and debugger launch configurations.
* **`build/`** — Generated build logs and compiled binary files.
* **`implementation_plan.md`** — The approved blueprint outlining the architectural goals, changes, and verification strategies.
* **`task.md`** — Active checklist of completed, in-progress, and pending development tasks.
* **`walkthrough.md`** — Walkthrough report summarizing implemented features, verified test runs, and system confirmations.
* **`SETUP.md`** — Comprehensive setup, execution, and troubleshooting guide for starting the project and handling compilation options.
* **`project_notes.md`** *(This File)* — Hierarchical reference guide explaining the project structure.

---

## 📁 Backend Directory (`backend/`)
Contains all service logic, REST API endpoints, the agent implementation, test suites, and configurations.

### Root Files in `backend/`
* **`.env`** — Configuration file managing environment variables (such as `GROQ_API_KEY` and `GEMINI_API_KEY`).
* **`.gitignore`** — Configures untracked files and folders that Git should ignore (e.g., credentials, caches, virtual environments).
* **`credentials.json`** — Google Cloud Platform OAuth 2.0 client secret file used to authorize Google Calendar API access.
* **`list_events.py`** — Testing script that directly queries and lists upcoming active Google Calendar events using the API helper.
* **`list_models.py`** — Verification script that queries and displays available models from the Google GenAI service.
* **`reminders.json`** — A persistent flat JSON file acting as the local database for user reminders.
* **`setup.py`** — Dispatches compilation instructions to build the C++ stack extension (`dsa_engine`) using `pybind11` and custom compiler flags.
* **`test_calendar.py`** — Verifies Google Calendar creation, stack push, and manual deletion/undo functionality.
* **`test_engine.py`** — Verifies the full agent loop (natural language request parsing -> scheduling -> natural language undo).
* **`test_parser.py`** — Unit test for testing structured JSON outputs from the LLM parser.
* **`test_user_case.py`** — Custom test file executing a target calendar event prompt without triggering an immediate undo.
* **`token.json`** — Cached user OAuth credentials, generated automatically after the first Google Calendar authentication flow.
* **`venv/`** — Local Python virtual environment containing the python binary, pip packages, and libraries used by the backend.

---

### 📁 Core App Package (`backend/app/`)
Implements the main application routes, configuration, database placeholders, and background agent routines.

* **`__init__.py`** — Marks the directory as a Python package.
* **`agent_service.py`** — Alternative agent service implementing native tool-calling JSON schemas via the Groq API.
* **`database.py`** — Placeholder module for database connections and sessions (currently empty).
* **`main.py`** — Entrypoint of the FastAPI web application, defining CORS policies and endpoints:
  * `GET /` — Service status check.
  * `POST /api/chat` — Core chat agent endpoint.
  * `GET /api/calendar/events` — Upcoming calendar feed.
  * `GET /api/reminders` / `POST /api/reminders` / `DELETE /api/reminders/{id}` — Reminders CRUD.

---

### 📁 Agent Subpackage (`backend/app/agent/`)
Orchestrates the intelligent decision-making logic, prompt classification, and the LIFO undo stack.

* **`__init__.py`** — Marks the directory as a package.
* **`engine.py`** — Main execution agent handler. Parses prompt intent via `parser.py`, invokes target tools (calendar/reminders), and manages reverse action undoing by popping from the global stack.
* **`models.py`** — Pydantic model schemas validating event parameters and structured JSON responses returned by the LLM.
* **`parser.py`** — Configures the system instructions (role, rules, output JSON schema) and calls the Groq API to parse prompts.
* **`undo.py`** — Initiates the global stack tracker. Safely attempts to import the compiled C++ stack; falls back to an identical pure Python implementation if missing.
* **`undo_stack.cpp`** — C++ source file implementing a fast, thread-safe memory stack, compiled into Python bytecode using `pybind11`.

---

### 📁 Tools Subpackage (`backend/app/tools/`)
Contains integration helpers that interface with external APIs or local persistence models.

* **`__init__.py`** — Marks the directory as a package.
* **`calendar.py`** — Integrates with the Google Calendar API. Fetches oauth credentials, schedules calendar events, registers undo items to the stack, and deletes events. Handles dynamic user session credentials loading.
* **`reminders.py`** — Manages CRUD operations on the SQLite database, generating reminder items and registering undo entries.

---

### 📁 Database Subpackage (`backend/app/db/`)
Manages configuration and model mapping for the relational SQL database.
* **`session.py`** — Configures SQLite engine connections and provides database sessions.
* **`models.py`** — Defines SQLAlchemy model declarations (`User`, `DBSession`, `Task`, `CalendarEvent`, `Reminder`, `ActionLog`).

---

### 📁 Templates Directory (`backend/app/templates/`)
Holds frontend UI screens.
* **`login.html`** — Account chooser interface and Google Sign-In redirect page.
* **`dashboard.html`** — Frosted glass workspace assistant containing sidebar drawer, Settings dialog card, voice toggle, soundwave animations, search keywords, and task visualization cards.

---

## 🚀 Phase 7 Feature Summaries & Sequential Implementation Notes
Below is the list of recent structural enhancements completed on the codebase:
1. **Centered Splash Typography**: Centered launch screen branding title and tagline vertically and horizontally by positioning `.launch-orb` absolutely behind the text to prevent layout shifting.
2. **Symmetric Input Bar & Speaker Relocation**: Removed the legacy `+` icon from the input bar and relocated the voice toggle (`#speak-toggle`) to the left of the input field.
3. **Settings Modal Card & Sign Out**: Added a centered Settings modal containing a theme toggle switch and a red **Sign Out** button linked to redirect flows.
4. **Warm Cream Light Theme**: Configured theme overrides using warm oatmeal cream backgrounds, sandy borders, and amber user chat bubbles with high-contrast text.
5. **Database Schema Auto-Migrations**: Added startup column migrations to prevent SQLite/Google auth session validation loops by automatically appending `profile_pic` to the `users` table.
6. **Animated Search Keyword Chips**: Added hashtag chips (`#Scrum`, `#Meeting`, `#Reminder`, `#Tomorrow`) under the search bar that slide out on focus and populate inputs on click.
7. **Conversations Drawer Sidebar**: Implemented a sliding drawer expanding from `0px` to `260px` in width triggered by clicking the hamburger menu to display past conversation logs.
8. **Contrast Visibility on Task Cards**: Modified the CSS variables in the card selectors (`task-card-title`, `task-card-details`) to use variable bindings, resolving visibility issues in Light mode.
9. **Google Calendar View Redirect (`view_calendar`)**: Added a new `"view_calendar"` action, mapping prompts like *"take me to the calendar"* to a text/voice response and launching Google Calendar in a new tab.
10. **Calendar Conflict Filter Bugfix**: Configured the Google Calendar conflict check loop in `calendar.py` to skip cancelled/deleted events (`status == 'cancelled'`), preventing false conflict errors.
11. **User Session-Bound OAuth Mapping**: Implemented thread-safe/async-safe context variable mappings (`active_user_token` and `active_user_id`) to load Google credentials dynamically for the active user session.
12. **User-Specific Email URL Parameters**: Customized the redirection URL to open the specific logged-in user's calendar (`r?authuser={email}`) and appended `&authuser={email}` to event links.
13. **Grouped / Atomic Undo Operations**: Refactored the `handle_undo` action in `engine.py` to pop and revert sibling sub-tasks sharing the same `parent_task_id` atomically, deleting both calendar events and reminders in a single command.
14. **Proactive Calendar Overrun Checker**: Integrated a background checking loop that polls calendar events for active sessions, finds overrunning meetings, and pushes floating toast alerts with Dismiss buttons to the dashboard via a new `/api/proactive-suggestions` endpoint.
15. **Unified Action Ledger, undo_last_action() and Testing Suite**: Aligned the database schema with the exact `actions` ledger specification, implemented standard state logs with reasons, rebuilt atomic grouped undos in `actions_logger.py`, and introduced a unified, self-contained test suite (`test_core_features.py`) asserting all undo, conflict, and suggestion rule conditions.
16. **Proactive Buffer Suggestion Rule (Rule 2)**: Added zero-buffer back-to-back meeting detection to the background checker loop, raising warnings to suggest a 5-minute buffer, and configured assertions in the unit test suite.
17. **Strict Timezone Conversion Math**: Added clear timezone arithmetic subtraction instructions and examples to the parser system prompt to enforce correct UTC timestamps calculation for IST scheduling requests.
18. **UI Polish & Interactive Control Upgrades (Phase 11)**: Integrated interactive glassmorphic option selection buttons inside conflict response bubbles, implemented an animated gradient wave LLM thinking indicator above the text input pill, created REST endpoints (`GET /api/actions`, `POST /api/actions/{id}/undo`), and designed a split-view Action History log panel inside the sidebar drawer for specific event rollbacks.
