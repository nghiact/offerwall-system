package com.ctn.offerwall.user.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email address and/or password is incorrect.");
    }
}
