package com.adem.attijari_compass.exception;

public class InvalidProfileValueException extends IllegalArgumentException {

    public static final String MESSAGE = "Invalid profile value";

    public InvalidProfileValueException() {
        super(MESSAGE);
    }
}
