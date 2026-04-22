package com.smartcampus.exception;

// this error happens when someone tries to delete a room that still has sensors in it
// we cant just delete a room and leave the sensors hanging with no room
public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException(String message) {
        super(message);
    }
}
