package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

// this class deals with sensor readings (like temperature values, co2 levels etc)
// its a sub-resource so SensorResource creates it, thats why theres no @Path on the class
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.INSTANCE;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // get all readings for a specific sensor
    @GET
    public Response getReadings() {
        if (!store.getSensors().containsKey(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                    .build();
        }
        List<SensorReading> history = store.getReadingsForSensor(sensorId);
        return Response.ok(history).build();
    }

    // add a new reading to a sensor
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                    .build();
        }

        // if the sensor is in maintenance mode we dont allow new readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is currently under MAINTENANCE and " +
                "cannot accept new readings. Please bring it online first."
            );
        }

        // create a new reading object, it auto-generates an id and timestamp
        SensorReading newReading = new SensorReading(reading.getValue());
        store.getReadingsForSensor(sensorId).add(newReading);

        // also update the sensor's current value so it always shows the latest reading
        sensor.setCurrentValue(newReading.getValue());

        return Response.status(Response.Status.CREATED).entity(newReading).build();
    }
}
