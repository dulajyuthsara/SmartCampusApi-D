package com.smartcampus;

import com.smartcampus.exception.*;
import com.smartcampus.filter.ApiLoggingFilter;
import com.smartcampus.resource.*;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

// this is where the API starts basically
// the @ApplicationPath annotation sets the base url to /api/v1
// so every endpoint we make will start with that
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    // here we register all the classes that our app needs to know about
    // jersey calls this method when the server starts up
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // these are our main endpoint classes (the ones that handle requests)
        classes.add(DiscoveryResource.class);
        classes.add(SensorRoomResource.class);
        classes.add(SensorResource.class);
        // SensorReadingResource is not added here because its a sub-resource
        // SensorResource creates it automatically when needed

        // these handle errors and turn them into proper json responses
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(GlobalExceptionMapper.class);

        // this filter logs every request and response that comes through
        classes.add(ApiLoggingFilter.class);

        return classes;
    }
}
