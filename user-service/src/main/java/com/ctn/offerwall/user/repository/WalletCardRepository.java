package com.ctn.offerwall.user.repository;

import com.ctn.offerwall.user.domain.WalletCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletCardRepository extends JpaRepository<WalletCard, UUID> {
}
