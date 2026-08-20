# TaskFlow AI 🚀

An intelligent agentic workflow manager powered by **Gemini** and **FastAPI**. TaskFlow AI interprets natural language user requests, executes tool calls directly against Google Workspace APIs (such as Google Calendar), and maintains an action stack to support reversible "undo" actions.

---

## 📂 Repository Structure

The project is structured as follows:

*   **`backend/`**: Contains the FastAPI backend application, the C++ LIFO undo stack, tools (Google Calendar integration), and the test suite.
*   **Documentation**:
    *   [`SETUP.md`](file:///SETUP.md): Step-by-step setup and execution guide (FastAPI, virtual environment, and C++ compilation options).
    *   [`project_notes.md`](file:///project_notes.md): Core concepts, backend routing, database schemas, and current accomplishments.
    *   [`agent_flow_analysis.md`](file:///agent_flow_analysis.md): Detailed trace and analysis of the agent prompt/response loop and calendar scheduling flow.
    *   [`implementation_plan.md`](file:///implementation_plan.md): Technical plan for features, including recurring events, conflicts, and undo mechanisms.
    *   [`walkthrough.md`](file:///walkthrough.md): Comprehensive developer log documenting features, files modified/created, and testing logs.
    *   [`task.md`](file:///task.md): Checklist of completed and in-progress developer tasks.

---

## 🛠️ Technology Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **LLM Engine** | Gemini API (`google-genai`) | Natural language parsing, JSON schema structuring |
| **Backend Framework** | FastAPI (Uvicorn ASGI) | REST API endpoints, static dashboards |
| **Integration Tools** | Google Calendar API | Create, delete, update, and resolve conflicts for events |
| **State & DSA Engine** | SQLite + C++ LIFO Stack (`pybind11`) | Event scheduling state, fast memory-safe undo operations |
| **Language** | Python 3.13 / C++17 | Core logic & performance-critical components |

---

## 🚀 Quick Start Guide

To get the application up and running locally:

### 1. Prerequisites
*   Python 3.11+
*   Google Cloud Console Project with Google Calendar API enabled
*   Gemini API Key

### 2. Setup and Installation
For step-by-step instructions on setting up credentials (such as Google Client Secrets) and starting the development server, please refer to the **[Setup Guide (`SETUP.md`)](file:///SETUP.md)**.

Brief commands:
```powershell
# Navigate to backend
cd backend

# Setup virtual environment
python -m venv venv
.\venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run the FastAPI server
python -m uvicorn app.main:app --port 8000 --reload
```

---

## 🧪 Verification Tests

Run the core feature validation suite to verify the scheduling flow, conflicts, and undo stack:
```powershell
cd backend
.\venv\Scripts\python.exe test_core_features.py
```
For advanced testing scenarios (such as DAG task dependency testing or conflict checkers), see the test files in the [`backend/`](file:///backend) directory.
