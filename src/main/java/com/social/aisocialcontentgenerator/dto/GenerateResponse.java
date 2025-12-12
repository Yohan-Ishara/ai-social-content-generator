package com.social.aisocialcontentgenerator.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateResponse {
    private List<String> captions;
    private List<String> hashtags;
    private List<PostIdea> ideas;

}
