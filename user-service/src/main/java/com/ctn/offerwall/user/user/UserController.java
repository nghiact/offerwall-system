package com.ctn.offerwall.user.user;

import com.ctn.offerwall.user.auth.AuthService;
import com.ctn.offerwall.user.auth.dto.NotificationPreferenceRequest;
import com.ctn.offerwall.user.auth.dto.UserProfileResponse;
import com.ctn.offerwall.user.domain.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping
    public UserProfileResponse me(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return UserProfileResponse.from(authService.requireBearerUser(authorization));
    }

    @PatchMapping("/notification-preferences")
    public UserProfileResponse updateNotificationPreferences(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        AppUser user = authService.requireBearerUser(authorization);
        return UserProfileResponse.from(userService.updateNotificationPreferences(
                user.getId(),
                request.emailEnabled(),
                request.inAppEnabled()
        ));
    }
}
