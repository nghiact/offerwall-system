package com.ctn.offerwall.offer.user;

import com.ctn.offerwall.offer.user.dto.UserWalletCandidate;

import java.util.UUID;

public interface UserWalletClient {

    UserWalletCandidate getWalletCandidate(UUID userId);
}
