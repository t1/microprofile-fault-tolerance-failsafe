package com.github.t1.faulttolerance.failsafe;

import com.github.t1.problem.ProblemDetail;
import com.github.t1.testtools.WebArchiveBuilder;
import io.dropwizard.testing.junit.DropwizardClientRule;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.ws.rs.*;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.function.Function;

import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.*;

@RunWith(Arquillian.class)
public class FaultToleranceIT {
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return new WebArchiveBuilder("fault-tolerance-it.war")
                .with(Fallback.class, RetryInterceptor.class)
                .with(JaxRs.class, FooGateway.class, Gateway.class, FooBoundary.class)
                .withBeansXml()
                .library("com.github.t1", "problem-detail")
                .library("com.github.t1", "stereotype-helper")
                .library("net.jodah", "failsafe")
                .library("org.eclipse.microprofile.fault-tolerance", "microprofile-fault-tolerance-api")
                .print().build();
    }

    @Path("/")
    public static class RemoteMock {
        private static String mockResponse;
        private static int mockCallCount;

        @GET public String get() {
            ++mockCallCount;
            if (mockResponse == null)
                throw new BadRequestException();
            return mockResponse;
        }
    }

    @Before
    public void setUp() {
        RemoteMock.mockResponse = null;
        RemoteMock.mockCallCount = 0;
    }

    @ClassRule public static final DropwizardClientRule remote = new DropwizardClientRule(RemoteMock.class);

    private final Client client = ClientBuilder.newClient();

    @ArquillianResource private URI baseUri;

    private Response GET() { return GET(null); }

    private Response GET(Function<WebTarget, WebTarget> function) {
        WebTarget webTarget = client.target(baseUri).queryParam("uri", remoteUri());
        if (function != null)
            webTarget = function.apply(webTarget);
        return webTarget.request().get();
    }

    private String remoteUri() { return remote.baseUri() + "/"; }

    @Test
    public void shouldStartClosed() {
        RemoteMock.mockResponse = "foo";

        Response response = GET();

        assertThat(response.getStatusInfo()).isEqualTo(OK);
        assertThat(response.readEntity(String.class)).isEqualTo("foo");
    }

    @Test
    public void shouldNotRetry() {
        Response response = GET();

        assertThat(RemoteMock.mockCallCount).isEqualTo(1);
        assertThat(response.getStatusInfo()).isEqualTo(BAD_GATEWAY);
        ProblemDetail problem = ProblemDetail.fromJson(response.readEntity(String.class));
        assertThat(problem.getTitle()).isEqualTo("can't GET " + remoteUri());
        assertThat(problem.getDetail()).isEqualTo("HTTP 400 Bad Request");
    }

    @Test
    public void shouldRetryOnce() {
        Response response = GET(target -> target.queryParam("retryOnce", true));

        assertThat(RemoteMock.mockCallCount).isEqualTo(2);
        assertThat(response.getStatusInfo()).isEqualTo(BAD_GATEWAY);
        ProblemDetail problem = ProblemDetail.fromJson(response.readEntity(String.class));
        assertThat(problem.getTitle()).isEqualTo("can't GET " + remoteUri());
        assertThat(problem.getDetail()).isEqualTo("HTTP 400 Bad Request");
    }

    @Test
    public void shouldFallBack() {
        Response response = GET(target -> target.queryParam("fallback", "bar"));

        assertThat(response.getStatusInfo()).isEqualTo(OK);
        assertThat(response.readEntity(String.class)).isEqualTo("bar");
    }
}
