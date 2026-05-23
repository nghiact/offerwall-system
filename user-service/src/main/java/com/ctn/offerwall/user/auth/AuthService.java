package com.ctn.offerwall.user.auth;

import com.ctn.offerwall.common.event.BusinessEvent;
import com.ctn.offerwall.common.event.EntityType;
import com.ctn.offerwall.common.event.EventMetadata;
import com.ctn.offerwall.common.event.EventOutcome;
import com.ctn.offerwall.common.event.EventType;
import com.ctn.offerwall.user.auth.dto.LoginRequest;
import com.ctn.offerwall.user.auth.dto.SignUpRequest;
import com.ctn.offerwall.user.config.AuthProperties;
import com.ctn.offerwall.user.config.UserDefaultsProperties;
import com.ctn.offerwall.user.domain.AppUser;
import com.ctn.offerwall.user.domain.NotificationPreferences;
import com.ctn.offerwall.user.domain.UserRole;
import com.ctn.offerwall.user.domain.UserSession;
import com.ctn.offerwall.user.exception.AuthenticationRequiredException;
import com.ctn.offerwall.user.exception.DuplicateEmailException;
import com.ctn.offerwall.user.exception.InvalidCredentialsException;
import com.ctn.offerwall.user.exception.UserInputException;
import com.ctn.offerwall.user.repository.AppUserRepository;
import com.ctn.offerwall.user.repository.UserSessionRepository;
import com.ctn.offerwall.user.tracking.BusinessEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordPolicy passwordPolicy;
    private final TokenService tokenService;
    private final AuthProperties authProperties;
    private final UserDefaultsProperties userDefaultsProperties;
    private final BusinessEventPublisher eventPublisher;
    private final Clock clock;

    public AuthService(AppUserRepository userRepository,
                       UserSessionRepository sessionRepository,
                       PasswordPolicy passwordPolicy,
                       TokenService tokenService,
                       AuthProperties authProperties,
                       UserDefaultsProperties userDefaultsProperties,
                       BusinessEventPublisher eventPublisher,
                       Clock clock) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordPolicy = passwordPolicy;
        this.tokenService = tokenService;
        this.authProperties = authProperties;
        this.userDefaultsProperties = userDefaultsProperties;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public AuthResult signUp(SignUpRequest request, String userAgent) {
        String email = normalizeEmail(request.email());
        validatePasswords(request.password(), request.confirmPassword());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException("Email address already exists.");
        }

        NotificationPreferences preferences = new NotificationPreferences(
                userDefaultsProperties.isNotificationEmailEnabled(),
                userDefaultsProperties.isNotificationInAppEnabled()
        );
        AppUser user = new AppUser(email, request.password(), preferences);
        user.addRole(UserRole.USER);
        AppUser savedUser = userRepository.save(user);
        AuthResult result = createSession(savedUser, userAgent);

        publishUserEvent(EventType.USER_SIGNED_UP, EventOutcome.SUCCESS, savedUser.getId(), null);
        return result;
    }

    @Transactional
    public AuthResult login(LoginRequest request, String userAgent) {
        String email = normalizeEmail(request.email());
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElse(null);

        if (user == null || !user.passwordMatches(request.password())) {
            publishUserEvent(EventType.USER_LOGIN_FAILED, EventOutcome.FAILURE, null,
                    EventMetadata.of("email", email));
            throw new InvalidCredentialsException();
        }

        AuthResult result = createSession(user, userAgent);
        publishUserEvent(EventType.USER_LOGIN_SUCCEEDED, EventOutcome.SUCCESS, user.getId(), null);
        return result;
    }

    @Transactional
    public AuthResult refresh(String refreshToken, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationRequiredException("Refresh token is required.");
        }

        Instant now = Instant.now(clock);
        UserSession session = sessionRepository.findByRefreshTokenHashWithUser(tokenService.hashToken(refreshToken))
                .filter(existing -> existing.canRefresh(now))
                .orElseThrow(() -> new AuthenticationRequiredException("Refresh token is invalid or expired."));

        session.revoke(now);
        return createSession(session.getUser(), userAgent);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        Instant now = Instant.now(clock);
        sessionRepository.findByRefreshTokenHash(tokenService.hashToken(refreshToken))
                .ifPresent(session -> session.revoke(now));
    }

    @Transactional(readOnly = true)
    public AppUser requireBearerUser(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new AuthenticationRequiredException("Bearer access token is required.");
        }

        String accessToken = authorizationHeader.substring("Bearer ".length()).trim();
        if (accessToken.isBlank()) {
            throw new AuthenticationRequiredException("Bearer access token is required.");
        }

        Instant now = Instant.now(clock);
        JwtAccessTokenClaims claims = tokenService.validateAccessToken(accessToken, now);
        return userRepository.findByIdWithRoles(claims.userId())
                .orElseThrow(() -> new AuthenticationRequiredException("Access token user no longer exists."));
    }

    private AuthResult createSession(AppUser user, String userAgent) {
        Instant now = Instant.now(clock);
        TokenPair tokens = tokenService.issueTokens(
                user.getId(),
                user.getEmail(),
                user.getRoles(),
                now,
                authProperties.getAccessTokenTtl(),
                authProperties.getRefreshTokenTtl()
        );
        UserSession session = new UserSession(
                user,
                tokens.accessTokenHash(),
                tokens.accessTokenExpiresAt(),
                tokens.refreshTokenHash(),
                tokens.refreshTokenExpiresAt(),
                userAgent
        );
        sessionRepository.save(session);
        return new AuthResult(user, tokens);
    }

    private void validatePasswords(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new UserInputException("Passwords do not match.");
        }

        PasswordPolicy.Result result = passwordPolicy.validate(password);
        if (!result.valid()) {
            throw new UserInputException(result.message());
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void publishUserEvent(EventType eventType, EventOutcome outcome, UUID userId, EventMetadata metadata) {
        eventPublisher.publish(new BusinessEvent(
                null,
                eventType,
                outcome,
                EntityType.USER,
                userId == null ? null : userId.toString(),
                userId == null ? null : userId.toString(),
                null,
                metadata
        ));
    }
}
