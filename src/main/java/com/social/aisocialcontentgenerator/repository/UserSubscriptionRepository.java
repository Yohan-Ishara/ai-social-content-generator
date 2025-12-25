package com.social.aisocialcontentgenerator.repository;

import com.social.aisocialcontentgenerator.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    Optional<UserSubscription> findByEmail(String email);

    Optional<UserSubscription> findByStripeSubscriptionId(String subscriptionId);
}
