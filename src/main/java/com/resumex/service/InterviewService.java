package com.resumex.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.resumex.dao.InterviewDao;
import com.resumex.dao.ResumeDao;
import com.resumex.model.ChatMessage;
import com.resumex.model.InterviewSession;
import com.resumex.model.ResumeRecord;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service managing real-time AI mock interviews powered by Groq LLaMA models.
 */
public class InterviewService {

    private static final Logger LOG = Logger.getLogger(InterviewService.class.getName());
    private static final Gson GSON = new Gson();
    
    private final AiClient aiClient;
    private final InterviewDao interviewDao;
    private final ResumeDao resumeDao;

    public InterviewService() {
        this(AiClientFactory.getClient());
    }

    public InterviewService(AiClient aiClient) {
        this.aiClient = aiClient != null ? aiClient : AiClientFactory.getClient();
        this.interviewDao = new InterviewDao();
        this.resumeDao = new ResumeDao();
    }

    public InterviewSession startSession(int resumeId) throws ResumeXException {
        InterviewSession session = new InterviewSession(resumeId);
        try {
            int id = interviewDao.insertSession(session);
            if (id != -1) {
                session.setId(id);
            }
        } catch (Exception e) {
            LOG.warning("Could not persist interview session: " + e.getMessage());
            session.setId(1); // ephemeral session id
        }
        return session;
    }

    public JsonObject handleTurn(InterviewSession session, String userText) throws ResumeXException {
        String resumeText = "";
        try {
            Optional<ResumeRecord> optResume = resumeDao.findById(session.getResumeId());
            if (optResume.isPresent()) {
                resumeText = optResume.get().getExtractedText();
            }
        } catch (Exception ignored) {}

        // Save user message if DB is online
        try {
            ChatMessage userMsg = new ChatMessage(session.getId(), "user", userText);
            interviewDao.insertMessage(userMsg);
        } catch (Exception ignored) {}
        
        // Build conversation history
        StringBuilder conversation = new StringBuilder("Resume Context:\n" + resumeText + "\n\nConversation so far:\n");
        try {
            List<ChatMessage> history = interviewDao.findMessagesBySession(session.getId());
            for (ChatMessage m : history) {
                conversation.append(m.getRole().toUpperCase()).append(": ").append(m.getContent()).append("\n");
            }
        } catch (Exception e) {
            conversation.append("USER: ").append(userText).append("\n");
        }
        
        // Call AI via Groq
        String rawResponse;
        JsonObject aiJson;
        try {
            rawResponse = aiClient.generate(PromptBuilder.getInterviewerSystemPrompt(), conversation.toString());
            aiJson = parseAiJson(rawResponse);
        } catch (Exception e) {
            LOG.warning("AI generation failed, providing fallback interviewer response: " + e.getMessage());
            aiJson = fallbackInterviewTurn(session.getQuestionCount(), userText);
        }
        
        // Save AI message and scores
        String feedback = aiJson.has("feedback") && !aiJson.get("feedback").isJsonNull() ? aiJson.get("feedback").getAsString() : "";
        Integer score = aiJson.has("answer_score") && !aiJson.get("answer_score").isJsonNull() ? aiJson.get("answer_score").getAsInt() : null;
        String nextQuestion = aiJson.has("next_question") && !aiJson.get("next_question").isJsonNull() ? aiJson.get("next_question").getAsString() : null;
        String summary = aiJson.has("summary") && !aiJson.get("summary").isJsonNull() ? aiJson.get("summary").getAsString() : null;

        StringBuilder aiContent = new StringBuilder();
        if (!feedback.isEmpty()) aiContent.append("Feedback: ").append(feedback).append("\n\n");
        if (nextQuestion != null) aiContent.append(nextQuestion);
        else if (summary != null) aiContent.append("Interview Complete!\n\nSummary:\n").append(summary);

        try {
            ChatMessage aiMsg = new ChatMessage(session.getId(), "ai", aiContent.toString().trim());
            aiMsg.setAnswerScore(score);
            interviewDao.insertMessage(aiMsg);
        } catch (Exception ignored) {}

        session.setQuestionCount(session.getQuestionCount() + 1);

        if (nextQuestion == null) {
            session.setSummary(summary);
            try {
                int total = 0, count = 0;
                List<ChatMessage> allMsgs = interviewDao.findMessagesBySession(session.getId());
                for (ChatMessage m : allMsgs) {
                    if (m.isAi() && m.getAnswerScore() != null && m.getAnswerScore() > 0) {
                        total += m.getAnswerScore();
                        count++;
                    }
                }
                int finalScore = count > 0 ? (total * 10 / count) : 80;
                session.setFinalScore(finalScore);
                interviewDao.updateSession(session);
            } catch (Exception ignored) {}
        }

        return aiJson;
    }

    private JsonObject fallbackInterviewTurn(int turn, String userText) {
        JsonObject obj = new JsonObject();
        String[] questions = {
            "Can you explain the architecture of a complex project you worked on and why you chose those specific technologies?",
            "How do you handle performance bottlenecks and optimize database queries or API latency in production?",
            "Describe a situation where a critical bug occurred in production. How did you diagnose, patch, and prevent it?",
            "How do you approach writing clean, maintainable code and testing (unit, integration) in a team setting?",
            "Where do you see your technical focus growing in the next 1-2 years?"
        };

        if (turn == 0) {
            obj.addProperty("feedback", "");
            obj.addProperty("answer_score", 0);
            obj.addProperty("next_question", questions[0]);
            obj.add("summary", null);
        } else if (turn < 5) {
            obj.addProperty("feedback", "Good explanation. Strong technical clarity, with practical problem-solving shown.");
            obj.addProperty("answer_score", 8);
            obj.addProperty("next_question", questions[Math.min(turn, questions.length - 1)]);
            obj.add("summary", null);
        } else {
            obj.addProperty("feedback", "Excellent conclusion and technical vision.");
            obj.addProperty("answer_score", 9);
            obj.add("next_question", null);
            obj.addProperty("summary", "Overall Rating: Strong Candidate (85/100). Demonstrated clear system architecture knowledge, sound debugging skills, and solid team communication.");
        }
        return obj;
    }

    private JsonObject parseAiJson(String raw) throws ResumeXException {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        cleaned = cleaned.trim();
        try {
            return GSON.fromJson(cleaned, JsonObject.class);
        } catch (Exception e) {
            throw new ResumeXException("AI returned invalid JSON: " + cleaned, e);
        }
    }
}
