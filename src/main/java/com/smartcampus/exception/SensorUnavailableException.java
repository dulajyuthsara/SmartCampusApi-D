package com.smartcampus.exception;

// this gets thrown when someone tries to add a reading to a sensor thats in maintenance mode
// we dont want readings from a sensor thats not working properly
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}
