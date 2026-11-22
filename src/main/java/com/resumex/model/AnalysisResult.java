package com.resumex.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * The result of an AI (or fallback) resume analysis.
 * <p>
 * Maps directly to the JSON schema returned by the grader prompt and
 * to the {@code analyses} database table.
 */
public class AnalysisResult {

    private int id;
    private int resumeId;
    private int score;
    private Map<String, Integer> breakdown = new LinkedHashMap<>();
    private List<String> strongVerbs  = new ArrayList<>();
    private List<String> skillsFound  = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();
    private List<String> suggestions  = new ArrayList<>();
    private String summary = "";
    private String rawResponse = "";
    private boolean fallback;
    private LocalDateTime createdAt;

    /** Default constructor for DAO hydration and builder-style population. */
    public AnalysisResult() {}

    // ---- Getters & Setters ----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getResumeId() { return resumeId; }
    public void setResumeId(int resumeId) { this.resumeId = resumeId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public Map<String, Integer> getBreakdown() { return breakdown; }
    public void setBreakdown(Map<String, Integer> breakdown) { this.breakdown = breakdown; }

    public List<String> getStrongVerbs() { return strongVerbs; }
    public void setStrongVerbs(List<String> strongVerbs) { this.strongVerbs = strongVerbs; }

    public List<String> getSkillsFound() { return skillsFound; }
    public void setSkillsFound(List<String> skillsFound) { this.skillsFound = skillsFound; }

    public List<String> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }

    public boolean isFallback() { return fallback; }
    public void setFallback(boolean fallback) { this.fallback = fallback; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ---- Helpers ----

    /**
     * Returns a human-readable verdict string based on the overall score.
     *
     * @return verdict text for display
     */
    public String verdict() {
        if (score >= 80) return "Excellent \u2013 interview ready";
        if (score >= 60) return "Good \u2013 a few improvements needed";
        return "Needs work";
    }
}
