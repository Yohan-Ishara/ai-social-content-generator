package com.social.aisocialcontentgenerator.service.impl;

import com.social.aisocialcontentgenerator.dto.enums.Plan;
import com.social.aisocialcontentgenerator.entity.UserSubscription;
import com.social.aisocialcontentgenerator.repository.UserRepository;
import com.social.aisocialcontentgenerator.repository.UserSubscriptionRepository;
import com.social.aisocialcontentgenerator.service.BillingService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

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

    public void upgradeUserToPro(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setPlan(Plan.PRO);
            userRepository.save(user);
        });
    }

    public void handleEvent(String payload, String signature) throws SignatureVerificationException {

        try {
            Event event = Webhook.constructEvent(
                    payload, signature, webhookSecret);

            switch (event.getType()) {

                case "checkout.session.completed" -> {
                    Session session = (Session) event
                            .getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow();

                    activateSubscription(
                            session.getCustomerEmail(),
                            session.getSubscription(),
                            session.getCustomer(),
                            session.getExpiresAt()
                    );

                  upgradeUserToPro(session.getCustomerEmail());
                }

                case "invoice.payment_failed" -> {
                    // downgrade / notify user
                }
            }

        } catch (SignatureVerificationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature");
        }
    }

    @Override
    public void activateSubscription(String email, String subscriptionId, String customerId, long periodEndEpoch) {
        UserSubscription sub = subscriptionRepository.findByEmail(email)
                .orElse(new UserSubscription());

        sub.setEmail(email);
        sub.setStripeSubscriptionId(subscriptionId);
        sub.setStripeCustomerId(customerId);
        sub.setPlan(Plan.PRO);
        sub.setActive(true);
        sub.setCurrentPeriodEnd(
                Instant.ofEpochSecond(periodEndEpoch)
        );

        subscriptionRepository.save(sub);
    }

    @Override
    public boolean isPro(String email) {
        return subscriptionRepository.findByEmail(email)
                .map(s -> s.isActive()
                        && s.getCurrentPeriodEnd().isAfter(Instant.now()))
                .orElse(false);

    }
}
