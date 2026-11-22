package com.resumex.model;

import java.time.LocalDateTime;

/**
 * Represents a mock-interview session linked to a specific resume.
 * A session consists of multiple {@link ChatMessage} exchanges and
 * optionally ends with a final score and summary.
 */
public class InterviewSession {

    private int id;
    private int resumeId;
    private LocalDateTime startedAt;
    private Integer finalScore;   // null until interview ends
    private String summary;       // null until interview ends
    private int questionCount;    // tracks how many questions have been asked

    /** Default constructor for DAO hydration. */
    public InterviewSession() {}

    /**
     * Creates a new session for a given resume.
     *
     * @param resumeId the database ID of the resume being discussed
     */
    public InterviewSession(int resumeId) {
        this.resumeId = resumeId;
        this.startedAt = LocalDateTime.now();
        this.questionCount = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getResumeId() { return resumeId; }
    public void setResumeId(int resumeId) { this.resumeId = resumeId; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public Integer getFinalScore() { return finalScore; }
    public void setFinalScore(Integer finalScore) { this.finalScore = finalScore; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    /** @return true if the interview has concluded with a final score. */
    public boolean isCompleted() { return finalScore != null; }
}
