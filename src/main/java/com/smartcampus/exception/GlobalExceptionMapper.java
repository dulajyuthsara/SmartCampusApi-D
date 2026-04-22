package com.smartcampus.exception;

import com.smartcampus.model.ApiError;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

// this is like a safety net that catches any errors we didnt specifically handle
// it returns a generic 500 error so we dont accidentally show server details to users
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        LOG.severe("Unhandled exception: " + ex.getClass().getName() + " - " + ex.getMessage());
        ApiError error = new ApiError(
            500,
            "Internal Server Error",
            "An unexpected error occurred on the server. Please contact the API administrator."
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
