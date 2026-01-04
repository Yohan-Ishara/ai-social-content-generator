package com.social.aisocialcontentgenerator.service;

public interface LLMService {
    /**
     * Call the LLM with the given prompt and return the raw model text output.
     * The returned string is the model's text (for Gemini: candidates[0].content.parts[0].text).
     *
     * @param prompt prompt text
     * @param userId optional userId (can be null) for routing/quota
     * @return raw model text (not an HTTP body wrapper)
     * @throws Exception on network / parse error
     */
    String callModel(String prompt, Long userId) throws Exception;
}
