package com.social.aisocialcontentgenerator.entity;

import com.social.aisocialcontentgenerator.dto.enums.Plan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_subscriptions")
@Getter
@Setter
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String stripeSubscriptionId;

    @Column(nullable = false)
    private String stripeCustomerId;

    @Enumerated @Column(nullable = false)
    private Plan plan; // FREE / PRO

    @Column(nullable = false)
    private boolean active;

    private Instant currentPeriodEnd;

    private Instant createdAt = Instant.now();
}