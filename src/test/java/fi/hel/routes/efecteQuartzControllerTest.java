package fi.hel.routes;

import static org.mockito.Mockito.when;

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.devikone.service.LeaderPodResolver;
import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.processors.ResourceInjector;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
@TestProfile(efecteQuartzControllerTest.class)
public class efecteQuartzControllerTest extends CamelQuarkusTestSupport {

    @Inject
    ResourceInjector ri;
    @Inject
    TestUtils testUtils;
    @Inject
    MockEndpointInjector mocked;
    @InjectMock
    LeaderPodResolver leaderPodResolver;

    @InjectMock
    Redis redis;

    @ConfigProperty(name = "app.routes.controller.efecte.quartz")
    String efecteQuartzControllerEndpoint;

    @Override
    protected void doPostSetup() throws Exception {
        when(leaderPodResolver.isLeaderPod()).thenReturn(true);
    }

    @Override
    protected void doPreSetup() throws Exception {
        super.doPostSetup();
        testConfiguration().withUseRouteBuilder(false);
    }

    @Test
    void testShouldCallTheEndpointsInCorrectOrder() throws Exception {
        Exchange ex = testUtils.createExchange();
        ex.setProperty("counter", 1);

        mocked.getGetMaxUpdated().whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));
        mocked.getCreateNewMaxUpdated().whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));
        mocked.getEfecteKeyCardsHandler().whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));
        mocked.getSetMaxUpdated().whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));
        when(redis.exists(ri.getILoqSessionIdPrefix())).thenReturn(true);

        mocked.getGetMaxUpdated().expectedMessageCount(1);
        mocked.getGetMaxUpdated().expectedPropertyReceived("counter", 1);
        mocked.getCreateNewMaxUpdated().expectedMessageCount(1);
        mocked.getCreateNewMaxUpdated().expectedPropertyReceived("counter", 2);
        mocked.getGetEfecteEntity().expectedMessageCount(1);
        mocked.getGetEfecteEntity().expectedPropertyReceived("counter", 3);
        mocked.getEfecteKeyCardsHandler().expectedMessageCount(1);
        mocked.getEfecteKeyCardsHandler().expectedPropertyReceived("counter", 4);
        mocked.getSetMaxUpdated().expectedMessageCount(1);
        mocked.getSetMaxUpdated().expectedPropertyReceived("counter", 5);
        mocked.getEfecteControllerCleanup().expectedMessageCount(1);
        mocked.getEfecteControllerCleanup().expectedPropertyReceived("counter", 6);

        template.send(efecteQuartzControllerEndpoint, ex);

        MockEndpoint.assertIsSatisfied(
                mocked.getGetMaxUpdated(),
                mocked.getCreateNewMaxUpdated(),
                mocked.getGetEfecteEntity(),
                mocked.getEfecteKeyCardsHandler(),
                mocked.getSetMaxUpdated(),
                mocked.getEfecteControllerCleanup());
    }

    @Test
    void testShouldBuildAnEfecteQueryBeforeGettingTheKeys() throws Exception {
        String maxUpdated = "07.08.2023 13:24";
        String expectedEfecteQuery = "SELECT entity FROM entity WHERE template.code = 'avain' AND $avain_tyyppi$ = 'iLOQ' AND $updated$ >= '%s' ORDER BY $avain_katuosoite$"
                .formatted(maxUpdated);

        Exchange ex = testUtils.createExchange();

        mocked.getGetMaxUpdated().whenAnyExchangeReceived(exchange -> exchange.setProperty("maxUpdated", maxUpdated));

        mocked.getGetEfecteEntity().expectedMessageCount(1);
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteEntityType", "key");
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteQuery", expectedEfecteQuery);

        template.send(efecteQuartzControllerEndpoint, ex);

        mocked.getGetEfecteEntity().assertIsSatisfied();
    }

    @Test
    void testShouldRemoveTheHeadersAfterFetchingEfecteKeyCards() throws Exception {
        Exchange ex = testUtils.createExchange();
        ex.getIn().setHeader("foo", "bar");

        mocked.getEfecteKeyCardsHandler().expectedMessageCount(1);
        mocked.getEfecteKeyCardsHandler().expectedHeaderReceived("foo", null);

        template.send(efecteQuartzControllerEndpoint, ex);

        mocked.getEfecteKeyCardsHandler().assertIsSatisfied();
    }

}
