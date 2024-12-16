package fi.hel.exceptions;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@TestInstance(Lifecycle.PER_CLASS)
@TestProfile(ExceptionHandlerTest.class)
@QuarkusTest
public class ExceptionHandlerTest extends CamelQuarkusTestSupport {

    @Inject
    MockEndpointInjector mocked;
    @Inject
    TestUtils testUtils;
    @InjectMock
    Redis redis;
    @ConfigProperty(name = "app.redis.prefix.auditExceptionInProgress")
    String auditExceptionInProgressPrefix;

    private MockEndpoint mock;
    private MockEndpoint throwingMock;
    private String testRoute = "direct:testRoute1";
    private String throwingEndpoint = "mock:throwingEndpoing";
    private String mockEndpoint = "mock:mockEndpoint";

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(testRoute)
                        .routeId(testRoute)
                        .to(throwingEndpoint)
                        .to(mockEndpoint);
            }
        };
    }

    @Override
    protected void doAfterConstruct() throws Exception {
        context.addRoutes(new ExceptionHandler(true));
    }

    @Override
    protected void doPostSetup() throws Exception {
        super.doPostSetup();
        mock = getMockEndpoint(mockEndpoint);
        throwingMock = getMockEndpoint(throwingEndpoint);
    }

    @Test
    void testShouldRemoveTheAuditExceptionInProcessKeyWhenAnAuditExceptionIsReceived() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        throwingMock.whenAnyExchangeReceived((exchange) -> {
            throw new AuditException("irrelevant");
        });

        verifyNoInteractions(redis);

        template.send(testRoute, ex);

        verify(redis).del(auditExceptionInProgressPrefix);
    }

    @Test
    void testShouldStopRoutingAfterThrowingAnException() throws Exception {
        List<Exception> exceptions = List.of(
                new Exception("Oh no! An error!"),
                new HttpOperationFailedException(
                        "http://irrelevant.uri", 500, null, null, null, "This is the response body"));

        for (Exception exception : exceptions) {
            Exchange ex = testUtils.createExchange(null);
            throwingMock.whenAnyExchangeReceived((exchange) -> {
                throw exception;
            });

            mock.expectedMessageCount(0);

            template.send(testRoute, ex);

            mock.assertIsSatisfied();
            mock.reset();
        }
    }

}
