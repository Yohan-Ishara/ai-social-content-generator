package com.social.aisocialcontentgenerator.service;


import com.social.aisocialcontentgenerator.dto.GenerateRequest;
import com.social.aisocialcontentgenerator.dto.GenerateResponse;
import com.social.aisocialcontentgenerator.dto.enums.Plan;
import com.social.aisocialcontentgenerator.entity.GenerationHistory;
import com.social.aisocialcontentgenerator.entity.User;
import com.social.aisocialcontentgenerator.repository.GenerationHistoryRepository;
import com.social.aisocialcontentgenerator.repository.UserRepository;
import com.social.aisocialcontentgenerator.util.AppConstants;
import com.social.aisocialcontentgenerator.util.PromptFactory;
import com.social.aisocialcontentgenerator.util.PromptParser;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class GenerationService {

    private final LLMService llmService;
    private final GenerationHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public GenerationService(LLMService llmService,
                             GenerationHistoryRepository historyRepository, UserRepository userRepository) {
        this.llmService = llmService;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    @Cacheable(value = "generations", key = "#req.platform + ':' + #req.industry + ':' + #req.tone + ':' + (#req.keywords==null? '': #req.keywords)")
    public GenerateResponse generateForUser(String email, GenerateRequest req) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        enforceUsageLimit(user);

        GenerateResponse response = generateContent(req,user.getId());

        user.setDailyUsage(user.getDailyUsage() + 1);
        userRepository.save(user);

        return response;
    }

    private GenerateResponse generateContent(GenerateRequest req, Long userId) {
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

    private void enforceUsageLimit(User user) {
        LocalDate today = LocalDate.now();

        if (user.getLastUsageDate() == null || !user.getLastUsageDate().equals(today)) {
            user.setDailyUsage(0);
            user.setLastUsageDate(today);
        }

        if (user.getPlan() == Plan.FREE && user.getDailyUsage() >= AppConstants.FREE_DAILY_LIMIT) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Daily limit reached. Upgrade to PRO for unlimited access."
            );
        }

    }
}
