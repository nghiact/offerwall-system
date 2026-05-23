package com.ctn.offerwall.card.repository;

import com.ctn.offerwall.card.domain.CardProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardProductRepository extends JpaRepository<CardProduct, UUID> {

    boolean existsByProductCodeIgnoreCase(String productCode);

    Optional<CardProduct> findByProductCodeIgnoreCase(String productCode);

    @Query("""
            select distinct product
            from CardProduct product
            left join fetch product.bins
            order by product.issuer asc, product.name asc, product.productCode asc
            """)
    List<CardProduct> findAllWithBins();

    @Query("""
            select distinct product
            from CardProduct product
            left join fetch product.bins
            where product.id = :id
            """)
    Optional<CardProduct> findByIdWithBins(@Param("id") UUID id);
}
