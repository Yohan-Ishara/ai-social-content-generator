package com.social.aisocialcontentgenerator.entity;

import com.social.aisocialcontentgenerator.dto.enums.Plan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(unique = true, nullable = false)
    private String email;


    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role = "USER";

    @Enumerated(EnumType.STRING)
    private Plan plan = Plan.FREE;

    private int dailyUsage = 0;

    private LocalDate lastUsageDate;

}