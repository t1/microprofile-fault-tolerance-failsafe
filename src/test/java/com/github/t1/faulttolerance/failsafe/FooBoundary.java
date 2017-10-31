package com.github.t1.faulttolerance.failsafe;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.net.URI;

@Path("/")
public class FooBoundary {
    @Inject FooGateway gateway;

    @GET public String get(
            @QueryParam("uri") URI uri,
            @QueryParam("retryOnce") boolean retryOnce,
            @QueryParam("fallback") String fallback
    ) {
        gateway.fallback = fallback;
        gateway.callNumber = 0;
        return retryOnce ? gateway.runWithOneRetry(uri) : gateway.run(uri);
    }
}
