package com.resumex.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroqClientTest {

    @Test
    void testGroqClientCreationAndModelName() {
        GroqClient client = new GroqClient("gsk_test12345", "llama-3.3-70b-versatile");
        assertEquals("llama-3.3-70b-versatile", client.getModelName());
        assertTrue(client.hasValidKey());
    }

    @Test
    void testGroqClientInvalidKeyDetection() {
        GroqClient client = new GroqClient("gsk_your_groq_api_key_here", "llama-3.3-70b-versatile");
        assertFalse(client.hasValidKey());

        assertThrows(ResumeXException.class, () -> {
            client.generate("System", "User message");
        });
    }

    @Test
    void testAiClientFactoryDefaults() {
        AiClient client = AiClientFactory.getClient();
        assertNotNull(client);
        String activeName = AiClientFactory.getActiveProviderName();
        assertNotNull(activeName);
        assertTrue(activeName.contains("Groq"));
    }
}

