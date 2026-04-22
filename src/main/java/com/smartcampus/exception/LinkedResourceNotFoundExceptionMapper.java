package com.smartcampus.exception;

import com.smartcampus.model.ApiError;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

// this catches LinkedResourceNotFoundException and turns it into a 422 response
// 422 means the request was okay but the data inside it didnt make sense
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        ApiError error = new ApiError(
            422,
            "Unprocessable Entity",
            ex.getMessage()
        );
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
