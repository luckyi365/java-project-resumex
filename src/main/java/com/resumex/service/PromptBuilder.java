package com.resumex.service;

/**
 * Contains the hidden system prompts for grading and interviewing.
 */
public final class PromptBuilder {

    private PromptBuilder() {}

    /**
     * Backward-compatible overload for callers that do not supply a job description.
     */
    public static String getGraderSystemPrompt() {
        return getGraderSystemPrompt(null);
    }

    /**
     * Builds the system prompt for the Resume Grader.
     */
    public static String getGraderSystemPrompt(String jobDescription) {
        String base = """
            You are an expert ATS (Applicant Tracking System) and senior technical recruiter.
            Evaluate the provided resume text rigorously.
            """;
            
        if (jobDescription != null && !jobDescription.isBlank()) {
            base += "\nCritically evaluate the resume AGAINST the following Job Description:\n" + 
                    "--- JOB DESCRIPTION START ---\n" + jobDescription + "\n--- JOB DESCRIPTION END ---\n";
        }
        
        base += """
            
            Return ONLY valid JSON matching exactly this schema, with no markdown formatting or extra text:
            {
              "score": <int 0-100 overall score>,
              "breakdown": {
                "Content & Impact": <int 0-100>,
                "Skills Match": <int 0-100>,
                "Structure & Sections": <int 0-100>,
                "Length & Readability": <int 0-100>
              },
              "strong_verbs": [<string array of action verbs found>],
              "skills_found": [<string array of technical skills found>],
              "missing_skills": [<string array of common/expected in-demand skills missing>],
              "suggestions": [<string array of actionable improvement tips>],
              "summary": "<string brief overall verdict>"
            }
            """;
        
        return base;
    }

    /**
     * Builds the system prompt for the Mock Interviewer.
     */
    public static String getInterviewerSystemPrompt() {
        return """
            You are a senior technical interviewer conducting a mock interview with a candidate based on their resume.
            Ask ONE question at a time. The question should be technical or behavioral, directly tied to their stated experience/skills.
            
            After the user answers, evaluate their response and return ONLY valid JSON matching exactly this schema:
            {
              "feedback": "<string briefly evaluating their answer and offering a tip>",
              "answer_score": <int 0-10 scoring their answer>,
              "next_question": "<string the next question, or null if 5 questions have been asked>",
              "summary": "<string final summary of the interview if it's over, else null>"
            }
            
            If this is the very first turn (user says 'ready'), provide an empty feedback string and 0 score, and just ask the first question in 'next_question'.
            End the interview after 5 questions by setting 'next_question' to null and providing a 'summary'.
            No markdown formatting, just raw JSON.
            """;
    }
}
