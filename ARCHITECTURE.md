# Architecture Overview

ResumeX is built strictly following a **3-Layer Architecture** (Presentation, Service/Logic, Data/DAO) to ensure high maintainability, testability, and clear separation of concerns.

## 1. Presentation Layer (`com.resumex.ui`)
**Role:** Handles all user interaction, rendering, and state display. 
- **Rule:** Contains absolutely no business logic, SQL queries, or HTTP calls.
- **Concurrency:** All heavy operations (parsing, Groq AI calls, DB queries) are offloaded to `SwingWorker` threads. The Event Dispatch Thread (EDT) is never blocked, ensuring the UI remains perfectly responsive and animations (like the `ScoreGauge`) remain smooth.
- **Key Classes:** `MainFrame`, `AnalyzerPanel`, `InterviewPanel`, `HistoryPanel`, `Theme` (for centralizing design tokens).

## 2. Service & Logic Layer (`com.resumex.service`)
**Role:** The core brain of the application. Orchestrates data flow between the UI, the AI, and the Database.
- **Rule:** Operates independently of the UI.
- **Components:**
  - `ResumeParser`: Uses Apache PDFBox and POI to extract text from unstructured documents.
  - `AiClient` & `GroqClient`: Manages the HTTP communication with the Groq LPU inference API, including JSON marshalling/unmarshalling and retry/backoff logic.
  - `AnalysisService` & `InterviewService`: Orchestrates the flows. If `GroqClient` fails (e.g. offline/network error), `AnalysisService` silently catches the exception and routes the data through a localized `fallbackScorer()` so the user experience is uninterrupted.

## 3. Data Access Layer (`com.resumex.dao`)
**Role:** Handles persistence and object-relational mapping.
- **Rule:** Only DAO classes speak SQL. They map JDBC ResultSets into the POJOs defined in `com.resumex.model`.
- **Components:** 
  - `Database.java` & `DatabaseConnection.java`: Connection pooling (HikariCP) and schema bootstrapper. It detects if the `resumex` database exists, and if not, runs schema creation automatically.
  - DAO Classes (`UserDao`, `ResumeDao`, `AnalysisDao`, `InterviewDao`): Execute `PreparedStatement` queries to prevent SQL injection.

---

## Example Data Flow: Analyzing a Resume
1. **Capture:** The user drags a PDF into the `AnalyzerPanel`.
2. **Offload:** `AnalyzerPanel` creates a `SwingWorker` and calls `AnalysisService.analyzeResume(file)`.
3. **Parse:** `ResumeParser` converts the PDF into a raw String.
4. **Persist (Input):** `ResumeDao` saves the raw text to MySQL.
5. **Transmit:** `GroqClient` injects the text into the `PromptBuilder` grader prompt and sends an HTTP POST.
6. **Evaluate:** `AnalysisService` parses the returned JSON string into an `AnalysisResult` object.
7. **Persist (Output):** `AnalysisDao` saves the score, breakdown, and suggestions to MySQL.
8. **Render:** The `SwingWorker`'s `done()` method pushes the `AnalysisResult` back to the EDT, and `AnalyzerPanel` animates the UI updates.
