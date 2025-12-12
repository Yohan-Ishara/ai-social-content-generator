package com.social.aisocialcontentgenerator.util;


import com.social.aisocialcontentgenerator.dto.GenerateRequest;

public class PromptFactory {

    /**
     * Build a clear prompt that instructs Gemini to return strict JSON with fields:
     * captions (array), hashtags (array), ideas (array of {title,description,imageIdea})
     */
    public static String buildPrompt(GenerateRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful social media copywriter.\n");
        sb.append("Produce output as strict JSON only (no commentary). The JSON must have keys: captions (array of short strings), hashtags (array), ideas (array of objects with title, description, imageIdea).\n\n");
        sb.append("Platform: ").append(req.getPlatform()).append("\n");
        sb.append("Industry: ").append(req.getIndustry()).append("\n");
        sb.append("Tone: ").append(req.getTone() == null ? "friendly" : req.getTone()).append("\n");
        if (req.getKeywords() != null && !req.getKeywords().isBlank()) {
            sb.append("Keywords: ").append(req.getKeywords()).append("\n");
        }
        sb.append("\nRequirements:\n");
        sb.append("- Provide up to 10 short captions (max 150 chars each) in captions array.\n");
        sb.append("- Provide a list of up to 20 hashtags (include # prefix) in hashtags array.\n");
        sb.append("- Provide 3 post ideas with title, description (~30-60 chars), and an imageIdea short phrase.\n");
        sb.append("\nReturn only JSON and nothing else.\n");
        sb.append("\nExample JSON structure:\n");
        sb.append("{\n  \"captions\": [\"...\"],\n  \"hashtags\": [\"#tag1\",\"#tag2\"],\n  \"ideas\": [{\"title\":\"...\",\"description\":\"...\",\"imageIdea\":\"...\"}]\n}\n\n");
        sb.append("Now generate for the input above.");
        return sb.toString();
    }
}

