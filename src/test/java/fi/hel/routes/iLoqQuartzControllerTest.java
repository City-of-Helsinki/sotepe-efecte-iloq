package fi.hel.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.devikone.service.LeaderPodResolver;
import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.configurations.ConfigProvider;
import fi.hel.mappers.ILoqKeyMapper;
import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntitySet;
import fi.hel.models.ILoqKey;
import fi.hel.models.ILoqKeyImport;
import fi.hel.models.ILoqKeyResponse;
import fi.hel.models.ILoqSecurityAccess;
import fi.hel.models.PreviousEfecteKey;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import fi.hel.processors.EfecteKeyProcessor;
import fi.hel.processors.ILoqKeyProcessor;
import fi.hel.processors.ResourceInjector;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
@TestProfile(iLoqQuartzControllerTest.class)
public class iLoqQuartzControllerTest extends CamelQuarkusTestSupport {

    @Inject
    ResourceInjector ri;
    @Inject
    MockEndpointInjector mocked;
    @Inject
    TestUtils testUtils;
    @InjectMock
    Redis redis;
    @InjectMock
    ConfigProvider configProvider;
    @InjectMock
    ILoqKeyProcessor iLoqKeyProcessor;
    @InjectMock
    ILoqKeyMapper iLoqKeyMapper;
    @InjectMock
    EfecteKeyProcessor efecteKeyProcessor;
    @InjectMock
    LeaderPodResolver leaderPodResolver;

    private String iLoqControllerEndpoint = "direct:iLoqController";
    private String enrichKeyWithSecurityAccessesEndpoint = "direct:enrichKeyWithSecurityAccesses";

    private String mockEndpoint = "mock:mockEndpoint";
    private MockEndpoint mock;

    @Override
    protected void doPostSetup() throws Exception {
        when(leaderPodResolver.isLeaderPod()).thenReturn(true);
        mock = getMockEndpoint(mockEndpoint);
    }

    @Override
    protected void doAfterConstruct() throws Exception {
        testUtils.addMockEndpointTo(enrichKeyWithSecurityAccessesEndpoint, mockEndpoint);
    }

    @Override
    protected void doPreSetup() throws Exception {
        super.doPostSetup();
        testConfiguration().withUseRouteBuilder(false);
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldGetTheListOfAvailableCustomerCodes() throws Exception {
        Exchange ex = testUtils.createExchange();

        verifyNoInteractions(configProvider);

        template.send(iLoqControllerEndpoint, ex);

        verify(configProvider).getConfiguredCustomerCodes();
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldDefineTheCustomerCodeAsChanged() throws Exception {
        Exchange ex = testUtils.createExchange();

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant"));
        setDefaultResponse();

        verifyNoInteractions(redis);

        template.send(iLoqControllerEndpoint, ex);

        verify(redis).set(ri.getILoqCurrentCustomerCodeHasChangedPrefix(), "true");
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldSaveTheCustomerCredentialsToRedis() throws Exception {
        Exchange ex = testUtils.createExchange();
        String expectedCustomerCode = "abc123";

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of(expectedCustomerCode));
        setDefaultResponse();

        verifyNoInteractions(configProvider);

        template.send(iLoqControllerEndpoint, ex);

        verify(configProvider).saveCurrentCredentialsToRedis(expectedCustomerCode);
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldListTheILoqKeys() throws Exception {
        Exchange ex = testUtils.createExchange();
        String customerCode = "irrelevant";

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of(customerCode));

        setDefaultResponse();

        mocked.getListILoqKeys().expectedMessageCount(1);

        template.send(iLoqControllerEndpoint, ex);

        mocked.getListILoqKeys().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldGetTheILoqKeysWithVerifiedRealEstate() throws Exception {
        Exchange ex = testUtils.createExchange();
        String customerCode = "irrelevant";

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of(customerCode));
        setDefaultResponse();
        mocked.getListILoqKeys().whenAnyExchangeReceived(exchange -> {
            exchange.getIn().setBody(List.of());
            exchange.setProperty("foo", "bar");
        });

        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.getIn().setBody(List.of());

            assertThat(exchange.getProperty("foo")).isEqualTo("bar");

            return null;
        }).when(iLoqKeyProcessor).getILoqKeysWithVerifiedRealEstate(any(Exchange.class));

        verifyNoInteractions(iLoqKeyProcessor);

        template.send(iLoqControllerEndpoint, ex);

        verify(iLoqKeyProcessor).getILoqKeysWithVerifiedRealEstate(any(Exchange.class));
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldCheckIfTheKeyIsMissingAPerson() throws Exception {
        Exchange ex = testUtils.createExchange();

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        ILoqKeyResponse expectedILoqKeyResponse = new ILoqKeyResponse();
        AtomicReference<ILoqKeyResponse> currentILoqKeyRef = new AtomicReference<>();

        setDefaultResponse();
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.getIn().setBody(List.of(expectedILoqKeyResponse));
            return null;
        }).when(iLoqKeyProcessor).getILoqKeysWithVerifiedRealEstate(any(Exchange.class));

        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            currentILoqKeyRef.set(exchange.getProperty("currentILoqKey", ILoqKeyResponse.class));

            return null;
        }).when(iLoqKeyProcessor).isMissingAPerson(any(Exchange.class));

        verifyNoInteractions(iLoqKeyProcessor);

        template.send(iLoqControllerEndpoint, ex);

        verify(iLoqKeyProcessor).isMissingAPerson(any(Exchange.class));
        assertThat(currentILoqKeyRef.get()).isEqualTo(expectedILoqKeyResponse);
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldStopProcessingILoqKeysThatAreNotLinkedToAPerson() throws Exception {
        Exchange ex = testUtils.createExchange();

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));
        String expectedILoqKeyId = "abc-123";
        ILoqKeyResponse expectedKeyResponse = new ILoqKeyResponse(expectedILoqKeyId);

        setDefaultResponse();
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.getIn().setBody(List.of(expectedKeyResponse));
            return null;
        }).when(iLoqKeyProcessor).getILoqKeysWithVerifiedRealEstate(any(Exchange.class));
        when(iLoqKeyProcessor.isMissingAPerson(any(Exchange.class))).thenReturn(true);

        mocked.getEnrichKeyWithSecurityAccesses().expectedMessageCount(0);

        template.send(iLoqControllerEndpoint, ex);

        mocked.getEnrichKeyWithSecurityAccesses().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldEnrichTheKeyUnderProcessingWithSecurityAccessesWhenILoqKeyIsActive() throws Exception {
        Exchange ex = testUtils.createExchange();

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));
        String expectedILoqKeyId = "abc-123";
        ILoqKeyResponse expectedKeyResponse = new ILoqKeyResponse(expectedILoqKeyId);

        setDefaultResponse();
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.getIn().setBody(List.of(expectedKeyResponse));
            return null;
        }).when(iLoqKeyProcessor).getILoqKeysWithVerifiedRealEstate(any(Exchange.class));

        mocked.getEnrichKeyWithSecurityAccesses().expectedMessageCount(1);
        mocked.getEnrichKeyWithSecurityAccesses().expectedPropertyReceived("currentILoqKey", expectedKeyResponse);
        mocked.getEnrichKeyWithSecurityAccesses().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);

        template.send(iLoqControllerEndpoint, ex);

        mocked.getEnrichKeyWithSecurityAccesses().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldBuildAnEfecteKeyForNonPassiveILoqKey() throws Exception {
        Exchange ex = testUtils.createExchange();

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        mocked.getListILoqKeys()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(new ILoqKeyResponse())));

        verifyNoInteractions(efecteKeyProcessor);

        template.send(iLoqControllerEndpoint, ex);

        verify(efecteKeyProcessor).buildEfecteKey(any(Exchange.class));
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldUpdateAnEfecteKey() throws Exception {
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY-00123";
        String expectedILoqId = "abc-123";
        String expectedEfectePayload = "EfecteEntitySetImport payload";
        String expectedEfecteEntityType = "key";
        String expectedEfecteOperation = "update";
        String expectedEfectePath = "/dataCardImport.ws";
        String expectedEfecteQuery = "folderCode=avaimet&updateDataCards=true";
        Exchange ex = testUtils.createExchange();

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        mocked.getListILoqKeys()
                .whenAnyExchangeReceived(
                        exchange -> exchange.getIn().setBody(List.of(new ILoqKeyResponse(expectedILoqId))));

        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.setProperty("shouldUpdateEfecteKey", true);
            exchange.setProperty("efectePayload", expectedEfectePayload);
            exchange.setProperty("efecteKeyEntityId", expectedEntityId);
            exchange.setProperty("efecteKeyEfecteId", expectedEfecteId);
            return null;
        }).when(efecteKeyProcessor).buildEfecteKey(any(Exchange.class));

        mocked.getProcessEfecteRequest().expectedMessageCount(1);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efectePayload", expectedEfectePayload);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteEntityType", expectedEfecteEntityType);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteOperation", expectedEfecteOperation);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("entityId", expectedEntityId);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteId", expectedEfecteId);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("iLoqId", expectedILoqId);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efectePath", expectedEfectePath);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteQuery", expectedEfecteQuery);

        template.send(iLoqControllerEndpoint, ex);

        mocked.getProcessEfecteRequest().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldCreateAnEfecteKey() throws Exception {
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY-00123";
        String expectedILoqId = "abc-123";
        String expectedEfectePayload = "EfecteEntitySetImport payload";
        String expectedEfecteEntityType = "key";
        String expectedEfecteOperation = "create";
        String expectedEfectePath = "/dataCardImport.ws";
        String expectedEfecteQuery = "folderCode=avaimet&createDataCards=true";
        Exchange ex = testUtils.createExchange();

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        mocked.getListILoqKeys()
                .whenAnyExchangeReceived(
                        exchange -> exchange.getIn().setBody(List.of(new ILoqKeyResponse(expectedILoqId))));
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> exchange.getIn()
                .setBody(List.of(new EfecteEntityBuilder().withDefaults(EnumEfecteTemplate.KEY).build())));
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.setProperty("shouldCreateEfecteKey", true);
            exchange.setProperty("efectePayload", expectedEfectePayload);
            exchange.setProperty("efecteKeyEntityId", expectedEntityId);
            exchange.setProperty("efecteKeyEfecteId", expectedEfecteId);
            return null;
        }).when(efecteKeyProcessor).buildEfecteKey(any(Exchange.class));

        mocked.getProcessEfecteRequest().expectedMessageCount(1);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efectePayload", expectedEfectePayload);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteEntityType", expectedEfecteEntityType);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteOperation", expectedEfecteOperation);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("entityId", expectedEntityId);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteId", expectedEfecteId);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("iLoqId", expectedILoqId);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efectePath", expectedEfectePath);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteQuery", expectedEfecteQuery);

        template.send(iLoqControllerEndpoint, ex);

        mocked.getProcessEfecteRequest().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldFetchTheCreatedEfecteKey() throws Exception {
        String expectedILoqId = "abc-123";
        Exchange ex = testUtils.createExchange();

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        mocked.getListILoqKeys()
                .whenAnyExchangeReceived(
                        exchange -> exchange.getIn().setBody(List.of(new ILoqKeyResponse(expectedILoqId))));
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> exchange.getIn()
                .setBody(List.of(new EfecteEntityBuilder().withDefaults(EnumEfecteTemplate.KEY).build())));
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.setProperty("shouldCreateEfecteKey", true);
            return null;
        }).when(efecteKeyProcessor).buildEfecteKey(any(Exchange.class));

        String expectedEfecteQuery = """
                SELECT entity FROM entity WHERE template.code = 'avain' AND $avain_external_id$ = '%s'
                """.formatted(expectedILoqId)
                .replaceAll("\\s+", " ")
                .trim();

        mocked.getGetEfecteEntity().expectedMessageCount(1);
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteEntityType", "key");
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteQuery", expectedEfecteQuery);

        template.send(iLoqControllerEndpoint, ex);

        mocked.getGetEfecteEntity().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldSaveTheMappedKeys() throws Exception {
        String expectedILoqId = "abc-123";
        Exchange ex = testUtils.createExchange();

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        mocked.getListILoqKeys()
                .whenAnyExchangeReceived(
                        exchange -> exchange.getIn().setBody(List.of(new ILoqKeyResponse(expectedILoqId))));

        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.setProperty("shouldCreateEfecteKey", true);
            return null;
        }).when(efecteKeyProcessor).buildEfecteKey(any(Exchange.class));

        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY_000123";
        List<EfecteEntity> efecteKeys = List.of(
                new EfecteEntityBuilder()
                        .withId(expectedEntityId)
                        .withKeyEfecteId(expectedEfecteId)
                        .build());

        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(efecteKeys));

        mocked.getSaveMappedKeys().expectedMessageCount(1);
        mocked.getSaveMappedKeys().expectedPropertyReceived("efecteKeyEntityId", expectedEntityId);
        mocked.getSaveMappedKeys().expectedPropertyReceived("efecteKeyEfecteId", expectedEfecteId);
        mocked.getSaveMappedKeys().expectedPropertyReceived("iLoqKeyId", expectedILoqId);

        template.send(iLoqControllerEndpoint, ex);

        mocked.getSaveMappedKeys().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldBuildAnILoqPayloadWithTheFetchedEfecteKey() throws Exception {
        String expectedILoqId = "abc-123";
        Exchange ex = testUtils.createExchange();
        ILoqKeyResponse expectedILoqKeyResponse = new ILoqKeyResponse(expectedILoqId);

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        mocked.getListILoqKeys()
                .whenAnyExchangeReceived(
                        exchange -> exchange.getIn().setBody(List.of(expectedILoqKeyResponse)));

        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.setProperty("shouldCreateEfecteKey", true);
            return null;
        }).when(efecteKeyProcessor).buildEfecteKey(any(Exchange.class));

        EfecteEntity expectedEfecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        List<EfecteEntity> efecteKeys = List.of(expectedEfecteEntity);

        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(efecteKeys));

        verifyNoInteractions(iLoqKeyMapper);

        template.send(iLoqControllerEndpoint, ex);

        verify(iLoqKeyMapper).buildUpdatedILoqKey(expectedEfecteEntity, expectedILoqKeyResponse);
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldSetThePropertyValuesBeforeUpdatingAnILoqKeyAfterCreatingAnEfecteKeyCard() throws Exception {
        String expectedILoqId = "abc-123";
        Exchange ex = testUtils.createExchange();
        ILoqKeyResponse expectedILoqKeyResponse = new ILoqKeyResponse(expectedILoqId);

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        mocked.getListILoqKeys()
                .whenAnyExchangeReceived(
                        exchange -> exchange.getIn().setBody(List.of(expectedILoqKeyResponse)));

        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.setProperty("shouldCreateEfecteKey", true);
            return null;
        }).when(efecteKeyProcessor).buildEfecteKey(any(Exchange.class));

        List<EfecteEntity> efecteKeys = List.of(new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build());

        ILoqKeyImport expectedILoqPayload = new ILoqKeyImport(new ILoqKey("irrelevant"));

        when(iLoqKeyMapper.buildUpdatedILoqKey(any(), any())).thenReturn(expectedILoqPayload);
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(efecteKeys));

        mocked.getProcessILoqKey().expectedMessageCount(1);
        mocked.getProcessILoqKey().expectedPropertyReceived("iLoqPayload", expectedILoqPayload);
        mocked.getProcessILoqKey().expectedPropertyReceived("iLoqKeyId", expectedILoqId);
        mocked.getProcessILoqKey().expectedPropertyReceived("operation", "update");
        mocked.getProcessILoqKey().expectedPropertyReceived("method", "PUT");
        mocked.getProcessILoqKey().expectedPropertyReceived("from", EnumDirection.ILOQ);
        mocked.getProcessILoqKey().expectedPropertyReceived("to", EnumDirection.EFECTE);

        template.send(iLoqControllerEndpoint, ex);

        mocked.getProcessILoqKey().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldUpdateILoqKey() throws Exception {
        String expectedILoqId = "abc-123";
        String expectedILoqPayload = "ILoqKeyImport payload";
        Exchange ex = testUtils.createExchange();

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        mocked.getListILoqKeys()
                .whenAnyExchangeReceived(
                        exchange -> exchange.getIn().setBody(List.of(new ILoqKeyResponse(expectedILoqId))));

        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.setProperty("shouldUpdateILoqKey", true);
            exchange.setProperty("iLoqPayload", expectedILoqPayload);
            return null;
        }).when(efecteKeyProcessor).buildEfecteKey(any(Exchange.class));

        mocked.getProcessILoqKey().expectedMessageCount(1);
        mocked.getProcessILoqKey().expectedPropertyReceived("iLoqPayload", expectedILoqPayload);
        mocked.getProcessILoqKey().expectedPropertyReceived("iLoqKeyId", expectedILoqId);
        mocked.getProcessILoqKey().expectedPropertyReceived("operation", "update");
        mocked.getProcessILoqKey().expectedPropertyReceived("method", "PUT");
        mocked.getProcessILoqKey().expectedPropertyReceived("from", EnumDirection.ILOQ);
        mocked.getProcessILoqKey().expectedPropertyReceived("to", EnumDirection.EFECTE);

        template.send(iLoqControllerEndpoint, ex);

        mocked.getProcessILoqKey().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:efecteEntitySetHandler - create new iLOQ key")
    void testShouldSaveTheNewPreviousKeyValuesForBothSystemsToRedis() throws Exception {
        Exchange ex = testUtils.createExchange();

        PreviousEfecteKey fakeNewPreviousEfecteKey = new PreviousEfecteKey();
        Set<String> fakeNewILoqSecurityAccessIds = Set.of("bar");

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        mocked.getListILoqKeys()
                .whenAnyExchangeReceived(
                        exchange -> exchange.getIn().setBody(List.of(new ILoqKeyResponse("abc-123"))));

        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.setProperty("newPreviousEfecteKey", fakeNewPreviousEfecteKey);
            exchange.setProperty("newILoqSecurityAccessIds", fakeNewILoqSecurityAccessIds);
            return null;
        }).when(efecteKeyProcessor).buildEfecteKey(any(Exchange.class));

        mocked.getSavePreviousKeyInfos().expectedMessageCount(1);
        mocked.getSavePreviousKeyInfos().expectedPropertyReceived("newPreviousEfecteKey", fakeNewPreviousEfecteKey);
        mocked.getSavePreviousKeyInfos().expectedPropertyReceived("newILoqSecurityAccessIds",
                fakeNewILoqSecurityAccessIds);

        template.send(iLoqControllerEndpoint, ex);

        mocked.getSavePreviousKeyInfos().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldResetTheEfecteKeyProcessorCacheAtTheEndOfTheControllerRun() throws Exception {
        Exchange ex = testUtils.createExchange();

        EfecteEntity efecteEntity = new EfecteEntity("123456");
        EfecteEntitySet efecteEntitySet = new EfecteEntitySet();
        efecteEntitySet.setEntities(List.of(efecteEntity));

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        mocked.getListILoqKeys()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(new ILoqKeyResponse())));

        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.setProperty("shouldCreateEfecteKey", true);
            exchange.setProperty("payload", efecteEntitySet);
            return null;
        }).when(efecteKeyProcessor).buildEfecteKey(any(Exchange.class));

        AtomicLong resetCacheCallCount = new AtomicLong();

        mocked.getProcessEfecteRequest().whenAnyExchangeReceived(e -> {
            long callCount = Mockito.mockingDetails(efecteKeyProcessor).getInvocations().stream()
                    .filter(invocation -> invocation.getMethod().getName().equals("resetCache"))
                    .count();
            resetCacheCallCount.set(callCount);
        });

        mocked.getGetEfecteEntity()
                .whenAnyExchangeReceived(exchange -> exchange.getIn()
                        .setBody(List.of(
                                new EfecteEntityBuilder()
                                        .withDefaults(EnumEfecteTemplate.KEY).build())));

        verifyNoInteractions(efecteKeyProcessor);

        template.send(iLoqControllerEndpoint, ex);

        verify(efecteKeyProcessor).resetCache();
        assertThat(resetCacheCallCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldRemoveAnOngoingILoqSessionsAtTheEndOfControllerRun() throws Exception {
        Exchange ex = testUtils.createExchange();

        EfecteEntity efecteEntity = new EfecteEntity("123456");
        EfecteEntitySet efecteEntitySet = new EfecteEntitySet();
        efecteEntitySet.setEntities(List.of(efecteEntity));

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        mocked.getListILoqKeys()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(new ILoqKeyResponse())));

        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.setProperty("shouldCreateEfecteKey", true);
            exchange.setProperty("payload", efecteEntitySet);
            return null;
        }).when(efecteKeyProcessor).buildEfecteKey(any(Exchange.class));

        AtomicLong resetCacheCallCount = new AtomicLong();

        mocked.getProcessEfecteRequest().whenAnyExchangeReceived(e -> {
            long callCount = Mockito.mockingDetails(efecteKeyProcessor).getInvocations().stream()
                    .filter(invocation -> invocation.getMethod().getName().equals("resetCache"))
                    .count();
            resetCacheCallCount.set(callCount);
        });

        mocked.getGetEfecteEntity()
                .whenAnyExchangeReceived(exchange -> exchange.getIn()
                        .setBody(List.of(
                                new EfecteEntityBuilder()
                                        .withDefaults(EnumEfecteTemplate.KEY).build())));
        when(redis.get(ri.getILoqSessionIdPrefix())).thenReturn("irrelevant but not null");

        mocked.getKillILoqSession().expectedMessageCount(1);
        mocked.getRemoveCurrentILoqSessionRelatedKeys().expectedMessageCount(1);

        template.send(iLoqControllerEndpoint, ex);

        assertThat(resetCacheCallCount.get()).isEqualTo(0);
        assertIsSatisfied(
                mocked.getKillILoqSession(),
                mocked.getRemoveCurrentILoqSessionRelatedKeys());
    }

    @Test
    @DisplayName("direct:iLoqController")
    void testShouldNotRemoveAnILoqSessionsIfItIsAbsent() throws Exception {
        Exchange ex = testUtils.createExchange();

        EfecteEntity efecteEntity = new EfecteEntity("123456");
        EfecteEntitySet efecteEntitySet = new EfecteEntitySet();
        efecteEntitySet.setEntities(List.of(efecteEntity));

        when(configProvider.getConfiguredCustomerCodes()).thenReturn(List.of("irrelevant customer code"));

        mocked.getListILoqKeys()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(new ILoqKeyResponse())));

        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.setProperty("shouldCreateEfecteKey", true);
            exchange.setProperty("payload", efecteEntitySet);
            return null;
        }).when(efecteKeyProcessor).buildEfecteKey(any(Exchange.class));

        AtomicLong resetCacheCallCount = new AtomicLong();

        mocked.getProcessEfecteRequest().whenAnyExchangeReceived(e -> {
            long callCount = Mockito.mockingDetails(efecteKeyProcessor).getInvocations().stream()
                    .filter(invocation -> invocation.getMethod().getName().equals("resetCache"))
                    .count();
            resetCacheCallCount.set(callCount);
        });

        mocked.getGetEfecteEntity()
                .whenAnyExchangeReceived(exchange -> exchange.getIn()
                        .setBody(List.of(
                                new EfecteEntityBuilder()
                                        .withDefaults(EnumEfecteTemplate.KEY).build())));
        when(redis.get(ri.getILoqSessionIdPrefix())).thenReturn(null);

        mocked.getKillILoqSession().expectedMessageCount(0);
        mocked.getRemoveCurrentILoqSessionRelatedKeys().expectedMessageCount(0);

        template.send(iLoqControllerEndpoint, ex);

        assertThat(resetCacheCallCount.get()).isEqualTo(0);
        assertIsSatisfied(
                mocked.getKillILoqSession(),
                mocked.getRemoveCurrentILoqSessionRelatedKeys());
    }

    @Test
    @DisplayName("direct:enrichKeyWithSecurityAccesses")
    void testShoulGetTheSecurityAccessesForTheKey() throws Exception {
        String expectedILoqKeyId = "abc-123";
        Exchange ex = testUtils.createExchange();
        ex.setProperty("iLoqKeyId", expectedILoqKeyId);

        mocked.getGetILoqKeySecurityAccesses().expectedMessageCount(1);
        mocked.getGetILoqKeySecurityAccesses().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);

        template.send(enrichKeyWithSecurityAccessesEndpoint, ex);

        mocked.getGetILoqKeySecurityAccesses().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:enrichKeyWithSecurityAccesses")
    void testShouldValidateTheSecurityAccesses() throws Exception {
        Exchange ex = testUtils.createExchange();
        ex.setProperty("iLoqKeyId", "abc-123");
        String expectedExchangeId = ex.getExchangeId().substring(0, 15);

        Set<ILoqSecurityAccess> expectedILoqSecurityAccesses = Set.of(new ILoqSecurityAccess("1"));

        mocked.getGetILoqKeySecurityAccesses()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(expectedILoqSecurityAccesses));

        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);

        verifyNoInteractions(iLoqKeyProcessor);

        template.send(enrichKeyWithSecurityAccessesEndpoint, ex);

        verify(iLoqKeyProcessor).hasValidSecurityAccesses(captor.capture());

        String resultExchangeId = captor.getValue().getExchangeId().substring(0, 15);
        assertThat(resultExchangeId).isEqualTo(expectedExchangeId);
    }

    @Test
    @DisplayName("direct:enrichKeyWithSecurityAccesses")
    void testShouldBuildTheEnrichedILoqKey() throws Exception {
        String expectedILoqKeyId = "abc-123";
        Exchange ex = testUtils.createExchange();
        ex.setProperty("iLoqKeyId", expectedILoqKeyId);

        AtomicBoolean handled = new AtomicBoolean(false);
        AtomicBoolean throwException = new AtomicBoolean(true);

        mocked.getGetILoqKeySecurityAccesses().whenAnyExchangeReceived(e -> handled.set(true));
        when(iLoqKeyProcessor.hasValidSecurityAccesses(any(Exchange.class))).thenReturn(true);

        doAnswer(i -> {
            if (handled.get()) {
                throwException.set(false);
            }
            return null;
        }).when(iLoqKeyProcessor).buildEnrichedILoqKey(ex);

        verifyNoInteractions(iLoqKeyProcessor);

        template.send(enrichKeyWithSecurityAccessesEndpoint, ex);

        verify(iLoqKeyProcessor).buildEnrichedILoqKey(ex);

        if (throwException.get()) {
            throw new Exception(
                    "Invalid route sequence order");
        }
    }

    @Test
    @DisplayName("direct:enrichKeyWithSecurityAccesses")
    void testShouldStopProcessingTheILoqKeyWhenSecurityAccessValidationFails() throws Exception {
        String expectedILoqKeyId = "abc-123";
        Exchange ex = testUtils.createExchange();
        ex.setProperty("iLoqKeyId", expectedILoqKeyId);

        when(iLoqKeyProcessor.hasValidSecurityAccesses(any(Exchange.class))).thenReturn(false);

        mock.expectedMessageCount(0);

        template.send(enrichKeyWithSecurityAccessesEndpoint, ex);

        mock.assertIsSatisfied();
    }

    private void setDefaultResponse() throws Exception {
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);
            exchange.getIn().setBody(List.of());

            return null;
        }).when(iLoqKeyProcessor).getILoqKeysWithVerifiedRealEstate(any(Exchange.class));
        mocked.getListILoqKeys().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of()));
    }
}
