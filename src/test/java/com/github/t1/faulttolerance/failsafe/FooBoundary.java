package com.github.t1.faulttolerance.failsafe;

import com.github.t1.log.Logged;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.net.URI;
import java.util.UUID;

import static com.github.t1.log.LogLevel.*;
import static javax.ws.rs.core.Response.Status.*;

@Logged(level = INFO)
@Path("/")
public class FooBoundary {
    @Inject FooGateway gateway;

    static @QueryParam("request-id") UUID requestId;

    @GET public String getWithoutRetry(@QueryParam("uri") URI uri) {
        gateway.callNumber = 0;
        return gateway.run(uri);
    }

    @GET @Path("/retry") public String getWithRetry(@QueryParam("uri") URI uri) {
        gateway.callNumber = 0;
        return gateway.runWithOneRetry(uri);
    }

    @GET @Path("/fallback-method") public String getWithFallbackMethod(@QueryParam("uri") URI uri) {
        return gateway.runWithFallbackMethod(uri);
    }

    @GET @Path("/nested-fallback-handler") public String getWithNestedFallbackHandler(@QueryParam("uri") URI uri) {
        return gateway.runWithNestedFallbackHandler(uri);
    }

    @GET @Path("/inner-fallback-handler") public String getWithInnerFallbackHandler(@QueryParam("uri") URI uri) {
        return gateway.runWithInnerFallbackHandler(uri);
    }

    @GET @Path("/timeout") public String getWithTimeout(@QueryParam("uri") URI uri) {
        try {
            return gateway.runWithTimeout(uri);
        } catch (TimeoutException e) {
            throw new ServerErrorException(BAD_GATEWAY, e);
        }
    }
}
