package com.resumex.service;

import com.resumex.AppConfig;

/**
 * Factory providing the active {@link AiClient}.
 * Powered exclusively by Groq AI for fast LPU inference.
 */
public final class AiClientFactory {
    
    private static volatile AiClient currentClient;

    private AiClientFactory() {}

    public static synchronized AiClient getClient() {
        if (currentClient == null) {
            currentClient = new GroqClient();
        }
        return currentClient;
    }

    public static synchronized void setClient(AiClient client) {
        currentClient = client;
    }

    public static String getActiveProviderName() {
        AiClient c = getClient();
        if (c instanceof GroqClient) {
            return "Groq (" + ((GroqClient) c).getModelName() + ")";
        }
        return "Groq AI";
    }

    public static boolean isConfigured() {
        AiClient c = getClient();
        if (c instanceof GroqClient) {
            return ((GroqClient) c).hasValidKey();
        }
        return false;
    }
}
