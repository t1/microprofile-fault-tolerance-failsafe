package com.github.t1.faulttolerance.failsafe;

import com.github.t1.log.Logged;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.net.URI;
import java.util.UUID;

import static com.github.t1.log.LogLevel.*;

@Logged(level = DEBUG)
@Path("/")
public class FooBoundary {
    @Inject FooGateway gateway;

    static @QueryParam("request-id") UUID requestId;

    @GET public String get(
            @QueryParam("uri") URI uri,
            @QueryParam("retryOnce") boolean retryOnce
    ) {
        gateway.callNumber = 0;
        return retryOnce ? gateway.runWithOneRetry(uri) : gateway.run(uri);
    }

    @GET
    @Path("fallback-method")
    public String getWithFallbackMethod(@QueryParam("uri") URI uri) {
        return gateway.runWithFallbackMethod(uri);
    }

    @GET
    @Path("nested-fallback-handler")
    public String getWithNestedFallbackHandler(@QueryParam("uri") URI uri) {
        return gateway.runWithNestedFallbackHandler(uri);
    }

    @GET
    @Path("inner-fallback-handler")
    public String getWithFallbackHandler(@QueryParam("uri") URI uri) {
        return gateway.runWithInnerFallbackHandler(uri);
    }
}
