package com.ctn.offerwall.notification.security;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        Set<String> roles
) {
}
