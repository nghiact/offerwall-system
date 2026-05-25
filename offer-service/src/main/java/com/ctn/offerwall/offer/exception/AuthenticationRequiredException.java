package com.ctn.offerwall.offer.exception;

public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
