package com.social.aisocialcontentgenerator.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;

public interface BillingService {
    String createCheckoutSession(String email) throws StripeException;
    void upgradeUserToPro(String email);

    void handleEvent(String payload, String signature);
    void activateSubscription(String email, String subscriptionId, String customerId);
    boolean isPro(String email);
}
