package com.ctn.offerwall.offer.repository;

import com.ctn.offerwall.offer.domain.OfferCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OfferCategoryRepository extends JpaRepository<OfferCategory, UUID> {

    Optional<OfferCategory> findByCodeIgnoreCase(String code);
}
