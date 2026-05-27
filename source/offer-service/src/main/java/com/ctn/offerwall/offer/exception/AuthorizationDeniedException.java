package com.ctn.offerwall.offer.exception;

public class AuthorizationDeniedException extends RuntimeException {

    public AuthorizationDeniedException(String message) {
        super(message);
    }
}
