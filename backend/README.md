# TaskFlow AI 🚀

An intelligent agentic workflow manager powered by **Gemini** and **FastAPI**. TaskFlow AI interprets natural language user requests, executes tool calls directly against Google Workspace APIs (such as Google Calendar), and maintains an action stack to support reversible "undo" actions.

---

## ✨ Features

* **Natural Language Parsing**: Translates human prompts into structured ISO 8601 timestamps and calendar actions using Gemini.
* **Google Calendar Tool Integration**: Directly creates and manages calendar events via OAuth 2.0.
* **Reversible Action Stack**: Uses a LIFO (Last-In-First-Out) stack mechanism to seamlessly undo recent actions.
* **FastAPI Endpoint**: Lightweight REST API serving synchronous chat payloads to front-end clients.

---

## 🛠️ Tech Stack

| Component | Technology |
| :--- | :--- |
| **LLM Engine** | Gemini API (`google-genai`) |
| **Backend Framework** | FastAPI (Uvicorn ASGI) |
| **Integration Tools** | Google Calendar API |
| **Language** | Python 3.13 |

---

## 🚀 Quickstart Guide

### Prerequisites
* Python 3.11+
* Google Cloud Console Project with Google Calendar API enabled
* Gemini API Key

### Installation

1. **Clone the repository**
   ```bash
   git clone [https://github.com/SanikaChoughule/taskflow-ai.git](https://github.com/SanikaChoughule/taskflow-ai.git)
   cd taskflow-ai/backend

   Set up Virtual Environment

Bash
# On Windows (Git Bash)
python -m venv venv
source venv/Scripts/activate
Install Dependencies

Bash
pip install -r requirements.txt
Environment Configuration
Create a .env file in the backend/ directory:

Code snippet
GEMINI_API_KEY=your_gemini_api_key_here
Place your Google OAuth credentials.json file inside the backend/ directory as well.

Run the Development Server

Bash
uvicorn app.main:app --reload
Access the interactive API docs at http://127.0.0.1:8000/docs.


---

### Saving Your README to GitHub

Once you've created `README.md`, commit and push it:

```bash
git add README.md
git commit -m "docs: add comprehensive project README"
git push origin main
