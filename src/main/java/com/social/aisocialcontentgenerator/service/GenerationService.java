package com.social.aisocialcontentgenerator.service;


import com.social.aisocialcontentgenerator.dto.GenerateRequest;
import com.social.aisocialcontentgenerator.dto.GenerateResponse;
import com.social.aisocialcontentgenerator.entity.GenerationHistory;
import com.social.aisocialcontentgenerator.repository.GenerationHistoryRepository;
import com.social.aisocialcontentgenerator.util.PromptFactory;
import com.social.aisocialcontentgenerator.util.PromptParser;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GenerationService {

    private final LLMService llmService;
    private final GenerationHistoryRepository historyRepository;

    public GenerationService(LLMService llmService,
                             GenerationHistoryRepository historyRepository) {
        this.llmService = llmService;
        this.historyRepository = historyRepository;
    }

    @Cacheable(value = "generations", key = "#req.platform + ':' + #req.industry + ':' + #req.tone + ':' + (#req.keywords==null? '': #req.keywords)")
    public GenerateResponse generateForUser(Long userId, GenerateRequest req) {
        try {
            String prompt = PromptFactory.buildPrompt(req);
            // call Gemini LLM via HttpGeminiService
            String modelText = llmService.callModel(prompt, userId);

            // modelText is the inner text from Gemini (expected JSON or text containing JSON)
            GenerateResponse parsed = PromptParser.parse(modelText);

            // Save raw modelText and metadata to DB
            GenerationHistory history = new GenerationHistory();
            history.setUserId(userId);
            history.setPlatform(req.getPlatform());
            history.setIndustry(req.getIndustry());
            history.setTone(req.getTone());
            history.setKeywords(req.getKeywords());
            history.setOutputJson(modelText);
            historyRepository.save(history);

            return parsed;
        } catch (Exception ex) {
            throw new RuntimeException("Generation failed: " + ex.getMessage(), ex);
        }
    }
}
