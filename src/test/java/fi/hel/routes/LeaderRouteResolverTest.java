package fi.hel.routes;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.devikone.service.LeaderResolver;
import com.devikone.test_utils.TestUtils;

import fi.hel.processors.ResourceInjector;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
@TestProfile(LeaderRouteResolverTest.class)
public class LeaderRouteResolverTest extends CamelQuarkusTestSupport {

    @Inject
    TestUtils testUtils;
    @Inject
    ResourceInjector ri;
    @InjectMock
    LeaderResolver leaderResolver;
    @ConfigProperty(name = "MAX_LEADER_ROUTE_RESOLVING_RETRY_COUNT")
    Integer maxLeaderRouteResolvingRetryCount;
    @ConfigProperty(name = "DELAY_MS_BETWEEN_RESOLVING_LEADER_ROUTE")
    Integer delayMsBetweenResolvingLeaderRoute;

    private String leaderRouteResolverEndpoint = "direct:leaderRouteResolver";

    private String mockEndpoint = "mock:mockEndpoint";
    private MockEndpoint mock;

    @Override
    protected void doPostSetup() throws Exception {
        mock = getMockEndpoint(mockEndpoint);
    }

    @Override
    protected void doAfterConstruct() throws Exception {
        testUtils.addMockEndpointTo(leaderRouteResolverEndpoint, mockEndpoint);
    }

    @Override
    protected void doPreSetup() throws Exception {
        super.doPostSetup();
        testConfiguration().withUseRouteBuilder(false);
    }

    @Test
    @DisplayName("direct:leaderRouteResolver")
    void testShouldContinueRoutingOnceTheLeaderRouteStatusIsAcquired() throws Exception {
        Exchange ex = testUtils.createExchange();

        when(leaderResolver.isLeaderRoute()).thenReturn(true);
        verifyNoInteractions(leaderResolver);
        mock.expectedMessageCount(1);

        template.send(leaderRouteResolverEndpoint, ex);

        verify(leaderResolver).isLeaderRoute();
        mock.assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:leaderRouteResolver")
    void testShouldTryTheMaximumTimesToAcquireTheLeaderRouteStatus() throws Exception {
        Exchange ex = testUtils.createExchange();

        when(leaderResolver.isLeaderRoute()).thenReturn(false);
        verifyNoInteractions(leaderResolver);

        template.send(leaderRouteResolverEndpoint, ex);

        verify(leaderResolver, times(maxLeaderRouteResolvingRetryCount)).isLeaderRoute();
    }

    @Test
    @DisplayName("direct:leaderRouteResolver")
    void testShouldStopRoutingWhenTheLeaderRouteStatusCanNotBeAcquired() throws Exception {
        Exchange ex = testUtils.createExchange();

        when(leaderResolver.isLeaderRoute()).thenReturn(false);
        mock.expectedMessageCount(0);

        template.send(leaderRouteResolverEndpoint, ex);

        mock.assertIsSatisfied();
    }

}
