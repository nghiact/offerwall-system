package com.ctn.offerwall.card.exception;

public class AuthorizationDeniedException extends RuntimeException {

    public AuthorizationDeniedException(String message) {
        super(message);
    }
}
