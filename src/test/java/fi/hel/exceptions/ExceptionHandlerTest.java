package fi.hel.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.configurations.ConfigProvider;
import fi.hel.processors.AuditExceptionProcessor;
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
    @InjectMock
    AuditExceptionProcessor auditExceptionProcessor;
    @InjectMock
    ConfigProvider configProvider;
    @ConfigProperty(name = "app.redis.prefix.auditExceptionInProgress")
    String auditExceptionInProgressPrefix;
    @ConfigProperty(name = "app.redis.prefix.iLoqCurrentCustomerCodeHasChanged")
    String iLoqCurrentCustomerCodeHasChangedPrefix;

    private MockEndpoint mock;
    private MockEndpoint throwingMock;
    private String saveHeadersAndBodyEndpoint = "direct:saveHeadersAndBody";
    private String restoreHeadersAndBodyEndpoint = "direct:restoreHeadersAndBody";
    private String testRoute = "direct:testRoute";
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
    @DisplayName("no route configuration")
    void testShouldRefreshTheAuthenticationTokenWhenAuthenticationFailsAndContinueRouting() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        ex.setProperty("counter", 1);

        throwingMock.whenAnyExchangeReceived(exchange -> {
            throw new HttpOperationFailedException(null, 401, "Unauthorized", null, null,
                    """
                            {
                                "Code": "Unauthorized",
                                "Message": "Invalid session id: 19e1eb4f-b8ec-43c7-9f3b-635a300e977e"
                            }
                            """);
        });

        doAnswer(i -> {
            testUtils.increaseCounter(ex);
            return null;
        }).when(redis).set(iLoqCurrentCustomerCodeHasChangedPrefix, "true");

        doAnswer(i -> {
            testUtils.increaseCounter(ex);
            return null;
        }).when(configProvider).getConfiguredCustomerCodes();
        mocked.getSaveHeadersAndBody().whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));
        mocked.getConfigureILoqSession().whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));
        mocked.getRestoreHeadersAndBody().whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));
        mocked.getOldhost().whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));

        verifyNoInteractions(configProvider);
        mocked.getSaveHeadersAndBody().expectedMessageCount(1);
        mocked.getSaveHeadersAndBody().expectedPropertyReceived("counter", 3);
        mocked.getConfigureILoqSession().expectedMessageCount(1);
        mocked.getConfigureILoqSession().expectedPropertyReceived("counter", 4);
        mocked.getRestoreHeadersAndBody().expectedMessageCount(1);
        mocked.getRestoreHeadersAndBody().expectedPropertyReceived("counter", 5);
        mocked.getOldhost().expectedMessageCount(1);
        mocked.getOldhost().expectedPropertyReceived("counter", 6);
        mock.expectedMessageCount(1);
        mock.expectedPropertyReceived("counter", 7);

        template.send(testRoute, ex);

        verify(redis).set(iLoqCurrentCustomerCodeHasChangedPrefix, "true");
        verify(configProvider).getConfiguredCustomerCodes();

        MockEndpoint.assertIsSatisfied(
                mocked.getSaveHeadersAndBody(),
                mocked.getConfigureILoqSession(),
                mocked.getRestoreHeadersAndBody(),
                mocked.getOldhost(),
                mock);
    }

    @Test
    void testShouldCreateAnAuditRecordWhenAnAuditExceptionIsReceived() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        throwingMock.whenAnyExchangeReceived((exchange) -> {
            throw new AuditException("irrelevant");
        });

        verifyNoInteractions(auditExceptionProcessor);

        template.send(testRoute, ex);

        verify(auditExceptionProcessor).setAuditRecord(ex);
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

    @Test
    @DisplayName("direct:saveHeadersAndBodyEndpoint")
    void testShouldSaveHeadersAndBody() throws Exception {
        String expectedHttpMethod = "POST";
        String expectedHttpUri = "http://www.foo.com";
        String expectedContentType = "application/json";
        String expectedHttpQuery = "$foo=bar";
        String expectedHttpPath = "/abc123";
        String expectedBody = "foobar";
        Exchange ex = testUtils.createExchange(expectedBody);
        ex.getIn().setHeader(Exchange.HTTP_METHOD, expectedHttpMethod);
        ex.getIn().setHeader(Exchange.HTTP_URI, expectedHttpUri);
        ex.getIn().setHeader(Exchange.CONTENT_TYPE, expectedContentType);
        ex.getIn().setHeader(Exchange.HTTP_QUERY, expectedHttpQuery);
        ex.getIn().setHeader(Exchange.HTTP_PATH, expectedHttpPath);

        template.send(saveHeadersAndBodyEndpoint, ex);

        String httpMethod = ex.getProperty("cachedMethod", String.class);
        String httpUri = ex.getProperty("cachedUri", String.class);
        String contentType = ex.getProperty("cachedContentType", String.class);
        String httpQuery = ex.getProperty("cachedHttpQuery", String.class);
        String httpPath = ex.getProperty("cachedHttpPath", String.class);
        String body = ex.getProperty("cachedBody", String.class);

        assertThat(httpMethod).isEqualTo(expectedHttpMethod);
        assertThat(httpUri).isEqualTo(expectedHttpUri);
        assertThat(contentType).isEqualTo(expectedContentType);
        assertThat(httpQuery).isEqualTo(expectedHttpQuery);
        assertThat(httpPath).isEqualTo(expectedHttpPath);
        assertThat(body).isEqualTo(expectedBody);
    }

    @Test
    @DisplayName("direct:restoreHeadersAndBody")
    void testShouldRestoreHeadersAndBody() throws Exception {
        String expectedHttpMethod = "POST";
        String expectedHttpUri = "http://www.foo.com";
        String expectedContentType = "application/json";
        String expectedHttpQuery = "$foo=bar";
        String expectedHttpPath = "/abc123";
        String expectedBody = "foobar";
        Exchange ex = testUtils.createExchange(expectedBody);
        ex.setProperty("cachedMethod", expectedHttpMethod);
        ex.setProperty("cachedUri", expectedHttpUri);
        ex.setProperty("cachedContentType", expectedContentType);
        ex.setProperty("cachedHttpQuery", expectedHttpQuery);
        ex.setProperty("cachedHttpPath", expectedHttpPath);
        ex.setProperty("cachedBody", expectedBody);

        template.send(restoreHeadersAndBodyEndpoint, ex);

        String httpMethod = ex.getIn().getHeader(Exchange.HTTP_METHOD, String.class);
        String httpUri = ex.getIn().getHeader(Exchange.HTTP_URI, String.class);
        String contentType = ex.getIn().getHeader(Exchange.CONTENT_TYPE, String.class);
        String httpQuery = ex.getIn().getHeader(Exchange.HTTP_QUERY, String.class);
        String httpPath = ex.getIn().getHeader(Exchange.HTTP_PATH, String.class);
        String body = ex.getIn().getBody(String.class);

        assertThat(httpMethod).isEqualTo(expectedHttpMethod);
        assertThat(httpUri).isEqualTo(expectedHttpUri);
        assertThat(contentType).isEqualTo(expectedContentType);
        assertThat(httpQuery).isEqualTo(expectedHttpQuery);
        assertThat(httpPath).isEqualTo(expectedHttpPath);
        assertThat(body).isEqualTo(expectedBody);
    }

}
