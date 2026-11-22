-- ================================================================
-- ResumeX Database Schema (MySQL 8+)
-- ================================================================
-- This script is also auto-executed by Database.java on first run.
-- To run manually:  mysql -u root -p < schema.sql
-- ================================================================

CREATE DATABASE IF NOT EXISTS resumex
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE resumex;

-- Users
CREATE TABLE IF NOT EXISTS users (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255)  NOT NULL,
    email      VARCHAR(255)  NOT NULL UNIQUE,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Uploaded resumes with extracted text
CREATE TABLE IF NOT EXISTS resumes (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT           NOT NULL,
    file_name      VARCHAR(500)  NOT NULL,
    extracted_text LONGTEXT      NOT NULL,
    uploaded_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- AI / fallback analysis results
CREATE TABLE IF NOT EXISTS analyses (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    resume_id        INT           NOT NULL,
    score            INT           NOT NULL,
    breakdown_json   TEXT          NOT NULL,
    missing_skills   TEXT,
    suggestions_json TEXT,
    raw_response     LONGTEXT,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Mock-interview sessions
CREATE TABLE IF NOT EXISTS interview_sessions (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    resume_id   INT           NOT NULL,
    started_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    final_score INT           DEFAULT NULL,
    summary     TEXT          DEFAULT NULL,
    FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Individual messages within a session
CREATE TABLE IF NOT EXISTS interview_messages (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    session_id   INT          NOT NULL,
    role         ENUM('ai','user') NOT NULL,
    content      TEXT         NOT NULL,
    answer_score INT          DEFAULT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES interview_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB;
