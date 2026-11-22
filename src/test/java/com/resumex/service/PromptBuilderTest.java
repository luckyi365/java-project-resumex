package com.resumex.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderTest {

    @Test
    void testGraderSystemPromptContainsRequiredKeys() {
        String prompt = PromptBuilder.getGraderSystemPrompt();

        assertNotNull(prompt);
        assertTrue(prompt.contains("\"score\""));
        assertTrue(prompt.contains("\"breakdown\""));
        assertTrue(prompt.contains("\"skills_found\""));
        assertTrue(prompt.contains("\"missing_skills\""));
        assertTrue(prompt.contains("\"suggestions\""));
    }

    @Test
    void testInterviewerSystemPromptContainsRequiredKeys() {
        String prompt = PromptBuilder.getInterviewerSystemPrompt();

        assertNotNull(prompt);
        assertTrue(prompt.contains("\"feedback\""));
        assertTrue(prompt.contains("\"answer_score\""));
        assertTrue(prompt.contains("\"next_question\""));
    }
}
