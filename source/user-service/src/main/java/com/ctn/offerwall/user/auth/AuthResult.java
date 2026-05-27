package com.ctn.offerwall.user.auth;

import com.ctn.offerwall.user.domain.AppUser;

public record AuthResult(AppUser user, TokenPair tokens) {
}
