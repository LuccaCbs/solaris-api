package com.luccavergara.solaris.exception;

public class SubscriptionLimitException extends RuntimeException {

    public SubscriptionLimitException(String message) {
        super(message);
    }
}
