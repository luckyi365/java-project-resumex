package com.resumex.model;

import java.time.LocalDateTime;

/**
 * Represents an uploaded resume file and its extracted plain text.
 * Linked to a {@link User} via {@code userId}.
 */
public class ResumeRecord {

    private int id;
    private int userId;
    private String fileName;
    private String extractedText;
    private LocalDateTime uploadedAt;

    /** Default constructor for DAO hydration. */
    public ResumeRecord() {}

    /**
     * Convenience constructor for a freshly-parsed resume.
     *
     * @param userId        owning user's database ID
     * @param fileName      original file name (e.g. "resume.pdf")
     * @param extractedText full plain-text content extracted from the file
     */
    public ResumeRecord(int userId, String fileName, String extractedText) {
        this.userId = userId;
        this.fileName = fileName;
        this.extractedText = extractedText;
        this.uploadedAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
