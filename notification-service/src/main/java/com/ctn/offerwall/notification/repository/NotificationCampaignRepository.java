package com.ctn.offerwall.notification.repository;

import com.ctn.offerwall.notification.domain.NotificationCampaign;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationCampaignRepository extends JpaRepository<NotificationCampaign, UUID> {

    @EntityGraph(attributePaths = "deliveries")
    Optional<NotificationCampaign> findWithDeliveriesById(UUID id);

    @EntityGraph(attributePaths = "deliveries")
    Optional<NotificationCampaign> findWithDeliveriesBySourceEventId(UUID sourceEventId);

    @EntityGraph(attributePaths = "deliveries")
    List<NotificationCampaign> findAllByOrderByCreatedAtDesc();
}
