package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

// this filter runs on every request and response
// it just prints out what method was called and what status code was returned
// way easier than putting log statements in every single method lol
@Provider
public class ApiLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(ApiLoggingFilter.class.getName());

    // this runs before the request reaches our endpoint
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOG.info(">>> INCOMING REQUEST  | Method: " + requestContext.getMethod()
                + " | URI: " + requestContext.getUriInfo().getRequestUri());
    }

    // this runs after the response is ready to be sent back
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOG.info("<<< OUTGOING RESPONSE | Status: " + responseContext.getStatus()
                + " | URI: " + requestContext.getUriInfo().getRequestUri());
    }
}
