# TaskFlow AI (Android)

> **"One sentence in. Every step handled. Nothing forgotten."**

TaskFlow AI is a native Android application engineered as an autonomous multi-step task execution agent. Users can speak or type high-level natural language instructions (such as *"Schedule a meeting with Rahul tomorrow at 3 PM and remind me 30 minutes before"*). The app analyzes the request, formulates a validated multi-step plan, checks for Google Calendar overlaps and schedule risks, presents execution details, carries out automated actions, logs an explainable audit trail, and offers instant one-tap undo.

---

## Key Features

1. **Natural Language Understanding & Voice Input**:
   - Integrated with Android `SpeechRecognizer` with real-time waveform visualization.
   - Text editing of transcribed speech before execution to prevent unintended actions.
   - Fast, deterministic local rule-based NLP extraction alongside Gemini and OpenAI adapters.

2. **AI Safety & Execution Pipeline**:
   - Intent recognition (`SCHEDULE_EVENT`, `RESCHEDULE_EVENT`, `CANCEL_EVENT`, `QUERY_SCHEDULE`, `CREATE_REMINDER`).
   - Entity extraction: participants, dates, times, durations, and reminder offsets.
   - Mandatory risk analysis and explicit confirmation dialogs for destructive actions.

3. **Google Calendar Integration & Conflict Detection**:
   - Full event CRUD operations with agenda and day views.
   - Automated conflict checking before event creation, alerting users to overlapping meetings.
   - Intelligent free-slot discovery.

4. **Reliable Reminders & Android Notifications**:
   - AndroidX WorkManager background scheduling for notifications even when the app is killed.
   - Notification channels: Reminders (High Priority), Task Updates, and Errors.
   - Configurable offsets: 10 min, 15 min, 30 min, 1 hour, 1 day, or custom.

5. **Explainable Action Trail (Audit Log)**:
   - Chronological, timestamped records for every step taken by the AI agent.
   - Zero leakage of credentials, tokens, or private secrets in log records.

6. **Instant Action Undo**:
   - Stack-based reversible action model.
   - One-tap reversal from the execution timeline, history screen, or dashboard banner (e.g. automatically deletes created calendar events or cancels scheduled reminders).

7. **Proactive AI Suggestions**:
   - Generates non-intrusive productivity recommendations (e.g. pre-meeting reminders, focus blocks).
   - Requires explicit user interaction before taking any action.

---

## Architecture & Technology Stack

Built strictly following **Clean Architecture** and **MVVM** principles:

```
com.taskflowai
├── app                 # Application container & dependency injection
├── navigation          # Jetpack Navigation Compose (16 screens & bottom navigation)
├── presentation        # Compose UI, ViewModels, Material 3, Themes, & Components
├── domain              # Pure business logic: Models, Repositories, & Use Cases
├── data                # Room ORM, Mappers, Remote APIs, & Repository implementations
├── ai                  # AIService abstraction (Local Engine, Gemini, OpenAI)
├── calendar            # Calendar sync & Conflict detection algorithms
├── voice               # Android SpeechRecognizer manager & StateFlow audio feedback
├── notification        # Notification channels & broadcast receivers
├── worker              # WorkManager CoroutineWorker for reminder alerts
└── security            # Keystore encrypted token storage & Biometric authentication
```

### Technology Specifications
- **Language**: Kotlin 1.9.24
- **UI Framework**: Jetpack Compose with Material 3 (BOM 2024.06.00)
- **Local Persistence**: Room Database 2.6.1 & DataStore Preferences 1.1.1
- **Networking**: Retrofit 2.11.0 + OkHttp 4.12.0
- **Background Work**: AndroidX WorkManager 2.9.0
- **Authentication**: AndroidX Credential Manager & Google Play Services Auth
- **SDK Target**: `minSdk = 26` (Android 8.0 Oreo), `compileSdk = 35`, `targetSdk = 35`
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`)

---

## Getting Started & Android Studio Setup

### Prerequisites
1. **Android Studio**: Android Studio Koala / Ladybug or newer.
2. **Android SDK**: API 35 SDK Platform and Build-Tools (installed via SDK Manager).
3. **JDK**: JDK 17 or JDK 21 (Android Studio bundled JBR is supported).

### Opening the Project in Android Studio
1. Launch **Android Studio**.
2. Select **Open** and choose the `C:\TaskFlow` folder.
3. Wait for the initial Gradle sync to complete.

### Local Configuration (`local.properties`)
Create or edit `local.properties` in the project root:
```properties
sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk

# TaskFlow AI Backend and AI Providers
BACKEND_BASE_URL=https://api.taskflow.ai/v1/
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
AI_PROVIDER=LOCAL # Options: LOCAL, GEMINI, OPENAI
GEMINI_API_KEY=your-gemini-api-key
OPENAI_API_KEY=your-openai-api-key
```
*(By default, `AI_PROVIDER=LOCAL` is active. This runs the high-accuracy offline NLP engine with zero paid external API dependencies!)*

---

## Building and Running

### Build Debug APK via Command Line
On Windows:
```cmd
gradlew.bat assembleDebug
```
The compiled APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Run Unit Tests
```cmd
gradlew.bat testDebugUnitTest
```

### Run on Device or Emulator
Click the **Run ('app')** button in the Android Studio toolbar, or execute:
```cmd
gradlew.bat installDebug
```

---

## Application Navigation Flow (16 Screens)

1. **Splash Screen**: Initial loading, permission checking, and automatic session routing.
2. **Onboarding**: 3-step guide explaining voice commands, AI planning, and explainable undo.
3. **Login**: Google Sign-In with Credential Manager + Guest preview mode.
4. **Dashboard**: Live greeting, main voice/text command card, today's meetings, and AI suggestions.
5. **Command Screen**: Voice audio waveform visualizer, text editor, and prompt templates.
6. **Task Plan Screen**: AI understanding breakdown, step-by-step card list, and conflict alert.
7. **Execution Screen**: Real-time progress timeline (WAITING -> RUNNING -> SUCCESS) + instant UNDO.
8. **Task Details**: Complete audit information, execution timestamps, and linked calendar events.
9. **Calendar**: Day and Agenda views with conflict indicators and detail modal sheets.
10. **History**: Filterable list of previous automated tasks.
11. **Action Trail Details**: Drill-down chronological log of every executed action.
12. **Reminders**: Active notification alerts and countdown timers.
13. **Suggestions**: Proactive recommendations with "Use" and "Dismiss" buttons.
14. **Settings**: Calendar sync status, theme toggle (Light/Dark/System), and biometric options.
15. **Profile**: Account status and sign-out controls.
16. **About**: Architecture details and mission statement.

---

## Security Best Practices
- **No Hardcoded Secrets**: Secrets and tokens are kept strictly in `local.properties` and injected safely via `BuildConfig`.
- **Encrypted Local Storage**: Sensitive session tokens are isolated in secure storage.
- **Audit Trails without Credentials**: Action trails explicitly strip authorization headers, OAuth refresh tokens, and passwords from logs.
- **Explicit Confirmation**: High-risk or destructive actions always prompt the user with a confirmation dialog before execution.
