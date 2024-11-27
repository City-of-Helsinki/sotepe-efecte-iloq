package fi.hel.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;

import com.devikone.TestUtils;
import com.devikone.service.LeaderPodResolver;
import com.devikone.transports.Redis;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class WebhookControllerTest extends CamelQuarkusTestSupport {

    @Inject
    TestUtils testUtils;
    @InjectMock
    Redis redis;
    @InjectMock
    LeaderPodResolver leaderPodResolver;

    @ConfigProperty(name = "WEBHOOK_API_TOKEN")
    String webhookApiToken;
    @ConfigProperty(name = "INTEGRATION_ENDPOINT")
    String integrationEndpoint;
    @ConfigProperty(name = "KEY_EXPIRATION_SECONDS")
    Long keyExpirationSeconds;
    @ConfigProperty(name = "app.redis.prefix.request")
    String requestPrefix;

    private String webhookControllerEndpoint = "direct:efecteWebhookController";
    private String mockEndpoint = "mock:mockEndpoint";
    private MockEndpoint integrationMock;

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected void doAfterConstruct() throws Exception {
        mockHTTPEndpoint();
    }

    @Override
    protected void doPostSetup() throws Exception {
        super.doPostSetup();
        integrationMock = getMockEndpoint(mockEndpoint);
        when(leaderPodResolver.isLeaderPod()).thenReturn(true);
    }

    @Test
    @Order(1)
    @DisplayName("Authentication")
    void testShouldNotForwardRequestsWithMissingAuthentication() throws Exception {
        Exchange ex = testUtils.createExchange(null);

        integrationMock.expectedMessageCount(0);

        template.send(webhookControllerEndpoint, ex);

        integrationMock.assertIsSatisfied();
    }

    @Test
    @Order(2)
    @DisplayName("Authentication")
    void testShouldNotForwardRequestsWithWrongAuthentication() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        ex.getIn().setHeader("Authorization", "invalidToken");

        integrationMock.expectedMessageCount(0);

        template.send(webhookControllerEndpoint, ex);

        integrationMock.assertIsSatisfied();
    }

    @Test
    @Order(3)
    @DisplayName("Authentication")
    void testShouldRespondWithStatusCode401ForUnauthorizedRequests() throws Exception {
        Exchange ex = testUtils.createExchange(null);
        ex.getIn().setHeader("Authorization", "invalidToken");

        template.send(webhookControllerEndpoint, ex);

        String statusCode = ex.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, String.class);

        assertThat(statusCode).isEqualTo("401");
    }

    @Test
    @Order(4)
    @DisplayName("direct:webhookController")
    void testShouldSaveTheRequestToRedis() throws Exception {
        String expectedBody = "Hello from webhook service";
        Exchange ex = createAuthenticatedExchange(expectedBody);
        String datetime = testUtils.createDatetimeNow("yyyy-MM-dd_HH:mm");
        String expectedPrefix = requestPrefix + datetime;

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> expirationTimeCaptor = ArgumentCaptor.forClass(Long.class);

        verifyNoInteractions(redis);

        template.send(webhookControllerEndpoint, ex);

        verify(redis).setex(keyCaptor.capture(), valueCaptor.capture(), expirationTimeCaptor.capture());

        assertThat(keyCaptor.getValue()).contains(expectedPrefix);
        assertThat(valueCaptor.getValue()).isEqualTo(expectedBody);
        assertThat(expirationTimeCaptor.getValue()).isEqualTo(keyExpirationSeconds);
    }

    @Test
    @Order(5)
    @DisplayName("direct:webhookController")
    void testShouldForwardTheRequestToTheEndpoint() throws Exception {
        String expectedBody = "Hello from webhook service";
        Exchange ex = createAuthenticatedExchange(expectedBody);

        integrationMock.expectedMessageCount(1);
        integrationMock.expectedBodiesReceived(expectedBody);

        template.send(webhookControllerEndpoint, ex);

        integrationMock.assertIsSatisfied();
    }

    @Test
    @Order(6)
    @DisplayName("direct:webhookController")
    void testShouldRemoveTheOriginalHeadersBeforeForwarding() throws Exception {
        Exchange ex = createAuthenticatedExchange("irrelevant");
        ex.getIn().setHeader(Exchange.HTTP_PATH, "irrelevant");
        ex.getIn().setHeader(Exchange.HTTP_URI, "irrelevant");
        ex.getIn().setHeader("foo", "irrelevant");

        integrationMock.expectedMessageCount(1);
        integrationMock.expectedHeaderReceived(Exchange.HTTP_PATH, null);
        integrationMock.expectedHeaderReceived(Exchange.HTTP_PATH, null);
        integrationMock.expectedHeaderReceived("foo", null);

        template.send(webhookControllerEndpoint, ex);

        integrationMock.assertIsSatisfied();
    }

    @Test
    @Order(7)
    @DisplayName("direct:webhookController")
    void testShouldAddTheHttpMethodHeader() throws Exception {
        Exchange ex = createAuthenticatedExchange("irrelevant");

        integrationMock.expectedMessageCount(1);
        integrationMock.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.send(webhookControllerEndpoint, ex);

        integrationMock.assertIsSatisfied();
    }

    @Test
    @Order(8)
    @DisplayName("direct:webhookController")
    void testShouldSetTheQueryParameter() throws Exception {
        String expectedHttpQuery = "bridgeEndpoint=true";
        Exchange ex = createAuthenticatedExchange("irrelevant");

        integrationMock.expectedMessageCount(1);
        integrationMock.expectedHeaderReceived(Exchange.HTTP_QUERY, expectedHttpQuery);

        template.send(webhookControllerEndpoint, ex);

        integrationMock.assertIsSatisfied();
    }

    @Test
    @Order(9)
    @DisplayName("direct:webhookController")
    void testShouldReturnStatusCode200() throws Exception {
        Exchange ex = createAuthenticatedExchange("irrelevant");

        template.send(webhookControllerEndpoint, ex);

        String statusCode = ex.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, String.class);

        assertThat(statusCode).isEqualTo("200");
    }

    private Exchange createAuthenticatedExchange(String body) {
        Exchange ex = testUtils.createExchange(body);
        ex.getIn().setHeader("Authorization", "Bearer " + webhookApiToken);

        return ex;
    }

    private void mockHTTPEndpoint() throws Exception {
        AdviceWith.adviceWith("efecteWebhookController", context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint(integrationEndpoint)
                        .skipSendToOriginalEndpoint()
                        .to(mockEndpoint);
            }
        });
    }
}
