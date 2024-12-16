package fi.hel.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.configurations.ConfigProvider;
import fi.hel.mappers.ILoqKeyMapper;
import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntitySet;
import fi.hel.models.EfecteReference;
import fi.hel.models.EnrichedILoqKey;
import fi.hel.models.ILoqKey;
import fi.hel.models.ILoqKeyImport;
import fi.hel.models.ILoqKeyResponse;
import fi.hel.models.ILoqSecurityAccess;
import fi.hel.models.PreviousEfecteKey;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteKeyState;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("unchecked")
public class ILoqKeyProcessorTest extends CamelQuarkusTestSupport {

    @Inject
    ResourceInjector ri;
    @Inject
    ILoqKeyProcessor iLoqKeyProcessor;
    @Inject
    TestUtils testUtils;
    @InjectMock
    Redis redis;
    @InjectMock
    ILoqKeyMapper iLoqKeyMapper;
    @InjectMock
    ConfigProvider configProvider;
    @InjectMock
    AuditExceptionProcessor auditExceptionProcessor;
    @InjectMock
    Helper helper;

    @Override
    protected void doPreSetup() throws Exception {
        super.doPostSetup();
        testConfiguration().withUseRouteBuilder(false);
    }

    @Test
    @DisplayName("processKey - key IS NOT previously mapped - valid state")
    void testShouldNotThrowAnExceptionThenTheExternalIdIsMissingFromAnEfecteKey() throws Exception {
        EfecteEntity efecteKey = new EfecteEntityBuilder()
                .withKeyEfecteId("irrelevant")
                .withState(EnumEfecteKeyState.AKTIIVINEN)
                .withSecurityAccesses(new EfecteReference())
                .withValidityDate("irrelevant")
                .build();

        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteKey);

        iLoqKeyProcessor.processKey(ex);

        assertThatNoException();
    }

    @Test
    @DisplayName("processKey - key IS NOT previously mapped - valid state")
    void testShouldBuildANewILoqKeyWhenTheKeyWithValidStateIsNotMapped() throws Exception {
        List<EnumEfecteKeyState> states = List.of(
                EnumEfecteKeyState.ODOTTAA_AKTIVOINTIA);
        String securityAccessEntityId = "12345";
        String efecteId = "KEY-000123";
        String validityDate = "foobar";

        for (EnumEfecteKeyState state : states) {
            EfecteEntity expectedEfecteEntity = new EfecteEntityBuilder()
                    .withKeyEfecteId(efecteId)
                    .withExternalId(null)
                    .withState(state)
                    .withValidityDate(validityDate)
                    .withSecurityAccesses(new EfecteReference(securityAccessEntityId))
                    .withDefaults(EnumEfecteTemplate.KEY)
                    .build();

            Exchange ex = testUtils.createExchange();
            ex.setProperty("efecteKey", expectedEfecteEntity);

            String expectedILoqKeyId = "abc-123";
            String iLoqSecurityAccessId = "xyz-456";
            Set<String> expectedNewILoqSecurityAccessIds = Set.of(iLoqSecurityAccessId);
            ILoqKey iLoqKey = new ILoqKey(expectedILoqKeyId);
            ILoqKeyImport expectedNewILoqKey = new ILoqKeyImport(iLoqKey);
            expectedNewILoqKey.setSecurityAccessIds(List.of(iLoqSecurityAccessId));
            Set<String> newEfecteSecurityAccesses = Set.of(securityAccessEntityId);
            PreviousEfecteKey expectedNewPreviousEfecteKey = new PreviousEfecteKey(
                    state.getName(), newEfecteSecurityAccesses, validityDate);
            EfecteEntitySet expectedEfectePayload = new EfecteEntitySet(new EfecteEntityBuilder()
                    .withTemplate(EnumEfecteTemplate.KEY.getCode())
                    .withKeyEfecteId(efecteId)
                    .withExternalId(expectedILoqKeyId)
                    .withState(EnumEfecteKeyState.AKTIIVINEN)
                    .build());

            when(iLoqKeyMapper.buildNewILoqKey(expectedEfecteEntity)).thenReturn(expectedNewILoqKey);

            iLoqKeyProcessor.processKey(ex);

            ILoqKeyImport iLoqPayload = ex.getProperty("iLoqPayload", ILoqKeyImport.class);
            String iLoqKeyId = ex.getProperty("iLoqKeyId", String.class);
            boolean shouldCreateILoqKey = ex.getProperty("shouldCreateILoqKey", boolean.class);
            boolean shouldUpdateILoqKey = ex.getProperty("shouldUpdateILoqKey", boolean.class);
            Set<String> newILoqSecurityAccessIds = ex.getProperty("newILoqSecurityAccessIds", Set.class);
            PreviousEfecteKey newPreviousEfecteKey = ex.getProperty("newPreviousEfecteKey",
                    PreviousEfecteKey.class);
            EfecteEntitySet efectePayload = ex.getProperty("efectePayload", EfecteEntitySet.class);

            verify(iLoqKeyMapper).buildNewILoqKey(expectedEfecteEntity);
            assertThat(iLoqPayload).isEqualTo(expectedNewILoqKey);
            assertThat(iLoqKeyId).isEqualTo(expectedILoqKeyId);
            assertThat(shouldCreateILoqKey).isTrue();
            assertThat(shouldUpdateILoqKey).isFalse();
            assertThat(newILoqSecurityAccessIds).isEqualTo(expectedNewILoqSecurityAccessIds);
            assertThat(newPreviousEfecteKey).isEqualTo(expectedNewPreviousEfecteKey);
            assertThat(efectePayload).isEqualTo(expectedEfectePayload);

            reset(iLoqKeyMapper);
        }
    }

    @Test
    @DisplayName("processKey - key IS NOT previously mapped - invalid state")
    void testShouldThrowAnAuditExceptionWhenTheKeyWithInvalidStateIsNotMapped() throws Exception {
        List<EnumEfecteKeyState> states = List.of(
                EnumEfecteKeyState.AKTIIVINEN,
                EnumEfecteKeyState.POISTETTU);
        String entityId = "12345";
        String efecteId = "KEY-0001";
        String iLoqKeyId = null;

        for (EnumEfecteKeyState state : states) {
            EfecteEntity expectedEfecteEntity = new EfecteEntityBuilder()
                    .withId(entityId)
                    .withKeyEfecteId(efecteId)
                    .withExternalId(null)
                    .withState(state)
                    .withDefaults(EnumEfecteTemplate.KEY)
                    .build();
            Exchange ex = testUtils.createExchange();
            ex.setProperty("efecteKey", expectedEfecteEntity);

            String expectedAuditMessage = "Efecte key state is '" + state + "'. Cannot create an iLOQ key.";

            verifyNoInteractions(auditExceptionProcessor);

            iLoqKeyProcessor.processKey(ex);

            verify(auditExceptionProcessor).throwAuditException(
                    EnumDirection.EFECTE,
                    EnumDirection.ILOQ,
                    entityId,
                    efecteId,
                    iLoqKeyId,
                    expectedAuditMessage);

            reset(auditExceptionProcessor);
        }
    }

    @Test
    @DisplayName("processKey - key IS previously mapped")
    void testShouldGetThePreviousEfecteKeyFromRedis() throws Exception {
        String efecteId = "1234";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withKeyEfecteId(efecteId)
                .withExternalId("irrelevant, but not null")
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        when(redis.get(anyString())).thenReturn("previousKey json");
        when(helper.writeAsPojo(any(), any())).thenReturn(new PreviousEfecteKey(""));

        String expectedPrefix = ri.getPreviousKeyEfectePrefix() + efecteId;

        verifyNoInteractions(redis);

        iLoqKeyProcessor.processKey(ex);

        verify(redis).get(expectedPrefix);
    }

    @Test
    @DisplayName("processKey - key IS previously mapped - key HAS NOT changed")
    void testShouldNotBuildAnUpdateWhenTheEfecteKeyHasNotChanged() throws Exception {
        String securityAccessEntityId = "foo";
        EnumEfecteKeyState keyState = EnumEfecteKeyState.AKTIIVINEN;
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withState(keyState)
                .withSecurityAccesses(new EfecteReference(securityAccessEntityId))
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        PreviousEfecteKey previousEfecteKey = new PreviousEfecteKey(
                keyState.getName(), Set.of(securityAccessEntityId), "20.05.2025 00:00");
        String previousEfecteKeyJson = testUtils.writeAsJson(previousEfecteKey);

        when(redis.get(anyString())).thenReturn(previousEfecteKeyJson);
        when(helper.writeAsPojo(previousEfecteKeyJson, PreviousEfecteKey.class)).thenReturn(previousEfecteKey);

        iLoqKeyProcessor.processKey(ex);

        ILoqKeyImport iLoqPayload = ex.getProperty("iLoqPayload", ILoqKeyImport.class);
        boolean shouldCreateILoqKey = ex.getProperty("shouldCreateILoqKey", boolean.class);
        boolean shouldUpdateILoqKey = ex.getProperty("shouldUpdateILoqKey", boolean.class);
        Set<String> newILoqSecurityAccessIds = ex.getProperty("newILoqSecurityAccessIds", Set.class);

        assertThat(iLoqPayload).isNull();
        assertThat(shouldCreateILoqKey).isFalse();
        assertThat(shouldUpdateILoqKey).isFalse();
        assertThat(newILoqSecurityAccessIds).isNull();
        verifyNoInteractions(iLoqKeyMapper);
    }

    @Test
    @DisplayName("processKey - key IS previously mapped as 'Aktiivinen' - key HAS changed (securityAccesses)")
    void testShouldBuildAnUpdateWhenTheEfecteKeySecurityAccessesHasChanged_AKTIIVINEN() throws Exception {
        String securityAccessEntityId1 = "foo";
        String securityAccessEntityId2 = "bar";
        String securityAccessEntityId3 = "baz";
        EnumEfecteKeyState keyState = EnumEfecteKeyState.AKTIIVINEN;
        String expectedILoqKeyId = "abc-123";
        String validityDate = "foo";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withState(keyState)
                .withExternalId(expectedILoqKeyId)
                .withValidityDate(validityDate)
                .withSecurityAccesses(
                        new EfecteReference(securityAccessEntityId1),
                        new EfecteReference(securityAccessEntityId2))
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        Set<String> expectedPreviousEfecteSecurityAccessIds = Set.of(
                securityAccessEntityId1, securityAccessEntityId3);
        PreviousEfecteKey previousEfecteKey = new PreviousEfecteKey(
                keyState.getName(), expectedPreviousEfecteSecurityAccessIds, validityDate);
        String previousEfecteKeyJson = testUtils.writeAsJson(previousEfecteKey);

        Set<String> expectedNewEfecteSecurityAccesses = Set.of(securityAccessEntityId1,
                securityAccessEntityId2);
        Set<String> expectedILoqSecurityAccessIds = Set.of("1,", "2");
        PreviousEfecteKey expectedNewPreviousEfecteKey = new PreviousEfecteKey(
                keyState.getName(), expectedNewEfecteSecurityAccesses, validityDate);

        when(redis.get(anyString())).thenReturn(previousEfecteKeyJson);
        when(helper.writeAsPojo(previousEfecteKeyJson, PreviousEfecteKey.class)).thenReturn(previousEfecteKey);
        when(iLoqKeyMapper.buildUpdatedILoqSecurityAccesses(any())).thenReturn(expectedILoqSecurityAccessIds);

        verifyNoInteractions(iLoqKeyMapper);

        iLoqKeyProcessor.processKey(ex);

        ILoqKeyImport iLoqPayload = ex.getProperty("iLoqPayload", ILoqKeyImport.class);
        String iLoqKeyId = ex.getProperty("iLoqKeyId", String.class);
        boolean shouldCreateILoqKey = ex.getProperty("shouldCreateILoqKey", boolean.class);
        boolean shouldUpdateILoqKey = ex.getProperty("shouldUpdateILoqKey", boolean.class);
        Set<String> newILoqSecurityAccessIds = ex.getProperty("newILoqSecurityAccessIds", Set.class);
        PreviousEfecteKey newPreviousEfecteKey = ex.getProperty("newPreviousEfecteKey",
                PreviousEfecteKey.class);

        assertThat(iLoqPayload).isNull();
        assertThat(iLoqKeyId).isEqualTo(expectedILoqKeyId);
        assertThat(shouldCreateILoqKey).isFalse();
        assertThat(shouldUpdateILoqKey).isTrue();
        assertThat(newPreviousEfecteKey).isEqualTo(expectedNewPreviousEfecteKey);
        assertThat(newILoqSecurityAccessIds).isEqualTo(expectedILoqSecurityAccessIds);
        verify(iLoqKeyMapper).buildUpdatedILoqSecurityAccesses(expectedNewEfecteSecurityAccesses);
    }

    @Test
    @DisplayName("processKey - key IS previously mapped as 'Aktiivinen' - key HAS changed (validity date)")
    void testShouldBuildAnUpdateWhenTheEfecteValidityDateHasChanged() throws Exception {
        EnumEfecteKeyState keyState = EnumEfecteKeyState.AKTIIVINEN;
        String expectedILoqKeyId = "abc-123";
        String newValidityDate = "foo";
        String securityAccessEntityId = "1";
        EfecteEntity expectedEfecteEntity = new EfecteEntityBuilder()
                .withState(keyState)
                .withExternalId(expectedILoqKeyId)
                .withValidityDate(newValidityDate)
                .withSecurityAccesses(
                        new EfecteReference(securityAccessEntityId))
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", expectedEfecteEntity);

        Set<String> oldSecurityAccessIds = Set.of(securityAccessEntityId);
        String oldValidityDate = "bar";
        PreviousEfecteKey previousEfecteKey = new PreviousEfecteKey(
                keyState.getName(), oldSecurityAccessIds, oldValidityDate);
        String previousEfecteKeyJson = testUtils.writeAsJson(previousEfecteKey);

        PreviousEfecteKey expectedNewPreviousEfecteKey = new PreviousEfecteKey(
                keyState.getName(), oldSecurityAccessIds, newValidityDate);
        ILoqKeyImport expectedILoqPayload = new ILoqKeyImport(new ILoqKey(expectedILoqKeyId));

        when(redis.get(anyString())).thenReturn(previousEfecteKeyJson);
        when(helper.writeAsPojo(previousEfecteKeyJson, PreviousEfecteKey.class)).thenReturn(previousEfecteKey);
        when(iLoqKeyMapper.buildUpdatedILoqKey(any())).thenReturn(expectedILoqPayload);

        verifyNoInteractions(iLoqKeyMapper);

        iLoqKeyProcessor.processKey(ex);

        ILoqKeyImport iLoqPayload = ex.getProperty("iLoqPayload", ILoqKeyImport.class);
        String iLoqKeyId = ex.getProperty("iLoqKeyId", String.class);
        boolean shouldCreateILoqKey = ex.getProperty("shouldCreateILoqKey", boolean.class);
        boolean shouldUpdateILoqKey = ex.getProperty("shouldUpdateILoqKey", boolean.class);
        Set<String> newILoqSecurityAccessIds = ex.getProperty("newILoqSecurityAccessIds", Set.class);
        PreviousEfecteKey newPreviousEfecteKey = ex.getProperty("newPreviousEfecteKey",
                PreviousEfecteKey.class);

        assertThat(iLoqPayload).isEqualTo(expectedILoqPayload);
        assertThat(iLoqKeyId).isEqualTo(expectedILoqKeyId);
        assertThat(shouldCreateILoqKey).isFalse();
        assertThat(shouldUpdateILoqKey).isTrue();
        assertThat(newPreviousEfecteKey).isEqualTo(expectedNewPreviousEfecteKey);
        assertThat(newILoqSecurityAccessIds).isNull();
        verify(iLoqKeyMapper).buildUpdatedILoqKey(expectedEfecteEntity);
    }

    @Test
    @DisplayName("processKey - key IS previously mapped as 'Aktiivinen' - key HAS changed (state=Passiivinen)")
    void testShouldRemoveILoqSecurityAccessesWhenTheEfecteKeyHasBeenPassived() throws Exception {
        String efecteId = "KEY-000123";
        String securityAccessEntityId = "foo";
        String previousKeyState = EnumEfecteKeyState.AKTIIVINEN.getName();
        EnumEfecteKeyState keyState = EnumEfecteKeyState.PASSIIVINEN;
        String expectedILoqKeyId = "abc-123";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withState(keyState)
                .withExternalId(expectedILoqKeyId)
                .withKeyEfecteId(efecteId)
                .withSecurityAccesses(new EfecteReference(securityAccessEntityId))
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        PreviousEfecteKey previousEfecteKey = new PreviousEfecteKey(
                previousKeyState, Set.of(securityAccessEntityId));
        String previousEfecteKeyJson = testUtils.writeAsJson(previousEfecteKey);

        EfecteEntitySet expectedEfectePayload = new EfecteEntitySet(new EfecteEntityBuilder()
                .withTemplate(EnumEfecteTemplate.KEY.getCode())
                .withKeyEfecteId(efecteId)
                .withExternalId(null)
                .build());

        when(redis.get(anyString())).thenReturn(previousEfecteKeyJson);
        when(helper.writeAsPojo(previousEfecteKeyJson, PreviousEfecteKey.class)).thenReturn(previousEfecteKey);

        iLoqKeyProcessor.processKey(ex);

        ILoqKeyImport iLoqPayload = ex.getProperty("iLoqPayload", ILoqKeyImport.class);
        String iLoqKeyId = ex.getProperty("iLoqKeyId", String.class);
        boolean shouldCreateILoqKey = ex.getProperty("shouldCreateILoqKey", boolean.class);
        boolean shouldUpdateILoqKey = ex.getProperty("shouldUpdateILoqKey", boolean.class);
        boolean shouldDisableILoqKey = ex.getProperty("shouldDisableILoqKey", boolean.class);
        Set<String> newILoqSecurityAccessIds = ex.getProperty("newILoqSecurityAccessIds", Set.class);
        PreviousEfecteKey newPreviousEfecteKey = ex.getProperty("newPreviousEfecteKey",
                PreviousEfecteKey.class);
        EfecteEntitySet efectePayload = ex.getProperty("efectePayload", EfecteEntitySet.class);

        assertThat(iLoqPayload).isNull();
        assertThat(iLoqKeyId).isEqualTo(expectedILoqKeyId);
        assertThat(shouldCreateILoqKey).isFalse();
        assertThat(shouldUpdateILoqKey).isFalse();
        assertThat(shouldDisableILoqKey).isTrue();
        assertThat(newPreviousEfecteKey).isNull();
        assertThat(newILoqSecurityAccessIds).isEmpty();
        verifyNoInteractions(iLoqKeyMapper);
        assertThat(efectePayload).isEqualTo(expectedEfectePayload);
    }

    @Test
    @DisplayName("processKey - key IS previously mapped as 'Odottaa aktivointia' - key HAS changed (state=Aktiivinen)")
    void testShouldUpdateThePreviousEfecteKeyInfo() throws Exception {
        String efecteEntityId = "12345";
        String securityAccessEntityId = "foo";
        EnumEfecteKeyState newKeyState = EnumEfecteKeyState.AKTIIVINEN;
        String expectedILoqKeyId = "abc-123";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withId(efecteEntityId)
                .withExternalId(expectedILoqKeyId)
                .withSecurityAccesses(new EfecteReference(securityAccessEntityId))
                .withState(newKeyState)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        PreviousEfecteKey previousEfecteKey = new PreviousEfecteKey(
                "Odottaa aktivointia", Set.of(securityAccessEntityId));
        String previousEfecteKeyJson = testUtils.writeAsJson(previousEfecteKey);

        Set<String> expectedNewSecurityAccesses = Set.of(securityAccessEntityId);
        PreviousEfecteKey expectedNewPreviousEfecteKey = new PreviousEfecteKey(
                newKeyState.getName(), expectedNewSecurityAccesses);

        when(redis.get(anyString())).thenReturn(previousEfecteKeyJson);
        when(helper.writeAsPojo(previousEfecteKeyJson, PreviousEfecteKey.class)).thenReturn(previousEfecteKey);

        iLoqKeyProcessor.processKey(ex);

        ILoqKeyImport iLoqPayload = ex.getProperty("iLoqPayload", ILoqKeyImport.class);
        String iLoqKeyId = ex.getProperty("iLoqKeyId", String.class);
        boolean shouldCreateILoqKey = ex.getProperty("shouldCreateILoqKey", boolean.class);
        boolean shouldUpdateILoqKey = ex.getProperty("shouldUpdateILoqKey", boolean.class);
        Set<String> newILoqSecurityAccessIds = ex.getProperty("newILoqSecurityAccessIds", Set.class);
        PreviousEfecteKey newPreviousEfecteKey = ex.getProperty("newPreviousEfecteKey",
                PreviousEfecteKey.class);

        assertThat(iLoqPayload).isNull();
        assertThat(iLoqKeyId).isEqualTo(expectedILoqKeyId);
        assertThat(shouldCreateILoqKey).isFalse();
        assertThat(shouldUpdateILoqKey).isFalse();
        assertThat(newPreviousEfecteKey).isEqualTo(expectedNewPreviousEfecteKey);
        assertThat(newILoqSecurityAccessIds).isNull();
        verifyNoInteractions(iLoqKeyMapper);
    }

    @Test
    @DisplayName("processKey - key IS previously mapped - previous key not found")
    void testShouldThrowAnAuditMessageWhenAMappedKeyDoesNotHavePreviousKeyInfo() throws Exception {
        String entityId = "12345";
        String efecteId = "KEY-0001";
        String expectedILoqKeyId = "abc-123";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withId(entityId)
                .withKeyEfecteId(efecteId)
                .withExternalId(expectedILoqKeyId)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        String expectedAuditMessage = "The id has been mapped, but the previous Efecte key info has been deleted from Redis. Deleting also the mapped ids results in creating a new iLOQ key.";

        when(redis.get(anyString())).thenReturn(null);

        verifyNoInteractions(auditExceptionProcessor);

        iLoqKeyProcessor.processKey(ex);

        verify(auditExceptionProcessor).throwAuditException(
                EnumDirection.EFECTE,
                EnumDirection.ILOQ,
                entityId,
                efecteId,
                expectedILoqKeyId,
                expectedAuditMessage);
    }

    @Test
    @DisplayName("setCurrentILoqCredentials")
    void testShouldResolveThCurrenteCustomerCode() throws Exception {
        String expectedEfecteAddressEntityId = "12345";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withStreetAddress(expectedEfecteAddressEntityId, "irrelevant street name")
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        verifyNoInteractions(configProvider);

        iLoqKeyProcessor.setCurrentILoqCredentials(ex);

        verify(configProvider).getILoqCustomerCodeByEfecteAddress(expectedEfecteAddressEntityId);
    }

    @Test
    @DisplayName("setCurrentILoqCredentials")
    void testShouldGetThePreviouslySetCustomerCode() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        verifyNoInteractions(redis);

        iLoqKeyProcessor.setCurrentILoqCredentials(ex);

        verify(redis).get(ri.getILoqCurrentCustomerCodePrefix());
    }

    @Test
    @DisplayName("setCurrentILoqCredentials")
    void testShouldSaveTheCustomerCodeHadChangedStatusWhenThereIsNoExistingCustomerCode() throws Exception {
        String expectedStatus = "true";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        verifyNoInteractions(redis);

        iLoqKeyProcessor.setCurrentILoqCredentials(ex);

        verify(redis).set(ri.getILoqCurrentCustomerCodeHasChangedPrefix(), expectedStatus);
    }

    @Test
    @DisplayName("setCurrentILoqCredentials")
    void testShouldSaveTheCustomerCodeHadChangedStatusWhenThereIsAnExistingNonequalCustomerCode() throws Exception {
        String expectedStatus = "true";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        when(redis.get(ri.getILoqCurrentCustomerCodePrefix())).thenReturn("some else customer code");
        when(configProvider.getILoqCustomerCodeByEfecteAddress(anyString())).thenReturn("foo");

        verifyNoInteractions(redis);

        iLoqKeyProcessor.setCurrentILoqCredentials(ex);

        verify(redis).set(ri.getILoqCurrentCustomerCodeHasChangedPrefix(), expectedStatus);
    }

    @Test
    @DisplayName("setCurrentILoqCredentials")
    void testShouldSaveTheCurrentCredentialsToRedisWhenThePreviousCredentialsDoesNotMatchTheNewOnes()
            throws Exception {
        String expectedCustomerCode = "customerCode1";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        when(redis.get(ri.getILoqCurrentCustomerCodePrefix())).thenReturn("some else customer code");
        when(configProvider.getILoqCustomerCodeByEfecteAddress(anyString())).thenReturn(expectedCustomerCode);

        verifyNoInteractions(configProvider);

        iLoqKeyProcessor.setCurrentILoqCredentials(ex);

        verify(configProvider).saveCurrentCredentialsToRedis(expectedCustomerCode);
    }

    @Test
    @DisplayName("setCurrentILoqCredentials")
    void testShouldSaveTheCustomerCodeHadChangedStatusWhenThereIsAnExistingEqualCustomerCode() throws Exception {
        String existingCustomerCode = "customerCode1";
        String expectedStatus = "false";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        when(redis.get(ri.getILoqCurrentCustomerCodePrefix())).thenReturn(existingCustomerCode);
        when(configProvider.getILoqCustomerCodeByEfecteAddress(anyString())).thenReturn(existingCustomerCode);

        verifyNoInteractions(redis);

        iLoqKeyProcessor.setCurrentILoqCredentials(ex);

        verify(redis).set(ri.getILoqCurrentCustomerCodeHasChangedPrefix(), expectedStatus);
    }

    ////////////////////
    // iLOQ -> Efecte //
    ////////////////////

    @Test
    @DisplayName("buildEnrichedILoqKey")
    void testShouldEnrichILoqKeyWithSecurityAccesses() throws Exception {
        String securityAccessName1 = "foo";
        String securityAccessName2 = "bar";
        String securityAccessId1 = "abc-foo";
        String securityAccessId2 = "abc-bar";
        ILoqSecurityAccess iLoqSecurityAccess1 = new ILoqSecurityAccess(
                securityAccessName1, "irrelevant", securityAccessId1);
        ILoqSecurityAccess iLoqSecurityAccess2 = new ILoqSecurityAccess(
                securityAccessName2, "irrelevant", securityAccessId2);
        List<ILoqSecurityAccess> securityAccesses = List.of(iLoqSecurityAccess1, iLoqSecurityAccess2);

        String expectedKeyId = "keyId";
        String expectedPersonId = "personId";
        String expectedRealEstateId = "realEstateId";
        String expectedInfoText = "efecteId";
        Integer expectedState = 1;
        ILoqKeyResponse iLoqKeyResponse = new ILoqKeyResponse(expectedKeyId);
        iLoqKeyResponse.setPersonId(expectedPersonId);
        iLoqKeyResponse.setRealEstateId(expectedRealEstateId);
        iLoqKeyResponse.setInfoText(expectedInfoText);
        iLoqKeyResponse.setState(expectedState);

        Exchange ex = testUtils.createExchange(securityAccesses);
        ex.setProperty("currentILoqKey", iLoqKeyResponse);

        iLoqKeyProcessor.buildEnrichedILoqKey(ex);

        Object result = ex.getProperty("enrichedILoqKey");

        assertThat(result).isInstanceOf(EnrichedILoqKey.class);

        EnrichedILoqKey enrichedKey = (EnrichedILoqKey) result;

        assertThat(enrichedKey.getFnKeyId()).isEqualTo(expectedKeyId);
        assertThat(enrichedKey.getPersonId()).isEqualTo(expectedPersonId);
        assertThat(enrichedKey.getRealEstateId()).isEqualTo(expectedRealEstateId);
        assertThat(enrichedKey.getInfoText()).isEqualTo(expectedInfoText);
        assertThat(enrichedKey.getState()).isEqualTo(expectedState);
        assertThat(enrichedKey.getSecurityAccesses()).containsExactlyInAnyOrder(
                iLoqSecurityAccess1, iLoqSecurityAccess2);
    }

    @Test
    @DisplayName("getVerifiedAndSortedILoqKeys")
    void testShouldGetTheConfiguredRealEstateIds() throws Exception {
        Exchange ex = testUtils.createExchange(List.of());

        verifyNoInteractions(configProvider);

        iLoqKeyProcessor.getILoqKeysWithVerifiedRealEstate(ex);

        verify(configProvider).getConfiguredILoqRealEstateIds();
    }

    @Test
    @DisplayName("getVerifiedAndSortedILoqKeys")
    void testShouldReturnOnlyKeysWithVerifiedRealEstate() throws Exception {
        String verifiedRealEstateId1 = "abc-123";
        String unverifiedRealEstateId2 = "foobar";
        String verifiedRealEstateId3 = "xyz-456";
        ILoqKeyResponse key1 = new ILoqKeyResponse();
        key1.setRealEstateId(verifiedRealEstateId1);
        ILoqKeyResponse key2 = new ILoqKeyResponse();
        key2.setRealEstateId(unverifiedRealEstateId2);
        ILoqKeyResponse key3 = new ILoqKeyResponse();
        key3.setRealEstateId(verifiedRealEstateId3);
        List<ILoqKeyResponse> allKeys = List.of(key1, key2, key3);

        Exchange ex = testUtils.createExchange(allKeys);

        when(configProvider.getConfiguredILoqRealEstateIds())
                .thenReturn(List.of(verifiedRealEstateId1, verifiedRealEstateId3));

        iLoqKeyProcessor.getILoqKeysWithVerifiedRealEstate(ex);

        List<ILoqKeyResponse> result = ex.getIn().getBody(List.class);

        assertThat(result).containsExactlyInAnyOrder(key1, key3);
    }

    @Test
    @DisplayName("getVerifiedAndSortedILoqKeys")
    void testShouldSortTheVerifiedKeysByRealEstateId() throws Exception {
        String realEstateId1 = "abc-123";
        String realEstateId2 = "xyz-456";
        String realEstateId3 = "abc-123";
        ILoqKeyResponse key1 = new ILoqKeyResponse();
        key1.setRealEstateId(realEstateId1);
        ILoqKeyResponse key2 = new ILoqKeyResponse();
        key2.setRealEstateId(realEstateId2);
        ILoqKeyResponse key3 = new ILoqKeyResponse();
        key3.setRealEstateId(realEstateId3);
        List<ILoqKeyResponse> allKeys = List.of(key1, key2, key3);

        Exchange ex = testUtils.createExchange(allKeys);

        when(configProvider.getConfiguredILoqRealEstateIds())
                .thenReturn(List.of(realEstateId1, realEstateId2, realEstateId3));

        iLoqKeyProcessor.getILoqKeysWithVerifiedRealEstate(ex);

        List<ILoqKeyResponse> result = ex.getIn().getBody(List.class);

        assertThat(result).containsExactly(key1, key3, key2);
    }

    @Test
    @DisplayName("isMissingAPerson")
    void testShouldReturnTrueWhenAnILoqKeyDoesNotHaveAPerson() throws Exception {
        ILoqKeyResponse iLoqKey = new ILoqKeyResponse();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("currentILoqKey", iLoqKey);

        boolean result = iLoqKeyProcessor.isMissingAPerson(ex);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isMissingAPerson")
    void testShouldReturnFalseWhenAnILoqKeyHasAPerson() throws Exception {
        ILoqKeyResponse iLoqKey = new ILoqKeyResponse();
        iLoqKey.setPersonId("irrelevant");
        Exchange ex = testUtils.createExchange();
        ex.setProperty("currentILoqKey", iLoqKey);

        boolean result = iLoqKeyProcessor.isMissingAPerson(ex);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("hasValidSecurityAccesses")
    void testShouldValidateTheSecurityAccesses() throws Exception {
        String iLoqSecurityAccessId1 = "1";
        String iLoqSecurityAccessId2 = "2";
        Set<ILoqSecurityAccess> securityAccesses = Set.of(
                new ILoqSecurityAccess(iLoqSecurityAccessId1),
                new ILoqSecurityAccess(iLoqSecurityAccessId2));
        Exchange ex = testUtils.createExchange(securityAccesses);

        when(configProvider.isValidILoqSecurityAccess(anyString())).thenReturn(true);

        verifyNoInteractions(configProvider);

        iLoqKeyProcessor.hasValidSecurityAccesses(ex);

        verify(configProvider).isValidILoqSecurityAccess(iLoqSecurityAccessId1);
        verify(configProvider).isValidILoqSecurityAccess(iLoqSecurityAccessId2);
    }

    @Test
    @DisplayName("hasValidSecurityAccesses")
    void testShouldReturnFalseIfKeyContainsNonSupportedSecurityAccesses() throws Exception {
        String iLoqSecurityAccessId1 = "1";
        String iLoqSecurityAccessId2 = "2";
        Set<ILoqSecurityAccess> securityAccesses = Set.of(
                new ILoqSecurityAccess(iLoqSecurityAccessId1),
                new ILoqSecurityAccess(iLoqSecurityAccessId2));
        Exchange ex = testUtils.createExchange(securityAccesses);

        when(configProvider.isValidILoqSecurityAccess("2")).thenReturn(true);

        boolean result = iLoqKeyProcessor.hasValidSecurityAccesses(ex);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("hasValidSecurityAccesses")
    void testShouldReturnTrueIfKeyContainsOnlySupportedSecurityAccesses() throws Exception {
        String iLoqSecurityAccessId1 = "1";
        String iLoqSecurityAccessId2 = "2";
        Set<ILoqSecurityAccess> securityAccesses = Set.of(
                new ILoqSecurityAccess(iLoqSecurityAccessId1),
                new ILoqSecurityAccess(iLoqSecurityAccessId2));
        Exchange ex = testUtils.createExchange(securityAccesses);

        when(configProvider.isValidILoqSecurityAccess(anyString())).thenReturn(true);

        boolean result = iLoqKeyProcessor.hasValidSecurityAccesses(ex);

        assertThat(result).isTrue();
    }

}
