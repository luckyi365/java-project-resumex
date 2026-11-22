package com.resumex.service;

/**
 * Interface defining communication with the underlying AI model.
 * Swap implementations to use OpenAI, Anthropic, local LLaMA, etc.
 */
public interface AiClient {

    /**
     * Sends a prompt and returns the raw string response.
     * Implementations should handle network timeouts, retries, and auth.
     *
     * @param systemPrompt the system instructions (hidden from user)
     * @param userMessage  the user/context message
     * @return the model's raw string reply
     * @throws ResumeXException on network or API failures
     */
    String generate(String systemPrompt, String userMessage) throws ResumeXException;
}
