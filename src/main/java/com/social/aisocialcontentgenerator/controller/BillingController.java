package com.social.aisocialcontentgenerator.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import com.social.aisocialcontentgenerator.service.BillingService;

@RestController
@RequestMapping("/api/v1/billing")
@Slf4j
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> checkout(Authentication authentication)
            throws StripeException {
        log.info("Checkout requested by {}", authentication.getName());
        String email = authentication.getName();
        String checkoutUrl = billingService.createCheckoutSession(email);

        return ResponseEntity.ok(Map.of("url", checkoutUrl));
    }


    @PostMapping(
            value = "/webhook",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        billingService.handleEvent(payload, signature);
        return ResponseEntity.ok("ok");
    }
}

