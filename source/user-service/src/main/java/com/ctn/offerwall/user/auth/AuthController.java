package com.ctn.offerwall.user.auth;

import com.ctn.offerwall.user.auth.dto.AuthResponse;
import com.ctn.offerwall.user.auth.dto.LoginRequest;
import com.ctn.offerwall.user.auth.dto.SignUpRequest;
import com.ctn.offerwall.user.config.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthProperties authProperties;

    public AuthController(AuthService authService, AuthProperties authProperties) {
        this.authService = authService;
        this.authProperties = authProperties;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request,
                                               HttpServletRequest servletRequest) {
        AuthResult result = authService.signUp(request, servletRequest.getHeader(HttpHeaders.USER_AGENT));
        return withRefreshCookie(result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest servletRequest) {
        AuthResult result = authService.login(request, servletRequest.getHeader(HttpHeaders.USER_AGENT));
        return withRefreshCookie(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest servletRequest) {
        String refreshToken = findRefreshCookie(servletRequest);
        AuthResult result = authService.refresh(refreshToken, servletRequest.getHeader(HttpHeaders.USER_AGENT));
        return withRefreshCookie(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
        authService.logout(findRefreshCookie(servletRequest));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthResult result) {
        ResponseCookie cookie = refreshCookie(result.tokens().refreshToken(), authProperties.getRefreshTokenTtl());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(AuthResponse.from(result));
    }

    private String findRefreshCookie(HttpServletRequest servletRequest) {
        if (servletRequest.getCookies() == null) {
            return null;
        }
        return Arrays.stream(servletRequest.getCookies())
                .filter(cookie -> authProperties.getRefreshCookie().getName().equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private ResponseCookie refreshCookie(String token, Duration maxAge) {
        return ResponseCookie.from(authProperties.getRefreshCookie().getName(), token)
                .httpOnly(true)
                .secure(authProperties.getRefreshCookie().isSecure())
                .sameSite(authProperties.getRefreshCookie().getSameSite())
                .path(authProperties.getRefreshCookie().getPath())
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return refreshCookie("", Duration.ZERO);
    }
}
