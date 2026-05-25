package com.ctn.offerwall.user.repository;

import com.ctn.offerwall.user.domain.WalletCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WalletCardRepository extends JpaRepository<WalletCard, UUID> {

    @Query("""
            select distinct walletCard.user
            from WalletCard walletCard
            where walletCard.cardId in :cardIds
            """)
    List<com.ctn.offerwall.user.domain.AppUser> findDistinctUsersByCardIdIn(@Param("cardIds") Collection<String> cardIds);
}
