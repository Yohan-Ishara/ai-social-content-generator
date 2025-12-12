package com.social.aisocialcontentgenerator.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.social.aisocialcontentgenerator.dto.GenerateResponse;
import com.social.aisocialcontentgenerator.dto.PostIdea;

import java.util.ArrayList;
import java.util.List;

/**
 * Robust PromptParser:
 * - Accepts the modelText that Gemini returns (the inner text extracted from candidates[0].content.parts[0].text)
 * - Tries to find a JSON substring and parse it.
 * - If no JSON found or parse fails, falls back to a simple heuristic parser.
 */
public class PromptParser {

    private static final ObjectMapper M = new ObjectMapper();

    public static GenerateResponse parse(String modelText) {
        if (modelText == null) {
            return emptyResponse();
        }

        // 1) Try to extract JSON substring (handles code fences and extra commentary)
        String jsonCandidate = extractJsonSubstring(modelText);
        if (jsonCandidate != null) {
            try {
                return parseFromJson(jsonCandidate);
            } catch (Exception e) {
                // parsing failed — fall through to fallback
                System.err.println("PromptParser: JSON parse failed: " + e.getMessage());
            }
        }

        // 2) Try to parse the full text as JSON (some providers return raw JSON)
        try {
            return parseFromJson(modelText);
        } catch (Exception ignored) {
        }

        // 3) Fallback heuristic parsing (plain text)
        return parseFromPlainText(modelText);
    }

    // ===== Helpers =====

    private static GenerateResponse parseFromJson(String json) throws JsonProcessingException {
        JsonNode root = M.readTree(json);
        List<String> captions = new ArrayList<>();
        List<String> hashtags = new ArrayList<>();
        List<PostIdea> ideas = new ArrayList<>();

        // captions: array or single string
        JsonNode capsNode = root.path("captions");
        if (capsNode.isArray()) {
            for (JsonNode n : capsNode) captions.add(n.asText());
        } else if (capsNode.isTextual()) {
            // maybe a single string with newlines
            for (String line : capsNode.asText().split("\\r?\\n")) {
                String t = line.trim();
                if (!t.isEmpty()) captions.add(t);
            }
        }

        // hashtags
        JsonNode tagsNode = root.path("hashtags");
        if (tagsNode.isArray()) {
            for (JsonNode n : tagsNode) hashtags.add(n.asText());
        } else if (tagsNode.isTextual()) {
            // extract tokens starting with # or split by commas
            String text = tagsNode.asText();
            for (String token : text.split("[,\\n]")) {
                token = token.trim();
                if (token.isEmpty()) continue;
                if (token.startsWith("#")) hashtags.add(token);
                else hashtags.add("#" + token.replaceAll("\\s+", ""));
            }
        }

        // ideas
        JsonNode ideasNode = root.path("ideas");
        if (ideasNode.isArray()) {
            for (JsonNode item : ideasNode) {
                String title = safeText(item, "title");
                String desc = safeText(item, "description");
                String imageIdea = safeText(item, "imageIdea");
                ideas.add(new PostIdea(title, desc, imageIdea));
            }
        } else if (root.has("ideas") && root.get("ideas").isTextual()) {
            // text block -> split heuristically into 3 ideas
            String[] blocks = root.get("ideas").asText().split("\\r?\\n\\r?\\n");
            for (String b : blocks) {
                String title = b.lines().findFirst().orElse("").trim();
                String desc = b.trim();
                ideas.add(new PostIdea(title, desc, ""));
            }
        }

        // If nothing parsed, attempt common aliases
        if (captions.isEmpty()) {
            JsonNode alt = root.path("posts");
            if (alt.isArray()) for (JsonNode n : alt) captions.add(n.asText());
        }

        GenerateResponse resp = new GenerateResponse();
        resp.setCaptions(captions);
        resp.setHashtags(hashtags);
        resp.setIdeas(ideas);
        return resp;
    }

    private static String safeText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : "";
    }

    private static GenerateResponse parseFromPlainText(String text) {
        List<String> captions = new ArrayList<>();
        List<String> hashtags = new ArrayList<>();
        List<PostIdea> ideas = new ArrayList<>();

        // split by lines, collect non-empty lines as captions until ~10
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            // skip lines that look like commentary
            if (t.toLowerCase().startsWith("sure") || t.toLowerCase().startsWith("here")) continue;
            // if line is a hashtag list
            if (t.startsWith("#") || t.contains(" #")) {
                // split words that start with #
                for (String token : t.split("\\s+")) {
                    token = token.trim();
                    if (token.startsWith("#")) hashtags.add(token.replaceAll("[,.;:]$", ""));
                }
                continue;
            }
            // if line starts with a bullet or dash, strip it
            if (t.startsWith("-") || t.startsWith("*")) {
                t = t.substring(1).trim();
            }
            // treat as caption
            captions.add(t);
            if (captions.size() >= 10) break;
        }

        // Try to extract hashtags from the whole text as fallback: words beginning with #
        for (String token : text.split("\\s+")) {
            if (token.startsWith("#")) {
                String clean = token.replaceAll("[,.;:]$", "");
                if (!hashtags.contains(clean)) hashtags.add(clean);
            }
        }

        // Simple heuristic for ideas: look for lines with ':' or 'Idea' keywords
        for (String line : lines) {
            String t = line.trim();
            if (t.toLowerCase().startsWith("idea") || t.contains(":")) {
                String title = t.length() > 40 ? t.substring(0, 40) + "..." : t;
                ideas.add(new PostIdea(title, t, ""));
                if (ideas.size() >= 3) break;
            }
        }

        // Fill placeholders if empty
        if (captions.isEmpty()) captions.add("Try this caption: Amazing results with #yourproduct");
        if (ideas.isEmpty()) ideas.add(new PostIdea("Quick Post Idea", "Share a photo with a short tip.", ""));

        GenerateResponse resp = new GenerateResponse();
        resp.setCaptions(captions);
        resp.setHashtags(hashtags);
        resp.setIdeas(ideas);
        return resp;
    }

    /**
     * Find the first JSON object substring in the input text by scanning for a balanced '{' ... '}' block.
     * Returns null if none found.
     */
    private static String extractJsonSubstring(String text) {
        int len = text.length();
        int i = 0;
        while (i < len) {
            // find next opening brace
            if (text.charAt(i) == '{') {
                int depth = 0;
                for (int j = i; j < len; j++) {
                    char c = text.charAt(j);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                    if (depth == 0) {
                        // substring [i..j] is a balanced JSON object
                        return text.substring(i, j + 1);
                    }
                }
                // if we found '{' but never closed, break
                break;
            }
            i++;
        }
        // try fenced code block with ```json ... ```
        int fence = text.indexOf("```json");
        if (fence >= 0) {
            int start = text.indexOf('{', fence);
            int endFence = text.indexOf("```", fence + 6);
            if (start >= 0 && endFence > start) {
                // attempt to find closing brace before endFence
                String between = text.substring(start, Math.min(endFence, text.length()));
                String candidate = extractJsonSubstring(between);
                if (candidate != null) return candidate;
            }
        }
        return null;
    }

    private static GenerateResponse emptyResponse() {
        GenerateResponse r = new GenerateResponse();
        r.setCaptions(new ArrayList<>());
        r.setHashtags(new ArrayList<>());
        r.setIdeas(new ArrayList<>());
        return r;
    }
}

