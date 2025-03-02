package fi.hel.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.ArgumentCaptor;

import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;

import fi.hel.models.EfecteEntity;
import fi.hel.models.ILoqKey;
import fi.hel.models.ILoqKeyImport;
import fi.hel.models.PreviousEfecteKey;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import fi.hel.processors.EfecteKeyProcessor;
import fi.hel.processors.ILoqKeyProcessor;
import fi.hel.processors.ResourceInjector;
import fi.hel.resolvers.ILoqPersonResolver;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
@TestProfile(EfecteKeyCardsHandlerTest.class)
public class EfecteKeyCardsHandlerTest extends CamelQuarkusTestSupport {

    @Inject
    ResourceInjector ri;
    @Inject
    MockEndpointInjector mocked;
    @Inject
    TestUtils testUtils;
    @InjectMock
    EfecteKeyProcessor efecteKeyProcessor;
    @InjectMock
    ILoqKeyProcessor iLoqKeyProcessor;
    @InjectMock
    ILoqPersonResolver iLoqPersonResolver;

    @Override
    protected void doPreSetup() throws Exception {
        super.doPostSetup();
        testConfiguration().withUseRouteBuilder(false);
    }

    private String efecteKeyCardsHandlerEndpoint = "direct:efecteKeyCardsHandler";

    @Test
    @DisplayName("direct:efecteKeyCardsHandler")
    void testShouldValidateThePayload() throws Exception {
        Exchange ex = testUtils.createExchange(createInput());

        String expectedExchangeId = ex.getExchangeId().substring(0, 15);
        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);

        verifyNoInteractions(efecteKeyProcessor);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        verify(efecteKeyProcessor).isValidated(captor.capture());

        String resultExchangeId = captor.getValue().getExchangeId().substring(0, 15);

        assertThat(resultExchangeId).isEqualTo(expectedExchangeId);
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler")
    void testShouldTransformThePayloadToPropertyValuesBeforeValidating() throws Exception {
        String expectedEntityId = "1234";
        String expectedEfecteId = "KEY-0001";
        EfecteEntity expectedEfecteEntity = new EfecteEntityBuilder()
                .withId(expectedEntityId)
                .withKeyEfecteId(expectedEfecteId)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        Exchange ex = testUtils.createExchange(List.of(expectedEfecteEntity));

        AtomicBoolean throwException = new AtomicBoolean(false);

        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            try {
                assertThat(exchange.getProperty("efecteKey")).isEqualTo(expectedEfecteEntity);
                assertThat(exchange.getProperty("efecteKeyEntityId")).isEqualTo(expectedEntityId);
                assertThat(exchange.getProperty("efecteKeyEfecteId")).isEqualTo(expectedEfecteId);
            } catch (AssertionError e) {
                throwException.set(true);
            }

            return true;
        }).when(efecteKeyProcessor).isValidated(any(Exchange.class));

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        if (throwException.get()) {
            throw new Exception(
                    "Assertion failed");
        }
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler")
    void testShouldNotProcessANonvalidatedKey() throws Exception {
        Exchange ex = testUtils.createExchange(createInput());

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(false);

        verifyNoInteractions(iLoqKeyProcessor);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        verify(iLoqKeyProcessor, times(0)).processKey(any(Exchange.class));
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler")
    void testShouldSetTheILoqCredentialsForTheValidatedKey() throws Exception {
        Exchange ex = testUtils.createExchange(createInput());

        String expectedExchangeId = ex.getExchangeId().substring(0, 15);
        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);

        verifyNoInteractions(iLoqKeyProcessor);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        verify(iLoqKeyProcessor).setCurrentILoqCredentials(captor.capture());

        String resultExchangeId = captor.getValue().getExchangeId().substring(0, 15);

        assertThat(resultExchangeId).isEqualTo(expectedExchangeId);
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler")
    void testShouldProcessAValidatedKey() throws Exception {
        Exchange ex = testUtils.createExchange(createInput());

        String expectedExchangeId = ex.getExchangeId().substring(0, 15);
        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);

        verifyNoInteractions(iLoqKeyProcessor);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        verify(iLoqKeyProcessor).processKey(captor.capture());

        String resultExchangeId = captor.getValue().getExchangeId().substring(0, 15);

        assertThat(resultExchangeId).isEqualTo(expectedExchangeId);
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler - create new iLOQ key")
    void testShouldCreateANewILoqKey() throws Exception {
        Exchange ex = testUtils.createExchange(createInput());

        String expectedILoqKeyId = "abc-123";
        ILoqKeyImport expectedILoqPayload = new ILoqKeyImport(new ILoqKey(expectedILoqKeyId));

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            exchange.setProperty("shouldCreateILoqKey", true);
            exchange.setProperty("iLoqKeyId", expectedILoqKeyId);
            exchange.setProperty("iLoqPayload", expectedILoqPayload);

            return null;
        }).when(iLoqKeyProcessor).processKey(any(Exchange.class));

        mocked.getProcessILoqKey().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("{}"));

        mocked.getProcessILoqKey().expectedMessageCount(1);
        mocked.getProcessILoqKey().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);
        mocked.getProcessILoqKey().expectedPropertyReceived("iLoqPayload", expectedILoqPayload);
        mocked.getProcessILoqKey().expectedPropertyReceived("operation", "create");
        mocked.getProcessILoqKey().expectedPropertyReceived("method", "POST");
        mocked.getProcessILoqKey().expectedPropertyReceived("from", EnumDirection.EFECTE);
        mocked.getProcessILoqKey().expectedPropertyReceived("to", EnumDirection.ILOQ);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        mocked.getProcessILoqKey().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler - create new iLOQ key")
    void testShouldConvertTheCreatedILoqKey() throws Exception {
        Exchange ex = testUtils.createExchange(createInput());

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            exchange.setProperty("shouldCreateILoqKey", true);
            exchange.setProperty("newILoqKey", "fake ILoqKeyImport payload");

            return null;
        }).when(iLoqKeyProcessor).processKey(any(Exchange.class));

        String expectedFnKeyId = "1";
        String expectedDescription = "key description";
        String expectedPersonId = "2";
        String expectedRealEstateId = "3";
        String fakeResponse = """
                {
                    "Description": "%s",
                    "FNKey_ID": "%s",
                    "Person_ID": "%s",
                    "RealEstate_ID": "%s",
                    "irrelevant": "fields"
                }
                    """.formatted(
                expectedDescription,
                expectedFnKeyId,
                expectedPersonId,
                expectedRealEstateId);

        ILoqKey expectedILoqKey = new ILoqKey(expectedPersonId, expectedRealEstateId);
        expectedILoqKey.setFnKeyId(expectedFnKeyId);
        expectedILoqKey.setDescription(expectedDescription);

        mocked.getProcessILoqKey().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(fakeResponse));

        mocked.getSaveMappedKeys().expectedMessageCount(1);
        mocked.getSaveMappedKeys().expectedBodiesReceived(expectedILoqKey);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        mocked.getSaveMappedKeys().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler - create new iLOQ key")
    void testShouldSaveTheMappedKeysAfterSuccessfullyCreatingANewILoqKey() throws Exception {
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY-00123";
        String expectedILoqId = "abc-123";
        Exchange ex = testUtils.createExchange(createInput(expectedEntityId, expectedEfecteId));

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            exchange.setProperty("shouldCreateILoqKey", true);
            exchange.setProperty("iLoqKeyId", expectedILoqId);

            return null;
        }).when(iLoqKeyProcessor).processKey(any(Exchange.class));
        mocked.getProcessILoqKey().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("{}"));

        mocked.getSaveMappedKeys().expectedMessageCount(1);
        mocked.getSaveMappedKeys().expectedPropertyReceived("efecteKeyEntityId", expectedEntityId);
        mocked.getSaveMappedKeys().expectedPropertyReceived("efecteKeyEfecteId", expectedEfecteId);
        mocked.getSaveMappedKeys().expectedPropertyReceived("iLoqKeyId", expectedILoqId);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        mocked.getSaveMappedKeys().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler - create new iLOQ key")
    void testShouldUpdateTheCreatedILoqKeyMainZone() throws Exception {
        String expectedILoqId = "abc-123";
        String expectedMainZoneId = "xyz-456";
        Exchange ex = testUtils.createExchange(createInput());

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            exchange.setProperty("shouldCreateILoqKey", true);
            exchange.setProperty("iLoqKeyId", expectedILoqId);
            exchange.setProperty("mainZoneId", expectedMainZoneId);

            return null;
        }).when(iLoqKeyProcessor).processKey(any(Exchange.class));
        mocked.getProcessILoqKey().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("{}"));

        mocked.getUpdateMainZone().expectedMessageCount(1);
        mocked.getUpdateMainZone().expectedPropertyReceived("iLoqKeyId", expectedILoqId);
        mocked.getUpdateMainZone().expectedPropertyReceived("mainZoneId", expectedMainZoneId);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        mocked.getUpdateMainZone().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler - create new iLOQ key")
    void testShouldUpdateTheEfecteKeyAfterCreatingAnILoqKey() throws Exception {
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY-00123";
        String expectedILoqId = "abc-123";
        String expectedEfectePayload = "EfecteEntitySet payload";
        String expectedEfecteEntityType = "key";
        String expectedEfecteOperation = "update";
        String expectedEfectePath = "/dataCardImport.ws";
        String expectedEfecteQuery = "folderCode=avaimet&updateDataCards=true";
        Exchange ex = testUtils.createExchange(createInput(expectedEntityId, expectedEfecteId));

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            exchange.setProperty("shouldCreateILoqKey", true);
            exchange.setProperty("iLoqKeyId", expectedILoqId);
            exchange.setProperty("efectePayload", expectedEfectePayload);

            return null;
        }).when(iLoqKeyProcessor).processKey(any(Exchange.class));
        mocked.getProcessILoqKey().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("{}"));

        mocked.getProcessEfecteRequest().expectedMessageCount(1);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efectePayload", expectedEfectePayload);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteEntityType", expectedEfecteEntityType);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteOperation", expectedEfecteOperation);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("entityId", expectedEntityId);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteId", expectedEfecteId);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("iLoqId", expectedILoqId);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efectePath", expectedEfectePath);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteQuery", expectedEfecteQuery);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        mocked.getProcessEfecteRequest().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler - create new iLOQ key")
    void testShouldUpdateThePreviousEfecteKeyValueAfterUpdatingEfecteKey() throws Exception {
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY-00123";
        Exchange ex = testUtils.createExchange(createInput(expectedEntityId, expectedEfecteId));

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            exchange.setProperty("shouldCreateILoqKey", true);

            return null;
        }).when(iLoqKeyProcessor).processKey(any(Exchange.class));
        mocked.getProcessILoqKey().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("{}"));

        AtomicBoolean efecteKeyHasBeenUpdated = new AtomicBoolean(false);
        AtomicBoolean routeSequenceIsCorrect = new AtomicBoolean(true);

        mocked.getProcessEfecteRequest().whenAnyExchangeReceived(e -> efecteKeyHasBeenUpdated.set(true));

        doAnswer(i -> {
            if (!efecteKeyHasBeenUpdated.get()) {
                routeSequenceIsCorrect.set(false);
            }
            return null;
        }).when(efecteKeyProcessor).updatePreviousEfecteKeyValue(any(Exchange.class));

        verifyNoInteractions(efecteKeyProcessor);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        verify(efecteKeyProcessor).updatePreviousEfecteKeyValue(any(Exchange.class));
        assertThat(routeSequenceIsCorrect.get()).isTrue();
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler - update existing iLOQ key security accesses")
    void testShouldUpdateExistingKeySecurityAccesses() throws Exception {
        Exchange ex = testUtils.createExchange(createInput());

        String iLoqSecurityAccessId1 = "1";
        String iLoqSecurityAccessId2 = "2";
        String expectedILoqKeyId = "abc-123";

        Set<String> expectedILoqSecurityAccessIds = Set.of(
                iLoqSecurityAccessId1, iLoqSecurityAccessId2);

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            exchange.setProperty("shouldUpdateILoqKey", true);
            exchange.setProperty("iLoqKeyId", expectedILoqKeyId);
            exchange.setProperty("newILoqSecurityAccessIds", expectedILoqSecurityAccessIds);

            return null;
        }).when(iLoqKeyProcessor).processKey(any(Exchange.class));

        mocked.getUpdateILoqKeySecurityAccesses().expectedMessageCount(1);
        mocked.getUpdateILoqKeySecurityAccesses().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);
        mocked.getUpdateILoqKeySecurityAccesses().expectedPropertyReceived("newILoqSecurityAccessIds",
                expectedILoqSecurityAccessIds);
        mocked.getProcessILoqKey().expectedMessageCount(0);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        MockEndpoint.assertIsSatisfied(
                mocked.getUpdateILoqKeySecurityAccesses(),
                mocked.getProcessILoqKey());
    }

    // Enable this if API security accesses are being used
    // @Test
    // @DisplayName("direct:efecteKeyCardsHandler - update existing iLOQ key")
    // void testShouldOrderTheKeyAfterUpdating() throws Exception {
    //     Exchange ex = testUtils.createExchange(createInput());

    //     String iLoqSecurityAccessId = "1";
    //     String expectedILoqKeyId = "abc-123";

    //     Set<String> expectedILoqSecurityAccessIds = Set.of(iLoqSecurityAccessId);

    //     when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
    //     doAnswer(i -> {
    //         Exchange exchange = i.getArgument(0);

    //         exchange.setProperty("shouldUpdateILoqKey", true);
    //         exchange.setProperty("iLoqKeyId", expectedILoqKeyId);
    //         exchange.setProperty("newILoqSecurityAccessIds", expectedILoqSecurityAccessIds);

    //         return null;
    //     }).when(iLoqKeyProcessor).processKey(any(Exchange.class));

    //     mocked.getUpdateILoqKeySecurityAccesses()
    //             .whenAnyExchangeReceived(exchange -> exchange.setProperty("foo", "bar"));
    //     mocked.getCanOrderKey().whenAnyExchangeReceived(exchange -> exchange.setProperty("canOrder", true));

    //     mocked.getCanOrderKey().expectedMessageCount(1);
    //     mocked.getCanOrderKey().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);
    //     mocked.getCanOrderKey().expectedPropertyReceived("foo", "bar");
    //     mocked.getOrderKey().expectedMessageCount(1);
    //     mocked.getOrderKey().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);
    //     mocked.getOrderKey().expectedPropertyReceived("foo", "bar");

    //     template.send(efecteKeyCardsHandlerEndpoint, ex);

    //     MockEndpoint.assertIsSatisfied(
    //             mocked.getCanOrderKey(),
    //             mocked.getOrderKey());
    // }

    // Enable this if API security accesses are being used
    // @Test
    // @DisplayName("direct:efecteKeyCardsHandler - update existing iLOQ key")
    // void testShouldNotTryToOrderTheKeyWhenTheMadeChangesCannotBeOrdered_Updating() throws Exception {
    //     Exchange ex = testUtils.createExchange(createInput());

    //     String iLoqSecurityAccessId = "1";
    //     String expectedILoqKeyId = "abc-123";

    //     Set<String> expectedILoqSecurityAccessIds = Set.of(iLoqSecurityAccessId);

    //     when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
    //     doAnswer(i -> {
    //         Exchange exchange = i.getArgument(0);

    //         exchange.setProperty("shouldUpdateILoqKey", true);
    //         exchange.setProperty("iLoqKeyId", expectedILoqKeyId);
    //         exchange.setProperty("newILoqSecurityAccessIds", expectedILoqSecurityAccessIds);

    //         return null;
    //     }).when(iLoqKeyProcessor).processKey(any(Exchange.class));

    //     mocked.getCanOrderKey().whenAnyExchangeReceived(exchange -> exchange.setProperty("canOrder", false));

    //     mocked.getCanOrderKey().expectedMessageCount(1);
    //     mocked.getCanOrderKey().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);
    //     mocked.getOrderKey().expectedMessageCount(0);
    //     mocked.getOrderKey().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);

    //     template.send(efecteKeyCardsHandlerEndpoint, ex);

    //     MockEndpoint.assertIsSatisfied(
    //             mocked.getCanOrderKey(),
    //             mocked.getOrderKey());
    // }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler - update existing iLOQ key expire date")
    void testShouldUpdateExistingKeyExpireDate() throws Exception {
        Exchange ex = testUtils.createExchange(createInput());

        String expectedILoqKeyId = "abc-123";
        ILoqKeyImport expectedILoqPayload = new ILoqKeyImport(new ILoqKey(expectedILoqKeyId));

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            exchange.setProperty("shouldUpdateILoqKey", true);
            exchange.setProperty("iLoqKeyId", expectedILoqKeyId);
            exchange.setProperty("iLoqPayload", expectedILoqPayload);

            return null;
        }).when(iLoqKeyProcessor).processKey(any(Exchange.class));

        mocked.getProcessILoqKey().expectedMessageCount(1);
        mocked.getProcessILoqKey().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);
        mocked.getProcessILoqKey().expectedPropertyReceived("iLoqPayload", expectedILoqPayload);
        mocked.getProcessILoqKey().expectedPropertyReceived("operation", "update");
        mocked.getProcessILoqKey().expectedPropertyReceived("method", "PUT");
        mocked.getProcessILoqKey().expectedPropertyReceived("from", EnumDirection.EFECTE);
        mocked.getProcessILoqKey().expectedPropertyReceived("to", EnumDirection.ILOQ);
        mocked.getUpdateILoqKeySecurityAccesses().expectedMessageCount(0);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        MockEndpoint.assertIsSatisfied(
                mocked.getUpdateILoqKeySecurityAccesses(),
                mocked.getProcessILoqKey());
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler - disable existing iLOQ key")
    void testShouldDisableExistingKeySecurityAccesses() throws Exception {
        Exchange ex = testUtils.createExchange(createInput());

        String expectedILoqKeyId = "abc-123";
        Set<String> expectedILoqSecurityAccessIds = Set.of();

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            exchange.setProperty("shouldDisableILoqKey", true);
            exchange.setProperty("iLoqKeyId", expectedILoqKeyId);
            exchange.setProperty("newILoqSecurityAccessIds", expectedILoqSecurityAccessIds);

            return null;
        }).when(iLoqKeyProcessor).processKey(any(Exchange.class));

        mocked.getUpdateILoqKeySecurityAccesses().expectedMessageCount(1);
        mocked.getUpdateILoqKeySecurityAccesses().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);
        mocked.getUpdateILoqKeySecurityAccesses().expectedPropertyReceived("newILoqSecurityAccessIds",
                expectedILoqSecurityAccessIds);
        mocked.getProcessILoqKey().expectedMessageCount(0);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        MockEndpoint.assertIsSatisfied(
                mocked.getUpdateILoqKeySecurityAccesses(),
                mocked.getProcessILoqKey());
    }

    // Enable this if API security accesses are being used
    // @Test
    // @DisplayName("direct:efecteKeyCardsHandler - disable existing iLOQ key")
    // void testShouldOrderTheKeyAfterDisabling() throws Exception {
    //     Exchange ex = testUtils.createExchange(createInput());

    //     String expectedILoqKeyId = "abc-123";
    //     Set<String> expectedILoqSecurityAccessIds = Set.of();

    //     when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
    //     doAnswer(i -> {
    //         Exchange exchange = i.getArgument(0);

    //         exchange.setProperty("shouldDisableILoqKey", true);
    //         exchange.setProperty("iLoqKeyId", expectedILoqKeyId);
    //         exchange.setProperty("newILoqSecurityAccessIds", expectedILoqSecurityAccessIds);

    //         return null;
    //     }).when(iLoqKeyProcessor).processKey(any(Exchange.class));

    //     mocked.getUpdateILoqKeySecurityAccesses()
    //             .whenAnyExchangeReceived(exchange -> exchange.setProperty("foo", "bar"));
    //     mocked.getCanOrderKey().whenAnyExchangeReceived(exchange -> exchange.setProperty("canOrder", true));

    //     mocked.getCanOrderKey().expectedMessageCount(1);
    //     mocked.getCanOrderKey().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);
    //     mocked.getCanOrderKey().expectedPropertyReceived("foo", "bar");
    //     mocked.getOrderKey().expectedMessageCount(1);
    //     mocked.getOrderKey().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);
    //     mocked.getOrderKey().expectedPropertyReceived("foo", "bar");

    //     template.send(efecteKeyCardsHandlerEndpoint, ex);

    //     MockEndpoint.assertIsSatisfied(
    //             mocked.getCanOrderKey(),
    //             mocked.getOrderKey());
    // }

    // Enable this if API security accesses are being used
    // @Test
    // @DisplayName("direct:efecteKeyCardsHandler - disable existing iLOQ key")
    // void testShouldNotTryToOrderTheKeyWhenTheMadeChangesCannotBeOrdered_Disabling() throws Exception {
    //     Exchange ex = testUtils.createExchange(createInput());

    //     String expectedILoqKeyId = "abc-123";
    //     Set<String> expectedILoqSecurityAccessIds = Set.of();

    //     when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
    //     doAnswer(i -> {
    //         Exchange exchange = i.getArgument(0);

    //         exchange.setProperty("shouldDisableILoqKey", true);
    //         exchange.setProperty("iLoqKeyId", expectedILoqKeyId);
    //         exchange.setProperty("newILoqSecurityAccessIds", expectedILoqSecurityAccessIds);

    //         return null;
    //     }).when(iLoqKeyProcessor).processKey(any(Exchange.class));

    //     mocked.getCanOrderKey().whenAnyExchangeReceived(exchange -> exchange.setProperty("canOrder", false));

    //     mocked.getCanOrderKey().expectedMessageCount(1);
    //     mocked.getCanOrderKey().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);
    //     mocked.getOrderKey().expectedMessageCount(0);
    //     mocked.getOrderKey().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);

    //     template.send(efecteKeyCardsHandlerEndpoint, ex);

    //     MockEndpoint.assertIsSatisfied(
    //             mocked.getCanOrderKey(),
    //             mocked.getOrderKey());
    // }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler - disable existing iLOQ key")
    void testShouldRemoveTheILoqKeyIdenfitierFromEfecteKeyCard() throws Exception {
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY-00123";
        String expectedILoqId = "abc-123";
        String expectedEfectePayload = "EfecteEntitySet payload";
        String expectedEfecteEntityType = "key";
        String expectedEfecteOperation = "update";
        String expectedEfectePath = "/dataCardImport.ws";
        String expectedEfecteQuery = "folderCode=avaimet&updateDataCards=true&removeEmptyValues=true";
        Exchange ex = testUtils.createExchange(createInput(expectedEntityId, expectedEfecteId));

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            exchange.setProperty("shouldDisableILoqKey", true);
            exchange.setProperty("iLoqKeyId", expectedILoqId);
            exchange.setProperty("efectePayload", expectedEfectePayload);

            return null;
        }).when(iLoqKeyProcessor).processKey(any(Exchange.class));

        mocked.getProcessEfecteRequest().expectedMessageCount(1);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efectePayload", expectedEfectePayload);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteEntityType", expectedEfecteEntityType);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteOperation", expectedEfecteOperation);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("entityId", expectedEntityId);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteId", expectedEfecteId);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("iLoqId", expectedILoqId);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efectePath", expectedEfectePath);
        mocked.getProcessEfecteRequest().expectedPropertyReceived("efecteQuery", expectedEfecteQuery);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        mocked.getProcessEfecteRequest().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler - create new iLOQ key")
    void testShouldSaveTheNewPreviousKeyValuesForBothSystemsToRedis() throws Exception {
        Exchange ex = testUtils.createExchange(createInput());

        PreviousEfecteKey fakeNewPreviousEfecteKey = new PreviousEfecteKey();
        Set<String> fakeNewILoqSecurityAccessIds = Set.of("bar");

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);
        doAnswer(i -> {
            Exchange exchange = i.getArgument(0);

            exchange.setProperty("newPreviousEfecteKey", fakeNewPreviousEfecteKey);
            exchange.setProperty("newILoqSecurityAccessIds", fakeNewILoqSecurityAccessIds);

            return null;
        }).when(iLoqKeyProcessor).processKey(any(Exchange.class));

        mocked.getSavePreviousKeyInfos().expectedMessageCount(1);
        mocked.getSavePreviousKeyInfos().expectedPropertyReceived("newPreviousEfecteKey", fakeNewPreviousEfecteKey);
        mocked.getSavePreviousKeyInfos().expectedPropertyReceived("newILoqSecurityAccessIds",
                fakeNewILoqSecurityAccessIds);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        mocked.getSavePreviousKeyInfos().assertIsSatisfied();
    }

    @Test
    @DisplayName("direct:efecteKeyCardsHandler")
    void testShouldResetTheILoqPersonResolverCacheAtTheEndOfTheControllerRun() throws Exception {
        Exchange ex = testUtils.createExchange(createInput());

        when(efecteKeyProcessor.isValidated(any(Exchange.class))).thenReturn(true);

        verifyNoInteractions(iLoqPersonResolver);

        template.send(efecteKeyCardsHandlerEndpoint, ex);

        verify(iLoqPersonResolver).resetCache();
    }

    private List<EfecteEntity> createInput() throws Exception {
        return createInput(null, null);
    }

    private List<EfecteEntity> createInput(String entityId, String efecteId) throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withId(entityId != null ? entityId : null)
                .withKeyEfecteId(efecteId != null ? efecteId : null)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        return List.of(efecteEntity);
    }
}
