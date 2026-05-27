package com.ctn.offerwall.tracking.exception;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String message) {
}
