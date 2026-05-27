package com.ctn.offerwall.notification.exception;

public class AuthorizationDeniedException extends RuntimeException {

    public AuthorizationDeniedException(String message) {
        super(message);
    }
}
