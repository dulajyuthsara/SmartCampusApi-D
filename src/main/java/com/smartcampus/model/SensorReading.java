package com.smartcampus.model;

import java.util.UUID;

// each time a sensor records a value, we save it as one of these
// it stores the value along with a unique id and when it was recorded
public class SensorReading {

    private String id;
    private long timestamp;
    private double value;

    public SensorReading() {
    }

    public SensorReading(double value) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
