package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// this class handles everything related to sensors
// all the endpoints here start with /api/v1/sensors
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.INSTANCE;

    // get all sensors, you can also filter by type using ?type=Temperature etc
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> list = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.isBlank()) {
            list = list.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }
        return Response.ok(list).build();
    }

    // create a new sensor
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Sensor id is required."))
                    .build();
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A sensor with id '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // make sure the room actually exists before adding the sensor to it
        String roomId = sensor.getRoomId();
        if (roomId == null || !store.getRooms().containsKey(roomId)) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: the referenced roomId '" + roomId
                    + "' does not exist in the system. Create the room first."
            );
        }

        store.getSensors().put(sensor.getId(), sensor);
        // also add this sensor's id to the room so we know which sensors are in which room
        store.getRooms().get(roomId).addSensorId(sensor.getId());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // get one specific sensor by its id
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // delete a sensor
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                    .build();
        }
        // also remove the sensor id from the room it was in
        if (sensor.getRoomId() != null && store.getRooms().containsKey(sensor.getRoomId())) {
            store.getRooms().get(sensor.getRoomId()).removeSensorId(sensorId);
        }
        store.getSensors().remove(sensorId);
        return Response.ok(Map.of("message", "Sensor '" + sensorId + "' successfully deleted.")).build();
    }

    @PATCH
    @Path("/{sensorId}/status")
    public Response updateSensorStatus(
            @PathParam("sensorId") String sensorId,
            Map<String, String> body) {

        // first check if the sensor even exists
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor '" + sensorId + "' not found."))
                    .build();
        }

        // get the status from the request body and make sure its not empty
        String newStatus = body == null ? null : body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Request body must contain a 'status' field."))
                    .build();
        }

        String upperStatus = newStatus.toUpperCase();
        if (!upperStatus.equals("ACTIVE")
                && !upperStatus.equals("MAINTENANCE")
                && !upperStatus.equals("OFFLINE")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                            "error", "Invalid status value '" + newStatus + "'.",
                            "allowed", "ACTIVE, MAINTENANCE, OFFLINE"
                    ))
                    .build();
        }

        // everything looks good so update the status
        sensor.setStatus(upperStatus);
        return Response.ok(sensor).build();
    }

    // this is a sub-resource locator, it hands off the /readings part to another class
    // i did this to keep the code cleaner instead of putting everything in one file
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}