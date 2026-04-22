package com.smartcampus.exception;

// this gets thrown when someone tries to add a sensor to a room that doesnt exist
// we return 422 instead of 404 because the url itself is fine,
// its just that the roomId inside the json body points to something that isnt there
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
