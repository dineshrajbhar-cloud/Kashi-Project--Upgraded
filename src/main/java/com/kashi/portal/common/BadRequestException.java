package com.kashi.portal.common;

/** Thrown when a request is well-formed JSON but asks for something invalid, such as an unknown sort field. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
