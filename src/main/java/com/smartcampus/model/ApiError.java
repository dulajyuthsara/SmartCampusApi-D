package com.smartcampus.model;

// this is the error format we send back when something goes wrong
// instead of showing ugly stack traces we return a clean json with status, error and message
public class ApiError {

    private int status;
    private String error;
    private String message;
    private long timestamp;

    public ApiError() {}

    public ApiError(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public int getStatus()             { return status; }
    public void setStatus(int status)  { this.status = status; }

    public String getError()               { return error; }
    public void setError(String error)     { this.error = error; }

    public String getMessage()                 { return message; }
    public void setMessage(String message)     { this.message = message; }

    public long getTimestamp()                 { return timestamp; }
    public void setTimestamp(long timestamp)   { this.timestamp = timestamp; }
}
