package fi.hel.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.devikone.service.LeaderResolver;
import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntitySet;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import fi.hel.processors.ResourceInjector;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
@TestProfile(EfecteTriggerControllerTest.class)
public class EfecteTriggerControllerTest extends CamelQuarkusTestSupport {

    @Inject
    ResourceInjector ri;
    @Inject
    TestUtils testUtils;
    @Inject
    MockEndpointInjector mocked;
    @InjectMock
    LeaderResolver leaderResolver;

    @InjectMock
    Redis redis;

    private String efecteTriggerControllerEndpoint = "direct:efecteTriggerController";
    private String efecteCleanupControllerEndpoint = "direct:efecteCleanupController";

    @Override
    protected void doPostSetup() throws Exception {
        when(leaderResolver.isLeaderPod()).thenReturn(true);
    }

    @Override
    protected void doPreSetup() throws Exception {
        super.doPostSetup();
        testConfiguration().withUseRouteBuilder(false);
    }

    @Test
    @DisplayName("direct:efecteTriggerController")
    void testShouldCallTheEndpointsInCorrectOrder_EfecteTriggerController() throws Exception {
        Exchange ex = testUtils.createExchange(testUtils.writeAsXml(new EfecteEntitySet()));
        ex.setProperty("counter", 1);

        mocked.getLeaderRouteResolver().whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));
        mocked.getEfecteKeyCardsHandler().whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));

        mocked.getLeaderRouteResolver().expectedMessageCount(1);
        mocked.getLeaderRouteResolver().expectedPropertyReceived("counter", 1);
        mocked.getEfecteKeyCardsHandler().expectedMessageCount(1);
        mocked.getEfecteKeyCardsHandler().expectedPropertyReceived("counter", 2);
        mocked.getEfecteControllerCleanup().expectedMessageCount(1);
        mocked.getEfecteControllerCleanup().expectedPropertyReceived("counter", 3);

        template.send(efecteTriggerControllerEndpoint, ex);

        MockEndpoint.assertIsSatisfied(
                mocked.getLeaderRouteResolver(),
                mocked.getEfecteKeyCardsHandler(),
                mocked.getEfecteControllerCleanup());
    }

    @Test
    @DisplayName("direct:efecteTriggerController")
    void testShouldConvertTheBodyToAListOfEfecteEntitiesBeforeProcessing() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withId("12345")
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        List<EfecteEntity> expectedEfecteEntities = new ArrayList<>();
        expectedEfecteEntities.add(efecteEntity);
        EfecteEntitySet efecteEntitySet = new EfecteEntitySet(expectedEfecteEntities);

        Exchange ex = testUtils.createExchange(testUtils.writeAsXml(efecteEntitySet));

        template.send(efecteTriggerControllerEndpoint, ex);

        Exchange exchange = mocked.getEfecteKeyCardsHandler().getExchanges().get(0);

        assertThat(exchange.getIn().getBody()).isEqualTo(expectedEfecteEntities);
    }

    @Test
    @DisplayName("direct:efecteCleanupController")
    void testShouldNotTryToKillAnILoqSessionWhenThereAreNoOngoingSessions() throws Exception {
        Exchange ex = testUtils.createExchange(testUtils.writeAsXml(new EfecteEntitySet()));

        when(redis.exists(ri.getILoqCurrentSessionIdPrefix())).thenReturn(false);

        mocked.getKillILoqSession().expectedMessageCount(0);

        template.send(efecteCleanupControllerEndpoint, ex);

        mocked.getKillILoqSession().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:efecteCleanupController")
    void testShouldCallTheEndpointsInCorrectOrder_EfecteCleanupController() throws Exception {
        Exchange ex = testUtils.createExchange(testUtils.writeAsXml(new EfecteEntitySet()));
        ex.setProperty("counter", 1);

        when(redis.exists(ri.getILoqCurrentSessionIdPrefix())).thenReturn(true);

        mocked.getKillILoqSession().whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));
        mocked.getRemoveCurrentILoqSessionRelatedKeys()
                .whenAnyExchangeReceived(exchange -> testUtils.increaseCounter(exchange));

        mocked.getKillILoqSession().expectedMessageCount(1);
        mocked.getKillILoqSession().expectedPropertyReceived("counter", 1);
        mocked.getRemoveCurrentILoqSessionRelatedKeys().expectedMessageCount(1);
        mocked.getRemoveCurrentILoqSessionRelatedKeys().expectedPropertyReceived("counter", 2);
        mocked.getRemoveTempKeys().expectedMessageCount(1);
        mocked.getRemoveTempKeys().expectedPropertyReceived("counter", 3);

        template.send(efecteCleanupControllerEndpoint, ex);

        MockEndpoint.assertIsSatisfied(
                mocked.getKillILoqSession(),
                mocked.getEfecteControllerCleanup(),
                mocked.getRemoveTempKeys());
    }

    @Test
    @DisplayName("direct:efecteCleanupController")
    void testShouldReleaseTheLeaderRouteStatusAtTheEnd() throws Exception {
        Exchange ex = testUtils.createExchange();
        AtomicBoolean handled = new AtomicBoolean(false);
        AtomicBoolean throwException = new AtomicBoolean(true);

        mocked.getRemoveTempKeys().whenAnyExchangeReceived(e -> handled.set(true));
        doAnswer(i -> {
            if (handled.get()) {
                throwException.set(false);
            }
            return null;
        }).when(leaderResolver).releaseLeaderRoute();

        verifyNoInteractions(leaderResolver);

        template.send(efecteCleanupControllerEndpoint, ex);

        verify(leaderResolver).releaseLeaderRoute();

        if (throwException.get()) {
            throw new Exception(
                    "Invalid route sequence order");
        }
    }
}
