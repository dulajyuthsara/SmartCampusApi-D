package com.smartcampus;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// basically this is our fake database since we dont have a real one
// i used an enum singleton because it was the easiest way to make sure theres only one instance
// ConcurrentHashMap is used here so things dont break if multiple requests come in at once
public enum DataStore {
    INSTANCE;

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();
    // this stores all the readings for each sensor, the key is the sensor id
    private final ConcurrentHashMap<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, Room> getRooms() {
        return rooms;
    }

    public ConcurrentHashMap<String, Sensor> getSensors() {
        return sensors;
    }

    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>());
    }
}
