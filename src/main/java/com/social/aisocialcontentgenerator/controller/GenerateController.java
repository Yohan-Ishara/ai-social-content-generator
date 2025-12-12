package com.social.aisocialcontentgenerator.controller;


import com.social.aisocialcontentgenerator.dto.GenerateRequest;
import com.social.aisocialcontentgenerator.dto.GenerateResponse;
import com.social.aisocialcontentgenerator.service.GenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class GenerateController {

    private final GenerationService generationService;

    public GenerateController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<GenerateResponse> generate(@Valid @RequestBody GenerateRequest req,
                                                     Authentication authentication) {
        Long userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            userId = (Long) authentication.getPrincipal();
        }
        // If JWT filter didn't set a Long principal (maybe future), try email->userId lookup is needed.
        GenerateResponse resp = generationService.generateForUser(userId, req);
        return ResponseEntity.ok(resp);
    }
}
