package com.resumex.model;

import java.time.LocalDateTime;

/**
 * A single message in a mock-interview conversation.
 * Role is either {@code "ai"} (interviewer) or {@code "user"} (candidate).
 */
public class ChatMessage {

    private int id;
    private int sessionId;
    private String role;          // "ai" or "user"
    private String content;
    private Integer answerScore;  // AI's score for the user's answer (null for AI messages or unscored)
    private LocalDateTime createdAt;

    /** Default constructor for DAO hydration. */
    public ChatMessage() {}

    /**
     * Convenience constructor for a new message.
     *
     * @param sessionId parent session ID
     * @param role      "ai" or "user"
     * @param content   message text
     */
    public ChatMessage(int sessionId, String role, String content) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getAnswerScore() { return answerScore; }
    public void setAnswerScore(Integer answerScore) { this.answerScore = answerScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return true if this message was sent by the AI interviewer. */
    public boolean isAi() { return "ai".equals(role); }
}
