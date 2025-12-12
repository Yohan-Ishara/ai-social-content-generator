package com.social.aisocialcontentgenerator.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "generation_history")
@Setter
public class GenerationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // nullable for anonymous

    private String platform;
    private String industry;
    private String tone;
    @Lob
    private String keywords;

    @Lob
    private String outputJson; // raw modelText or raw JSON

    private Instant createdAt = Instant.now();

    public GenerationHistory() {}

}

