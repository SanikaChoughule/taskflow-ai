# TaskFlow AI - Setup & Execution Guide

This guide explains how to start, build, and test the **TaskFlow AI** backend application on Windows.

---

## 🚀 Quick Start Instructions

1. **Open your terminal** and navigate to the backend directory:
   ```powershell
   cd backend
   ```

2. **Start the FastAPI Dev Server**:
   ```powershell
   venv\Scripts\python.exe -m uvicorn app.main:app --port 8000 --reload
   ```

3. **Access the Dashboard**:
   Open your browser and navigate to:
   * **[http://localhost:8000/](http://localhost:8000/)** (redirects to the user dashboard or login)

---

## 🛠️ C++ DSA Engine Compilation & Fallback

The backend includes a performance-optimized C++ LIFO undo stack ([`undo_stack.cpp`](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/undo_stack.cpp)) bound to Python with `pybind11` via [`setup.py`](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/setup.py).

If you attempt to compile the extension using:
```powershell
venv\Scripts\python.exe setup.py build_ext --inplace
```

You might encounter this error if Microsoft Visual Studio C++ build tools are not installed:
```
error: Microsoft Visual C++ 14.0 or greater is required. Get it with "Microsoft C++ Build Tools"
```

### Options to Resolve:

1. **Option A: Ignore and use the Python Fallback (Recommended)**
   You do **not** need to compile the C++ code to run the application. The system has a built-in pure Python fallback class in [`undo.py`](file:///c:/Users/LENOVO/OneDrive/Desktop/taskflow-ai/backend/app/agent/undo.py). If the compiled `dsa_engine` binary is not found, it seamlessly falls back to Python execution with no impact on functionality.

2. **Option B: Compile with MinGW (GCC)**
   If you have GCC/G++ installed via MinGW instead of MSVC, run:
   ```powershell
   venv\Scripts\python.exe setup.py build_ext --inplace --compiler=mingw32
   ```

3. **Option C: Install MSVC**
   Download and install the [Microsoft C++ Build Tools](https://visualstudio.microsoft.com/visual-cpp-build-tools/) and select the "Desktop development with C++" workload, then rerun the standard compile command.

---

## 🧪 Verification Tests

To verify that the application, database scheduler, conflict checkers, and undo stack are working properly, run the test suite:
```powershell
venv\Scripts\python.exe test_core_features.py
```
