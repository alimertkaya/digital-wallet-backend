package com.alimertkaya.digitalwallet.exception.kafka;

public class EventSerializationException extends RuntimeException {

    public EventSerializationException(String message) {
        super(message);
    }

    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
