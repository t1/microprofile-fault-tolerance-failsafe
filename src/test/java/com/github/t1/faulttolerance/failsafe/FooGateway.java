package com.github.t1.faulttolerance.failsafe;

import org.eclipse.microprofile.faulttolerance.*;
import org.slf4j.*;

import javax.enterprise.inject.Produces;
import javax.ws.rs.*;
import javax.ws.rs.client.*;
import java.net.URI;

import static com.github.t1.problem.WebException.*;
import static javax.ws.rs.core.Response.Status.*;

@Gateway
public class FooGateway {
    public static final Logger log = LoggerFactory.getLogger(FooGateway.class);

    private final Client client = ClientBuilder.newClient();

    int callNumber;


    @Retry(maxRetries = 1)
    public String runWithOneRetry(URI uri) { return run(uri); }


    @Fallback(fallbackMethod = "fallback")
    public String runWithFallbackMethod(URI uri) { return run(uri); }

    @SuppressWarnings("unused") public String fallback() {
        log.info("fallback-method");
        return "fallback-method-" + FooBoundary.requestId;
    }


    @Fallback(InnerFallbackHandler.class)
    public String runWithInnerFallbackHandler(URI uri) { return run(uri); }

    @Produces InnerFallbackHandler produceInnerFallbackHandler() { return new InnerFallbackHandler(); }

    private class InnerFallbackHandler implements FallbackHandler<String> {
        @Override public String handle(ExecutionContext context) {
            log.info("inner-fallback");
            return "inner-fallback-handler-" + FooBoundary.requestId;
        }
    }


    @Fallback(NestedFallbackHandler.class)
    public String runWithNestedFallbackHandler(URI uri) { return run(uri); }

    private static class NestedFallbackHandler implements FallbackHandler<String> {
        @Override public String handle(ExecutionContext context) {
            log.info("nested-fallback");
            return "nested-fallback-handler-" + FooBoundary.requestId;
        }
    }


    @Timeout(100)
    public String runWithTimeout(URI uri) {
        return run(uri);
    }


    public String run(URI uri) {
        log.info("call #" + ++callNumber + " to " + uri);
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
