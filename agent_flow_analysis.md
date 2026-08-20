# Agent Decision Flow Analysis and Test Matrix

This document provides a systematic review of the **Agent Decision Flow** diagram, verifying how it behaves across a comprehensive matrix of test cases, detailing the data structures, execution paths, and the exact files in the codebase that implement each stage.

---

## 🏗️ Architectural Component Mapping

The following sections define how our Python backend maps to the components of the decision flow:

| Flow Stage | Code Component / File | Description |
| :--- | :--- | :--- |
| **User Command** | [`dashboard.html`](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/templates/dashboard.html) | Captures voice via Web Speech API or text input from the chat UI bar. |
| **Speech-to-Text** | [`voice_wake.py`](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/voice_wake.py) | Python-based voice wake and processing endpoint using speech recognition. |
| **Intent Recognition** | [`parser.py`](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/parser.py) | Calls Groq LLM (`llama-3.1-8b-instant`) to parse raw prompts into structured JSON tasks. |
| **Build Task Graph** | [`dag_planner.py`](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/dag_planner.py) | Compiles parsed tasks into a Directed Acyclic Graph (`TaskDAG`) using topological sorting. |
| **Prioritize & Schedule**| [`scheduler.py`](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/scheduler.py) | Places reminder sub-tasks into a priority queue sorted by execution epoch times. |
| **Conflict Detection** | [`calendar.py`](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/tools/calendar.py) | Normalizes Google Calendar events into UTC and checks for time overlaps. |
| **Action Execution** | [`engine.py`](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/engine.py) | Traverses the DAG, executes node tools, and monitors status updates. |
| **Undo Stack** | [`undo.py`](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/undo.py) | Implements a LIFO stack to record reverse commands (e.g. `delete_event`). |
| **Proactive Suggestions**| [`engine.py`](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/engine.py) | Generates dynamic text chips based on execution status and context. |

---

## 🧪 Comprehensive Test Case Matrix

Here is how different input commands traverse the decision flow diagram:

### 1. Simple Calendar Booking (Happy Path)
* **Command**: *"Schedule a sync with Sanika tomorrow at 3 PM UTC for 1 hour."*
* **Flow Path**:
  $$\text{User Command} \rightarrow \text{LLM Intent Parsing} \rightarrow \text{DAG Builder (1 Node)} \rightarrow \text{Conflict Check (No Overlaps)} \rightarrow \text{Execute Tool} \rightarrow \text{Push to Undo Stack} \rightarrow \text{Confirm & End}$$
* **Validation**:
  - LLM extracts: Summary="sync with Sanika", Start="3:00 PM UTC", End="4:00 PM UTC".
  - Calendar overlap check is `False`.
  - Event books directly; Google Calendar Card renders.
  - Action `{"action": "delete_event", "event_id": "..."}` is pushed to the Undo Stack.

### 2. Multi-Task Request with Dependencies
* **Command**: *"Schedule Scrum meeting in 5 days from 4 PM to 5 PM UTC and remind me 30 mins before."*
* **Flow Path**:
  $$\text{User Command} \rightarrow \text{LLM Parsing} \rightarrow \text{DAG Builder (2 Nodes: Event } \rightarrow \text{ Reminder)} \rightarrow \text{Conflict Check} \rightarrow \text{Priority Queue Scheduling} \rightarrow \text{Execute Actions} \rightarrow \text{Push Stack} \rightarrow \text{End}$$
* **Validation**:
  - DAG contains: `task_1` (Create Event) and `task_2` (Set Reminder dependent on `task_1`).
  - `task_2` waits for `task_1` to succeed (to inherit the final event title and timing).
  - Reminder is placed in the `ReminderPriorityQueue` sorted by epoch time.
  - User receives two cards: Google Calendar card and Reminder card.

### 3. Calendar Conflict / Risk Detection
* **Command**: *"Schedule meeting with Rahul tomorrow at 3 PM for 1 hour."* (When already booked tomorrow 3:00 PM - 3:15 PM).
* **Flow Path**:
  $$\text{User Command} \rightarrow \text{LLM Parsing} \rightarrow \text{DAG Builder} \rightarrow \text{Conflict Check (Overlap detected)} \rightarrow \text{Branch: Yes} \rightarrow \text{Alert User & Suggest Alternate} \rightarrow \text{End}$$
* **Validation**:
  - Overlap is mathematically identified in `check_calendar_conflict`.
  - Execution of `task_1` returns `status="conflict"`.
  - Dependent reminder task is aborted (skipped).
  - Assistant overrides normal confirmation response, rendering conflict suggestion: *"You're booked until 3:15 PM. Want Rahul at 3:30 PM instead, or double-book?"*
  - No visual event cards are generated in the UI.

### 4. Overriding Conflict (Double Booking)
* **Command**: User replies *"double book"* to the previous conflict warning.
* **Flow Path**:
  $$\text{User Command} \rightarrow \text{LLM Context Merging (Previous turns resolved)} \rightarrow \text{DAG Builder (double\_book=True)} \rightarrow \text{Conflict Check (Bypassed)} \rightarrow \text{Execute Tool} \rightarrow \text{Push Stack} \rightarrow \text{Confirm}$$
* **Validation**:
  - Parser reads conversation history, retrieves details of the conflicting event, and sets parameter `double_book=True`.
  - `check_calendar_conflict` returns conflict details but `engine.py` bypasses abortion because `double_book` is active.
  - Event is created; Calendar card and Reminder card are generated.

### 5. Missing Critical Parameter Validation (Deferred Flow)
* **Command**: *"Schedule a Scrum meeting today."* (Missing start time and duration).
* **Flow Path**:
  $$\text{User Command} \rightarrow \text{LLM Parsing (Negative constraint check)} \rightarrow \text{Task: Conversation} \rightarrow \text{Bypass Execution} \rightarrow \text{Ask Clarification} \rightarrow \text{End}$$
* **Validation**:
  - LLM checks system prompts (missing duration and start time negative rules).
  - Bypasses tool generation and returns a single `conversation` action.
  - Assistant replies: *"Could you please share the start time and duration you'd like to schedule the meeting for?"*
  - No booking attempt is made.

### 6. Undo Stack Execution (Reversal)
* **Command**: Clicking the **Undo** button in the chat interface.
* **Flow Path**:
  $$\text{User Click} \rightarrow \text{API /api/undo} \rightarrow \text{Pop Action from Undo Stack} \rightarrow \text{Execute Reversal (delete\_calendar\_event)} \rightarrow \text{Confirm & End}$$
* **Validation**:
  - Pops the last recorded transaction from the global stack in `undo.py`.
  - Calls `delete_calendar_event` on the specific `event_id`.
  - Card disappears from the user interface and the event is removed from Google Calendar.

---

## 🏆 Key Flow Diagram Assertions & Design Quality

1. **Topological Ordering**: The Directed Acyclic Graph structure prevents race conditions (e.g. trying to schedule a reminder for an event that failed to book).
2. **Prioritization Layer**: rem_pq guarantees that reminders trigger chronologically, even when multiple events are scheduled concurrently.
3. **Loopback Closure**: Overriding conflicts loops back into the execution stage dynamically without requiring the user to re-enter all details, providing a seamless UX.

---

## 🚀 Phase 7 Test Scenarios & Decision Flow Enhancements
Below is the list of decision flow enhancements completed during Phase 7:
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
