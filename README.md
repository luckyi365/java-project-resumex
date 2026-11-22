# ResumeX

**Smart Resume Analyzer & AI Mock Interviewer**

A desktop application built in Java (Swing + FlatLaf) for advanced Java students and job seekers. ResumeX parses uploaded resumes, evaluates them against industry standards using Groq AI (LLaMA 3.3), and provides a fully interactive AI mock interviewer tailored to the applicant's exact experience.

## Features
- **Modern UI:** FlatLaf Dark theme dashboard with custom cards, badges, and smooth scrollbars.
- **Resume Parsing:** Upload PDF, DOCX, or TXT files.
- **ATS Grader:** Instant ATS score, breakdown, skill gaps, and actionable suggestions via Groq LPU inference.
- **Mock Interview:** Practice with a Groq AI recruiter that asks technical questions based on your resume, evaluates your answers, and gives you a final score.
- **Offline Fallback:** If the AI provider or API goes down, the app falls back to a local keyword-heuristic grader automatically.
- **Data Persistence:** All resumes, analyses, and interview chats are saved locally to a MySQL database with HikariCP connection pooling.

## Prerequisites
1. **Java 25 or higher**
2. **Maven 3.6+**
3. **MySQL Server (8.0+)** running locally (or remotely)
4. A free **Groq API Key** (Get one from [Groq Console](https://console.groq.com/keys))

## Setup & Running

### 1. Database Setup
Ensure MySQL is running. The app will automatically create the `resumex` database and all required tables upon its first launch, provided you give it valid credentials. 

### 2. Configuration
Open `config.properties` and add your database credentials and Groq API key:
```properties
groq.api.key=gsk_your_groq_api_key_here
groq.model=llama-3.3-70b-versatile
db.user=root
db.password=your_mysql_password
```
*(Alternatively, you can set these as environment variables: `GROQ_API_KEY`, `RESUMEX_DB_USER`, and `RESUMEX_DB_PASSWORD`)*

### 3. Build & Run
Open terminal in the project directory:
```powershell
.\apache-maven-3.9.6\bin\mvn.cmd compile exec:java "-Dexec.mainClass=com.resumex.Main"
```
Or run `Main.java` directly inside VS Code or IntelliJ IDEA.
