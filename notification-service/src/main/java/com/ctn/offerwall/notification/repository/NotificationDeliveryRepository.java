package com.ctn.offerwall.notification.repository;

import com.ctn.offerwall.common.notification.NotificationChannel;
import com.ctn.offerwall.notification.domain.NotificationDelivery;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    @EntityGraph(attributePaths = "campaign")
    List<NotificationDelivery> findByRecipientUserIdAndChannelOrderByCreatedAtDesc(UUID recipientUserId, NotificationChannel channel);

    @EntityGraph(attributePaths = "campaign")
    Optional<NotificationDelivery> findWithCampaignById(UUID id);
}
