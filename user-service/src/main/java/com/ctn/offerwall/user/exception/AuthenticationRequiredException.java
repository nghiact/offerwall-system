package com.ctn.offerwall.user.exception;

public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
