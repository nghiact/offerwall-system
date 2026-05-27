package com.ctn.offerwall.user.repository;

import com.ctn.offerwall.user.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select distinct user
            from AppUser user
            left join fetch user.roles
            where user.id = :id
            """)
    Optional<AppUser> findByIdWithRoles(@Param("id") UUID id);
}
