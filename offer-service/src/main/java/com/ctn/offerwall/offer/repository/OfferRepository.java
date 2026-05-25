package com.ctn.offerwall.offer.repository;

import com.ctn.offerwall.offer.domain.Offer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    @EntityGraph(attributePaths = {"category", "targetCardProductIds"})
    @Query("select distinct o from Offer o")
    List<Offer> findAllWithDetails();

    @EntityGraph(attributePaths = {"category", "targetCardProductIds"})
    Optional<Offer> findById(UUID id);

    boolean existsByCategoryId(UUID categoryId);
}
