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

    @Retry(maxRetries = 1)
    public String runWithOneRetry(URI uri) { return run(uri); }

    @Fallback(fallbackMethod = "fallback")
    public String runWithFallbackMethod(URI uri) { return run(uri); }

    @SuppressWarnings("unused") public String fallback(Throwable e) {
        return "fallback-method-" + FooBoundary.requestId;
    }

    @Fallback(InnerFallbackHandler.class)
    public String runWithInnerFallbackHandler(URI uri) { return run(uri); }

    private class InnerFallbackHandler implements FallbackHandler<String> {
        @Override public String handle(ExecutionContext context) {
            return "inner-fallback-handler-"
                    + FooBoundary.requestId;
        }
    }

    @Fallback(NestedFallbackHandler.class)
    public String runWithNestedFallbackHandler(URI uri) { return run(uri); }

    private static class NestedFallbackHandler implements FallbackHandler<String> {
        @Override public String handle(ExecutionContext context) {
            return "nested-fallback-handler-" + FooBoundary.requestId;
        }
    }

    public String run(URI uri) {
        log.debug("call number " + ++callNumber + " to " + uri);
        try {
            return client.target(uri).request().get(String.class);
        } catch (RedirectionException e) { // ugly, but JAX-RS doesn't define redirect handling
            URI location = e.getLocation();
            log.info("redirect {} to {}", uri, location);
            return client.target(location).request().get(String.class);
        } catch (BadRequestException e) {
            log.warn("request to " + uri + " failed: " + e.getResponse().readEntity(String.class));
            // 502 Bad Gateway should only be returned on a remote 5xx
            throw builderFor(BAD_GATEWAY).title("can't GET " + uri).detail(e.getMessage()).causedBy(e).build();
        } catch (Exception e) {
            log.warn("request to " + uri + " failed: " + e.getMessage());
            // 502 Bad Gateway should only be returned on a remote 5xx
            throw builderFor(BAD_GATEWAY).title("can't GET " + uri).detail(e.getMessage()).causedBy(e).build();
        }
    }
}
