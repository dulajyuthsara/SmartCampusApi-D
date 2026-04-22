package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

// this is like the homepage of the api
// when someone visits /api/v1 it shows them whats available and the links to other endpoints
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("api", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("description", "RESTful API for managing campus rooms and IoT sensors.");
        response.put("contact", "w2120670@westminster.ac.uk");

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",     "/api/v1");
        links.put("rooms",    "/api/v1/rooms");
        links.put("sensors",  "/api/v1/sensors");
        response.put("_links", links);

        return Response.ok(response).build();
    }
}
