# Phase 1 - Core Backend & Integrations
- [x] Compile C++ extension with MinGW using custom setup.py (Attempted; resolved gracefully with pure Python fallback)
- [x] Implement C++ and Python fallback logic in `undo.py`
- [x] Add reminder parameters in `models.py`
- [x] Create persistent JSON-based reminder utility in `reminders.py`
- [x] Update `calendar.py` with absolute paths and listing method
- [x] Refine `parser.py` prompting and temperature settings
- [x] Fully implement tool invocation and actual undo actions in `engine.py`
- [x] Add REST API endpoints to `main.py`
- [x] Run validation tests to verify correctness

# Phase 2 - Hybrid LLM + Classical DSA Engine
- [x] Implement HashMap Action Router in `backend/app/agent/engine.py`
- [x] Implement C++ Priority Queue (Min-Heap) in `backend/app/agent/undo_stack.cpp` and export to Python
- [x] Implement Python fallback and wrapper for Priority Queue in `backend/app/agent/scheduler.py`
- [x] Add background polling/scheduler lifespan task to FastAPI application in `backend/app/main.py`
- [x] Implement Task Dependency Graph (DAG) logic in `backend/app/agent/dag_planner.py`
- [x] Update `backend/app/agent/parser.py` schema & prompting to support multi-task parsing with dependencies
- [x] Update `backend/app/agent/engine.py` to compile, topologically sort, and execute sub-tasks using the DAG Graph
- [x] Verify implementation with unit tests: `test_dag.py` and `test_scheduler.py`

# Phase 3 - Frosted-Glass Gemini Overlay & Voice Assistant
- [x] Create Python background voice wake-up daemon `voice_wake.py`
- [x] Implement Gemini-style frosted-glass web assistant template served from `GET /dashboard` in `backend/app/main.py`
- [x] Implement floating circular launcher orb widget at bottom-right corner of screen
- [x] Integrate Web Speech API (`SpeechRecognition`) for browser microphone command input
- [x] Add animated neon soundwave indicator during listening state
- [x] Implement task status cards for allotting events/reminders and direct Google Calendar links
- [x] Implement Speech Synthesis to speak responses back
- [x] Run end-to-end manual checks to verify trigger, voice/type execution, and undo flow

# Phase 7 - Advanced Customizations & Multi-User Context Routing
- [x] 1. **Centered Splash Typography**: Centered launch screen branding title and tagline vertically and horizontally by positioning `.launch-orb` absolutely behind the text to prevent layout shifting.
- [x] 2. **Symmetric Input Bar & Speaker Relocation**: Removed the legacy `+` icon from the input bar and relocated the voice toggle (`#speak-toggle`) to the left of the input field.
- [x] 3. **Settings Modal Card & Sign Out**: Added a centered Settings modal containing a theme toggle switch and a red **Sign Out** button linked to redirect flows.
- [x] 4. **Warm Cream Light Theme**: Configured theme overrides using warm oatmeal cream backgrounds, sandy borders, and amber user chat bubbles with high-contrast text.
- [x] 5. **Database Schema Auto-Migrations**: Added startup column migrations to prevent SQLite/Google auth session validation loops by automatically appending `profile_pic` to the `users` table.
- [x] 6. **Animated Search Keyword Chips**: Added hashtag chips (`#Scrum`, `#Meeting`, `#Reminder`, `#Tomorrow`) under the search bar that slide out on focus and populate inputs on click.
- [x] 7. **Conversations Drawer Sidebar**: Implemented a sliding drawer expanding from `0px` to `260px` in width triggered by clicking the hamburger menu to display past conversation logs.
- [x] 8. **Contrast Visibility on Task Cards**: Modified the CSS variables in the card selectors (`task-card-title`, `task-card-details`) to use variable bindings, resolving visibility issues in Light mode.
- [x] 9. **Google Calendar View Redirect (`view_calendar`)**: Added a new `"view_calendar"` action, mapping prompts like *"take me to the calendar"* to a text/voice response and launching Google Calendar in a new tab.
- [x] 10. **Calendar Conflict Filter Bugfix**: Configured the Google Calendar conflict check loop in `calendar.py` to skip cancelled/deleted events (`status == 'cancelled'`), preventing false conflict errors.
- [x] 11. **User Session-Bound OAuth Mapping**: Implemented thread-safe/async-safe context variable mappings (`active_user_token` and `active_user_id`) to load Google credentials dynamically for the active user session.
- [x] 12. **User-Specific Email URL Parameters**: Customized the redirection URL to open the specific logged-in user's calendar (`r?authuser={email}`) and appended `&authuser={email}` to event links.
- [x] 13. **Grouped / Atomic Undo Operations**: Refactored the `handle_undo` action in `engine.py` to pop and revert sibling sub-tasks sharing the same `parent_task_id` atomically, deleting both calendar events and reminders in a single command.

# Phase 8 - Proactive Calendar Overrun Checker
- [x] Implement `proactive_suggestions` dictionary and `start_proactive_calendar_checker` background polling loop in `scheduler.py`
- [x] Start background task loop in FastAPI lifespan in `main.py`
- [x] Add `GET /api/proactive-suggestions` endpoint in `main.py`
- [x] Implement AJAX polling and toast UI alerts with a Dismiss button in `dashboard.html`

# Phase 9 - Unified Action Ledger, undo_last_action() and Testing Suite
- [x] Create the new `Action` model mapping the requested columns in `models.py`
- [x] Implement `log_action` and `undo_last_action` inside `actions_logger.py`
- [x] Connect event/reminder creations to `log_action` in `engine.py`
- [x] Connect the undo route to `undo_last_action` in `engine.py`
- [x] Implement the unified unit test suite `test_core_features.py`

# Phase 10 - Proactive Buffer Suggestion Rule (Rule 2)
- [x] Implement Rule 2 (Back-to-back meeting checking with zero buffer) in `scheduler.py`
- [x] Add the `test_proactive_back_to_back` unit test to `test_core_features.py`

# Phase 11 - UI Polish & Interactive Control Upgrades
- [x] Create specific action undo function `undo_specific_action` in `actions_logger.py`
- [x] Add GET `/api/actions` and POST `/api/actions/{action_id}/undo` routes in `main.py`
- [x] Implement `.thinking-wave` indicator container and show/hide animation triggers in `dashboard.html`
- [x] Implement inline interactive option buttons parser for conflict chats in `dashboard.html`
- [x] Implement Visual Action Trail Ledger with inline Undo buttons inside drawer in `dashboard.html`
- [x] Add unit assertions for specific action undo in `test_core_features.py`

