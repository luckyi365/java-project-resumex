package com.resumex.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.resumex.dao.AnalysisDao;
import com.resumex.dao.ResumeDao;
import com.resumex.model.AnalysisResult;
import com.resumex.model.ResumeRecord;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Orchestrator service that handles resume extraction, AI grading (via Groq),
 * local fallback scoring if offline, and DB persistence.
 */
public class AnalysisService {

    private static final Logger LOG = Logger.getLogger(AnalysisService.class.getName());
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                @Override
                public void write(JsonWriter out, LocalDateTime value) throws IOException {
                    out.value(value != null ? value.toString() : null);
                }

                @Override
                public LocalDateTime read(JsonReader in) throws IOException {
                    return LocalDateTime.parse(in.nextString());
                }
            }.nullSafe())
            .create();

    private final ResumeParser parser;
    private final AiClient aiClient;
    private final ResumeDao resumeDao;
    private final AnalysisDao analysisDao;

    public AnalysisService() {
        this(AiClientFactory.getClient());
    }

    public AnalysisService(AiClient aiClient) {
        this.parser = new ResumeParser();
        this.aiClient = aiClient != null ? aiClient : AiClientFactory.getClient();
        this.resumeDao = new ResumeDao();
        this.analysisDao = new AnalysisDao();
    }

    /**
     * Executes the full pipeline for a given user and file.
     *
     * @param userId the ID of the user uploading the file (-1 or 0 for guest)
     * @param file   the physical file on disk (PDF, DOCX, TXT)
     * @param jobDescription optional job description to tailor grading
     * @return the resulting AnalysisResult
     * @throws ResumeXException if parsing fails
     */
    public AnalysisResult analyzeResume(int userId, File file, String jobDescription) throws ResumeXException {
        // 1. Parse text from PDF/DOCX/TXT
        String text = parser.parse(file);

        // 2. Save resume record if DB is available
        int resumeId = -1;
        if (userId > 0) {
            try {
                ResumeRecord record = new ResumeRecord(userId, file.getName(), text);
                resumeId = resumeDao.insert(record);
            } catch (Exception e) {
                LOG.warning("Could not persist resume to database: " + e.getMessage());
            }
        }

        // 3. Attempt AI Analysis via Groq
        AnalysisResult result = null;
        try {
            String systemPrompt = PromptBuilder.getGraderSystemPrompt(jobDescription);
            String aiJson = aiClient.generate(systemPrompt, text);
            result = parseAiJson(aiJson);
            result.setRawResponse(aiJson);
            result.setFallback(false);
        } catch (Exception e) {
            LOG.warning("AI Analysis failed or not configured, using local heuristic: " + e.getMessage());
            result = fallbackScorer(text);
        }

        // 4. Save analysis if DB is available
        if (resumeId > 0) {
            try {
                result.setResumeId(resumeId);
                analysisDao.insert(result);
            } catch (Exception e) {
                LOG.warning("Could not persist analysis to database: " + e.getMessage());
            }
        }

        return result;
    }

    private AnalysisResult parseAiJson(String raw) throws ResumeXException {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        try {
            return GSON.fromJson(cleaned, AnalysisResult.class);
        } catch (Exception e) {
            throw new ResumeXException("AI returned invalid JSON: " + cleaned, e);
        }
    }

    /**
     * High-accuracy heuristic fallback when offline.
     */
    private AnalysisResult fallbackScorer(String text) {
        AnalysisResult a = new AnalysisResult();
        a.setFallback(true);
        String t = text.toLowerCase();
        
        String[] VERBS = {"architected", "deployed", "led", "optimized", "built", "developed",
            "designed", "improved", "implemented", "automated", "launched", "managed", "created", "reduced"};
        String[] SKILLS = {"java", "python", "sql", "git", "spring", "docker", "kubernetes",
            "ci/cd", "aws", "rest", "html", "javascript", "react", "linux", "jdbc", "junit"};

        for (String v : VERBS) if (t.contains(v)) a.getStrongVerbs().add(cap(v));
        for (String s : SKILLS) {
            if (t.contains(s)) a.getSkillsFound().add(s.length() <= 4 ? s.toUpperCase() : cap(s));
            else if (Arrays.asList("docker", "kubernetes", "ci/cd", "aws", "junit").contains(s))
                a.getMissingSkills().add(s.equals("ci/cd") ? "CI/CD" : s.equals("aws") ? "AWS" : s.equals("junit") ? "JUnit" : cap(s));
        }

        boolean hasEdu = t.contains("education");
        boolean hasExp = t.contains("experience");
        boolean hasProj = t.contains("project");
        boolean hasSkills = t.contains("skills");
        boolean hasContact = t.contains("@");
        boolean hasNumbers = t.matches("(?s).*\\d+\\s*%.*");

        int content = Math.min(100, 40 + a.getStrongVerbs().size() * 8 + (hasNumbers ? 15 : 0));
        int skills = Math.min(100, a.getSkillsFound().size() * 12 + 10);
        int structure = 30 + (hasEdu ? 15 : 0) + (hasExp ? 20 : 0) + (hasProj ? 15 : 0) + (hasSkills ? 10 : 0) + (hasContact ? 10 : 0);
        int length = Math.max(30, Math.min(100, text.split("\\s+").length / 4 + 40));

        a.getBreakdown().put("Content & Impact", content);
        a.getBreakdown().put("Skills Match", skills);
        a.getBreakdown().put("Structure & Sections", Math.min(100, structure));
        a.getBreakdown().put("Length & Readability", length);
        
        a.setScore((int) Math.round(content * .3 + skills * .3 + Math.min(100, structure) * .25 + length * .15));

        if (!hasNumbers) a.getSuggestions().add("Quantify achievements with metrics and outcomes (e.g. 'reduced latency by 40%').");
        if (!a.getMissingSkills().isEmpty()) a.getSuggestions().add("Target in-demand industry skills: " + String.join(", ", a.getMissingSkills()) + ".");
        if (!hasProj) a.getSuggestions().add("Add a notable Projects section with GitHub / live demo links.");
        if (!hasContact) a.getSuggestions().add("Include professional email, GitHub, and LinkedIn links.");
        if (a.getStrongVerbs().size() < 5) a.getSuggestions().add("Start bullet points with assertive verbs (e.g., Architected, Optimized, Spearheaded).");
        a.getSuggestions().add("Format with consistent single-page margins, concise bullet points, and ATS-friendly headers.");

        a.setSummary("ATS Heuristic Evaluation: Good technical base. Expanding quantifiable metrics and cloud keywords will significantly boost match scores.");
        a.setRawResponse(GSON.toJson(a));
        return a;
    }

    private static String cap(String s) { 
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1); 
    }
}
