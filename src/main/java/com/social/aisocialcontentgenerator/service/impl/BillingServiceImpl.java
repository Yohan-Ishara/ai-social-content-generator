package com.social.aisocialcontentgenerator.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.social.aisocialcontentgenerator.dto.enums.Plan;
import com.google.gson.JsonObject;
import com.social.aisocialcontentgenerator.entity.UserSubscription;
import com.social.aisocialcontentgenerator.repository.UserRepository;
import com.social.aisocialcontentgenerator.repository.UserSubscriptionRepository;
import com.social.aisocialcontentgenerator.service.BillingService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingServiceImpl implements BillingService {

    private final Gson gson = new Gson();
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    @Value("${app.frontend.success-url}")
    private String successUrl;

    @Value("${app.frontend.cancel-url}")
    private String cancelUrl;

    @Value("${stripe.price.pro}")
    private String proPriceId;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;


    @Override
    public String createCheckoutSession(String email) throws StripeException {
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                        .setCustomerEmail(email)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setPrice(proPriceId)
                                        .setQuantity(1L)
                                        .build()
                        )
                        .build();

        Session session = Session.create(params);
        return session.getUrl();
    }


    public void handleEvent(String payload, String signature) {
        final Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Invalid Stripe signature", e);
        }

        // Stripe java may fail to deserialize when API versions differ.
        // So use raw JSON safely.
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        String rawJson = deserializer.getRawJson();
        if (rawJson == null || rawJson.isBlank()) {
            // If this happens, just log and return 200 in controller (don’t crash).
            log.warn("Stripe webhook has no rawJson. eventId=" + event.getId() + " type=" + event.getType());
            return;
        }

        switch (event.getType()) {

            //  BEST event for upgrade
            case "checkout.session.completed" -> handleCheckoutSessionCompleted(rawJson);

            //  BEST event for renewals
            case "invoice.payment_succeeded" -> handleInvoicePaymentSucceeded(rawJson);

            //  handle failed payment
            case "invoice.payment_failed" -> handleInvoicePaymentFailed(rawJson);

            default -> {
                // For now log only
                System.out.println("Ignoring Stripe event: " + event.getType());
            }
        }
    }


    private void handleCheckoutSessionCompleted(String rawJson) {
        JsonObject obj = gson.fromJson(rawJson, JsonObject.class);

        String email = getAsString(obj, "customer_email");
        String subscriptionId = getAsString(obj, "subscription");
        String customerId = getAsString(obj, "customer");

        if (email == null || subscriptionId == null || customerId == null) {
            System.out.println("checkout.session.completed missing fields: " + rawJson);
            return;
        }

        activateSubscription(email, subscriptionId, customerId);
        upgradeUserToPro(email);
    }

    private void handleInvoicePaymentSucceeded(String rawJson) {
        JsonObject obj = gson.fromJson(rawJson, JsonObject.class);

        // Invoice has these fields (your sample shows them)
        String email = getAsString(obj, "customer_email");
        String subscriptionId = getAsString(obj, "subscription"); // might be nested in newer payloads, see below
        String customerId = getAsString(obj, "customer");

        // In your sample invoice JSON, subscription id is inside:
        // parent.subscription_details.subscription
        if (subscriptionId == null) {
            subscriptionId = getNestedAsString(obj, "parent", "subscription_details", "subscription");
        }

        if (email == null || subscriptionId == null || customerId == null) {
            System.out.println("invoice.payment_succeeded missing fields: " + rawJson);
            return;
        }

        // Renewal should keep it active + extend period
        activateSubscription(email, subscriptionId, customerId);
        upgradeUserToPro(email);
    }

    private void handleInvoicePaymentFailed(String rawJson) {
        JsonObject obj = gson.fromJson(rawJson, JsonObject.class);

        String email = getAsString(obj, "customer_email");
        if (email == null) return;

        // simplest: mark inactive, keep plan maybe FREE
        subscriptionRepository.findByEmail(email).ifPresent(sub -> {
            sub.setActive(false);
            subscriptionRepository.save(sub);
        });
    }

    public void activateSubscription(String email, String subscriptionId, String customerId) {
        final Subscription subscription;
        try {
            subscription = Subscription.retrieve(subscriptionId);
        } catch (StripeException e) {
            throw new RuntimeException("Failed to retrieve subscription " + subscriptionId, e);
        }

        UserSubscription sub = subscriptionRepository
                .findByEmail(email)
                .orElseGet(UserSubscription::new);

        sub.setEmail(email);
        sub.setStripeSubscriptionId(subscriptionId);
        sub.setStripeCustomerId(customerId);
        sub.setPlan(Plan.PRO);
        sub.setActive(true);

        // ✅ correct field
        Long periodEnd = subscription.getCurrentPeriodEnd();
        if (periodEnd != null) {
            sub.setCurrentPeriodEnd(Instant.ofEpochSecond(periodEnd));
        }

        subscriptionRepository.save(sub);
    }

    public void upgradeUserToPro(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setPlan(Plan.PRO);
            userRepository.save(user);
        });
    }

    // ---------- small helpers ----------
    private String getAsString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    private String getNestedAsString(JsonObject obj, String... path) {
        JsonObject cur = obj;
        for (int i = 0; i < path.length; i++) {
            String key = path[i];
            if (cur == null || !cur.has(key) || cur.get(key).isJsonNull()) return null;
            if (i == path.length - 1) return cur.get(key).getAsString();
            cur = cur.get(key).getAsJsonObject();
        }
        return null;
    }


    @Override
    public boolean isPro(String email) {
        return subscriptionRepository.findByEmail(email)
                .map(s -> s.isActive()
                        && s.getCurrentPeriodEnd().isAfter(Instant.now()))
                .orElse(false);

    }

}

