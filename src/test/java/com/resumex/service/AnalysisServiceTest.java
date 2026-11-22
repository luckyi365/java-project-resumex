package com.resumex.service;

import com.resumex.model.AnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisServiceTest {

    private AnalysisService service;

    @BeforeEach
    void setUp() {
        // We supply a dummy AiClient that intentionally throws an exception,
        // which will force the AnalysisService to use the fallbackScorer.
        AiClient failingAiClient = (systemPrompt, userMessage) -> {
            throw new ResumeXException("Simulated AI Failure");
        };
        service = new AnalysisService(failingAiClient);
    }

    @Test
    void testFallbackScorerLogic() throws Exception {
        // Because the AI client fails, this should trigger the fallback scorer internally.
        // We will reflectively invoke the private fallbackScorer method for direct testing.
        
        String dummyResume = "Experienced Java Developer. I built and deployed scalable applications using Spring Boot and Docker. " +
                             "I optimized database queries in SQL and improved performance by 30%. " +
                             "Education: B.S. in Computer Science. " +
                             "Skills: Java, SQL, Python, Git. " +
                             "Contact: dev@example.com";
                             
        try {
            java.lang.reflect.Method method = AnalysisService.class.getDeclaredMethod("fallbackScorer", String.class);
            method.setAccessible(true);
            
            AnalysisResult result = (AnalysisResult) method.invoke(service, dummyResume);
            
            assertNotNull(result);
            assertTrue(result.isFallback(), "Should be flagged as a fallback result");
            
            // Check if skills were found correctly
            assertTrue(result.getSkillsFound().contains("JAVA"), "Should find Java");
            assertTrue(result.getSkillsFound().contains("SQL"), "Should find SQL");
            
            // Check if missing skills were flagged
            assertTrue(result.getMissingSkills().contains("Kubernetes"), "Should flag Kubernetes as missing");
            assertTrue(result.getMissingSkills().contains("CI/CD"), "Should flag CI/CD as missing");
            
            // Check verbs
            assertTrue(result.getStrongVerbs().contains("Built"), "Should find 'built'");
            assertTrue(result.getStrongVerbs().contains("Optimized"), "Should find 'optimized'");
            
            // Score should be populated
            assertTrue(result.getScore() > 0, "Score should be greater than 0");
            assertEquals(4, result.getBreakdown().size(), "Breakdown should have 4 categories");
            
        } catch (Exception e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            fail("Reflection test failed: " + cause, cause);
        }
    }
}
