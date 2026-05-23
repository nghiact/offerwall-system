package com.ctn.offerwall.user.repository;

import com.ctn.offerwall.user.domain.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByAccessTokenHash(String accessTokenHash);

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    @Query("""
            select distinct session
            from UserSession session
            join fetch session.user user
            left join fetch user.roles
            where session.accessTokenHash = :accessTokenHash
            """)
    Optional<UserSession> findByAccessTokenHashWithUser(@Param("accessTokenHash") String accessTokenHash);

    @Query("""
            select distinct session
            from UserSession session
            join fetch session.user user
            left join fetch user.roles
            where session.refreshTokenHash = :refreshTokenHash
            """)
    Optional<UserSession> findByRefreshTokenHashWithUser(@Param("refreshTokenHash") String refreshTokenHash);
}
