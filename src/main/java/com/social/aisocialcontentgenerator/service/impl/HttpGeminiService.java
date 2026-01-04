package com.social.aisocialcontentgenerator.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.aisocialcontentgenerator.service.LLMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Service
public class HttpGeminiService implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(HttpGeminiService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;

    @Value("${llm.apiUrl}")
    private String apiUrl;

    @Value("${llm.apiKey}")
    private String apiKey;

    public HttpGeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String callModel(String prompt, Long userId) throws Exception {
        // Build request body (generative content)
        JsonNode requestBody = MAPPER.createObjectNode()
                .set("contents", MAPPER.createArrayNode()
                        .add(MAPPER.createObjectNode()
                                .set("parts", MAPPER.createArrayNode()
                                        .add(MAPPER.createObjectNode().put("text", prompt))
                                )
                        )
                );

        // Ensure full URL (should be full already per application.yml)
        String fullUrl = apiUrl;
        if (!fullUrl.startsWith("http://") && !fullUrl.startsWith("https://")) {
            fullUrl = "https://generativelanguage.googleapis.com" + (fullUrl.startsWith("/") ? "" : "/") + fullUrl;
        }
        String uriWithKey = fullUrl + (fullUrl.contains("?") ? "&" : "?") + "key=" + apiKey;

        try {
            String raw = webClient.post()
                    .uri(uriWithKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (raw == null) throw new RuntimeException("Empty response from LLM");
            log.debug("Raw LLM response: {}", raw);

            JsonNode root = MAPPER.readTree(raw);
            // typical path: candidates[0].content.parts[0].text
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!textNode.isMissingNode() && !textNode.isNull()) {
                return textNode.asText();
            }

            // fallback: return whole body
            return raw;

        } catch (WebClientResponseException wre) {
            log.error("LLM HTTP error: status={} body={}", wre.getRawStatusCode(), wre.getResponseBodyAsString());
            throw new RuntimeException("LLM call failed: " + wre.getRawStatusCode() + " - " + wre.getResponseBodyAsString(), wre);
        } catch (Exception ex) {
            log.error("LLM call error", ex);
            throw new RuntimeException("LLM call failed: " + ex.getMessage(), ex);
        }
    }
}
