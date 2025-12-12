package com.social.aisocialcontentgenerator.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateRequest {
    @NotBlank
    private String platform;
    @NotBlank
    private String industry;
    private String tone = "friendly";
    private String keywords;

    public GenerateRequest() {}

}

