package com.social.aisocialcontentgenerator.service.impl;

import com.social.aisocialcontentgenerator.dto.enums.Plan;
import com.social.aisocialcontentgenerator.repository.UserRepository;
import com.social.aisocialcontentgenerator.service.BillingService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final UserRepository userRepository;

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

        Event event = Webhook.constructEvent(
                payload,
                signature,
                webhookSecret
        );

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event
                    .getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow();

            String email = session.getCustomerEmail();
            upgradeUserToPro(email);
        }
    }
}
