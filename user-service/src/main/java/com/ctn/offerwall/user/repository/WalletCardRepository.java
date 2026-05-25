package com.ctn.offerwall.user.repository;

import com.ctn.offerwall.user.domain.AppUser;
import com.ctn.offerwall.user.domain.WalletCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletCardRepository extends JpaRepository<WalletCard, UUID> {

    List<WalletCard> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<WalletCard> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select distinct walletCard.user
            from WalletCard walletCard
            where walletCard.cardId in :cardIds
            """)
    List<AppUser> findDistinctUsersByCardIdIn(@Param("cardIds") Collection<String> cardIds);
}
