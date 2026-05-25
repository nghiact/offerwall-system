package com.ctn.offerwall.tracking.tracking.dto;

import java.time.Instant;

public record RetentionPolicyResponse(
        int retentionDays,
        Instant deleteBefore
) {
}
