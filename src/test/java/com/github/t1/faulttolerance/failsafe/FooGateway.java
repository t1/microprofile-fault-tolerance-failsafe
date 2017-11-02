package com.github.t1.faulttolerance.failsafe;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.*;

import javax.ws.rs.*;
import javax.ws.rs.client.*;
import java.net.URI;

import static com.github.t1.problem.WebException.*;
import static javax.ws.rs.core.Response.Status.*;

@Slf4j
@Gateway
public class FooGateway {
    private final Client client = ClientBuilder.newClient();

    int callNumber;
    String fallback;

    @Retry(maxRetries = 1)
    public String runWithOneRetry(URI uri) { return run(uri); }

    @Fallback(fallbackMethod = "fallback")
    public String run(URI uri) {
        log.debug("call number " + ++callNumber + " to " + uri);
        try {
            return client.target(uri).request().get(String.class);
        } catch (RedirectionException e) { // ugly, but JAX-RS doesn't define redirect handling
            URI location = e.getLocation();
            log.info("redirect {} to {}", uri, location);
            return client.target(location).request().get(String.class);
        } catch (BadRequestException e) {
            log.warn("request to " + uri + " failed: " + e.getResponse().readEntity(String.class), e);
            // 502 Bad Gateway should only be returned on a remote 5xx
            throw builderFor(BAD_GATEWAY).title("can't GET " + uri).detail(e.getMessage()).causedBy(e).build();
        } catch (Exception e) {
            log.warn("request to " + uri + " failed", e);
            // 502 Bad Gateway should only be returned on a remote 5xx
            throw builderFor(BAD_GATEWAY).title("can't GET " + uri).detail(e.getMessage()).causedBy(e).build();
        }
    }

    public String fallback(Throwable e) throws Exception {
        if (fallback != null)
            return fallback;
        else
            throw (Exception) e;
    }
}
