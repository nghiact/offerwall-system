package com.ctn.offerwall.card.repository;

import com.ctn.offerwall.card.domain.CardBin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CardBinRepository extends JpaRepository<CardBin, UUID> {

    boolean existsByBin(String bin);

    @Query("""
            select bin
            from CardBin bin
            join fetch bin.cardProduct product
            where bin.bin like concat(:prefix, '%')
               or :prefix like concat(bin.bin, '%')
            order by
                case
                    when bin.bin = :prefix then 0
                    when bin.bin like concat(:prefix, '%') then 1
                    else 2
                end,
                length(bin.bin) desc,
                product.issuer asc,
                product.name asc
            """)
    List<CardBin> findMatchesForPrefix(@Param("prefix") String prefix);
}
