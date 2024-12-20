package fi.hel.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.configurations.ConfigProvider;
import fi.hel.mappers.EfecteKeyMapper;
import fi.hel.mappers.ILoqKeyMapper;
import fi.hel.models.EfecteAttributeImport;
import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.EfecteEntityImport;
import fi.hel.models.EfecteEntitySetImport;
import fi.hel.models.EfecteReference;
import fi.hel.models.EnrichedILoqKey;
import fi.hel.models.ILoqKey;
import fi.hel.models.ILoqKeyImport;
import fi.hel.models.ILoqKeyResponse;
import fi.hel.models.ILoqSecurityAccess;
import fi.hel.models.PreviousEfecteKey;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.models.enumerations.EnumEfecteKeyState;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import fi.hel.resolvers.EfecteKeyResolver;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("unchecked")
public class EfecteKeyProcessorTest {

    @Inject
    ResourceInjector ri;
    @Inject
    TestUtils testUtils;
    @Inject
    MockEndpointInjector mocked;
    @Inject
    EfecteKeyProcessor efecteKeyProcessor;
    @InjectMock
    Redis redis;
    @InjectMock
    Helper helper;
    @InjectMock
    EfecteKeyMapper efecteKeyMapper;
    @InjectMock
    EfecteKeyResolver efecteKeyResolver;
    @InjectMock
    ConfigProvider configProvider;
    @InjectMock
    ILoqKeyMapper iLoqKeyMapper;

    @BeforeEach
    void setup() {
        mocked.getGetEfecteEntity().reset();
        reset(configProvider, redis);
        efecteKeyProcessor.resetCache();
    }

    ////////////////////
    // Efecte -> iLOQ //
    ////////////////////

    @Test
    @DisplayName("isValidated")
    void testShouldCheckTheEfecteStreetAddressValidity() throws Exception {
        String expectedEfecteAddressEntityId = "12345";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withStreetAddress(expectedEfecteAddressEntityId, "irrelevant")
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        verifyNoInteractions(configProvider);

        efecteKeyProcessor.isValidated(ex);

        verify(configProvider).isValidEfecteAddress(expectedEfecteAddressEntityId);
    }

    @Test
    @DisplayName("isValidated")
    void testShouldCheckTheSecurityAccessValidity() throws Exception {
        String expectedSecurityAccessId = "12345";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withSecurityAccesses(new EfecteReference(expectedSecurityAccessId))
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        when(configProvider.isValidEfecteAddress(anyString())).thenReturn(true);

        verifyNoInteractions(configProvider);

        efecteKeyProcessor.isValidated(ex);

        verify(configProvider).isValidEfecteSecurityAccess(expectedSecurityAccessId);
    }

    @Test
    @DisplayName("isValidated")
    void testShouldReturnFalseWhenAddressIsNotValid() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withStreetAddress("invalid street address id", "irrelevant")
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        when(configProvider.isValidEfecteAddress(any())).thenReturn(false);
        when(configProvider.isValidEfecteSecurityAccess(any())).thenReturn(true);

        boolean result = efecteKeyProcessor.isValidated(ex);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidated")
    void testShouldReturnFalseWhenSecurityAccessIsNotValid() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withSecurityAccesses(new EfecteReference("invalid security access id"))
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        when(configProvider.isValidEfecteAddress(any())).thenReturn(true);
        when(configProvider.isValidEfecteSecurityAccess(any())).thenReturn(false);

        boolean result = efecteKeyProcessor.isValidated(ex);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidated")
    void testShouldReturnTrueForValidKeyStates() throws Exception {
        List<EnumEfecteKeyState> validKeyStates = List.of(
                EnumEfecteKeyState.ODOTTAA_AKTIVOINTIA,
                EnumEfecteKeyState.AKTIIVINEN,
                EnumEfecteKeyState.PASSIIVINEN);

        when(configProvider.isValidEfecteAddress(any())).thenReturn(true);
        when(configProvider.isValidEfecteSecurityAccess(any())).thenReturn(true);

        for (EnumEfecteKeyState validKeyState : validKeyStates) {
            EfecteEntity efecteEntity = new EfecteEntityBuilder()
                    .withState(validKeyState)
                    .withDefaults(EnumEfecteTemplate.KEY)
                    .build();
            Exchange ex = testUtils.createExchange();
            ex.setProperty("efecteKey", efecteEntity);

            boolean result = efecteKeyProcessor.isValidated(ex);

            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("isValidated")
    void testShouldReturnFalseForInvalidKeyStates() throws Exception {
        List<EnumEfecteKeyState> invalidKeyStates = List.of(
                EnumEfecteKeyState.POISTETTU,
                EnumEfecteKeyState.HYLATTY);

        when(configProvider.isValidEfecteAddress(any())).thenReturn(true);
        when(configProvider.isValidEfecteSecurityAccess(any())).thenReturn(true);

        for (EnumEfecteKeyState invalidKeyState : invalidKeyStates) {
            EfecteEntity efecteEntity = new EfecteEntityBuilder()
                    .withState(invalidKeyState)
                    .withDefaults(EnumEfecteTemplate.KEY)
                    .build();
            Exchange ex = testUtils.createExchange();
            ex.setProperty("efecteKey", efecteEntity);

            boolean result = efecteKeyProcessor.isValidated(ex);

            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("isValidated")
    void testShouldReturnFalseForInvalidKeyTypes() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withKeyType("invalidKeyType")
                .withState(EnumEfecteKeyState.AKTIIVINEN)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        Exchange ex = testUtils.createExchange();
        ex.setProperty("efecteKey", efecteEntity);

        when(configProvider.isValidEfecteAddress(any())).thenReturn(true);
        when(configProvider.isValidEfecteSecurityAccess(any())).thenReturn(true);

        boolean result = efecteKeyProcessor.isValidated(ex);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isValidated")
    void testShouldReturnTrueForValidKeys() throws Exception {
        List<EnumEfecteKeyState> validKeyStates = List.of(
                EnumEfecteKeyState.ODOTTAA_AKTIVOINTIA,
                EnumEfecteKeyState.AKTIIVINEN,
                EnumEfecteKeyState.PASSIIVINEN);

        when(configProvider.isValidEfecteAddress(any())).thenReturn(true);
        when(configProvider.isValidEfecteSecurityAccess(any())).thenReturn(true);

        for (EnumEfecteKeyState validKeyState : validKeyStates) {
            EfecteEntity efecteEntity = new EfecteEntityBuilder()
                    .withState(validKeyState)
                    .withKeyType("iLOQ")
                    .withDefaults(EnumEfecteTemplate.KEY)
                    .build();
            Exchange ex = testUtils.createExchange();
            ex.setProperty("efecteKey", efecteEntity);

            boolean result = efecteKeyProcessor.isValidated(ex);

            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("updatePreviousEfecteKeyValue")
    void testShouldUpdateTheEfecteKeyStatusInNewPreviousEfecteKey() throws Exception {
        String oldState = "Odottaa aktivointia";
        String expectedState = "Aktiivinen";
        Set<String> securityAccessEntityIds = Set.of("1", "2");
        String validityDate = "foo";
        PreviousEfecteKey newPreviousEfecteKey = new PreviousEfecteKey(oldState, securityAccessEntityIds, validityDate);

        Exchange ex = testUtils.createExchange();
        ex.setProperty("newPreviousEfecteKey", newPreviousEfecteKey);

        efecteKeyProcessor.updatePreviousEfecteKeyValue(ex);

        PreviousEfecteKey updatedPreviousEfecteKey = ex.getProperty("newPreviousEfecteKey", PreviousEfecteKey.class);

        assertThat(updatedPreviousEfecteKey.getState()).isEqualTo(expectedState);
        assertThat(updatedPreviousEfecteKey.getSecurityAccesses()).isEqualTo(securityAccessEntityIds);
        assertThat(updatedPreviousEfecteKey.getValidityDate()).isEqualTo(validityDate);
    }

    ////////////////////
    // iLOQ -> Efecte //
    ////////////////////

    @Test
    @DisplayName("buildEfecteKey")
    void testShouldGetTheMappedEfecteIdentifier() throws Exception {
        String iLoqKeyId = "abc-123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setInfoText(null);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(Set.of());
        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        setDefaultResponses();
        String expectedPrefix = ri.getMappedKeyILoqPrefix() + iLoqKeyId;

        verifyNoInteractions(redis);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(redis).get(expectedPrefix);
    }

    @Test
    @DisplayName("buildEfecteKey")
    void testShouldGetThePreviousILoqKeySecurityAccessesFromRedis() throws Exception {
        String iLoqKeyId = "abc-123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(Set.of());
        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        setDefaultResponses();
        String expectedPrefix = ri.getPreviousKeyILoqPrefix() + iLoqKeyId;

        verifyNoInteractions(redis);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(redis).getSet(expectedPrefix);
    }

    //////////////////////////////////
    // Key IS NOT previously mapped //
    //////////////////////////////////

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped")
    void testShouldResolveTheMatchingEfecteAddressForTheILoqRealEstate() throws Exception {
        String iLoqKeyId = "abc-123";
        String expectedILoqRealEstateId = "xyz-456";

        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setRealEstateId(expectedILoqRealEstateId);
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess()));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        setDefaultResponses();
        when(redis.get(anyString())).thenReturn(null);

        verifyNoInteractions(configProvider);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(configProvider).getEfecteAddressNameByILoqRealEstateId(expectedILoqRealEstateId);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped")
    void testShouldNotResolveTheMatchingEfecteAddressIfTheRealEstateHasNotChanged() throws Exception {
        String iLoqRealEstateId = "xyz-456";

        EnrichedILoqKey enrichedILoqKey1 = new EnrichedILoqKey("irrelevant");
        enrichedILoqKey1.setRealEstateId(iLoqRealEstateId);
        enrichedILoqKey1.setSecurityAccesses(Set.of(new ILoqSecurityAccess()));
        EnrichedILoqKey enrichedILoqKey2 = new EnrichedILoqKey("irrelevant");
        enrichedILoqKey2.setRealEstateId(iLoqRealEstateId);
        enrichedILoqKey2.setSecurityAccesses(Set.of(new ILoqSecurityAccess()));

        List<EnrichedILoqKey> iLoqKeys = List.of(enrichedILoqKey1, enrichedILoqKey2);
        setDefaultResponses();

        for (EnrichedILoqKey key : iLoqKeys) {
            Exchange ex = testUtils.createExchange();
            ex.setProperty("enrichedILoqKey", key);

            efecteKeyProcessor.buildEfecteKey(ex);
        }

        verify(configProvider, times(1)).getEfecteAddressNameByILoqRealEstateId(any());
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped")
    void testShouldListAllEfecteKeysInTheKeyAddress()
            throws Exception {
        String iLoqKeyId = "abc-123";
        String efecteAddress = "Testikatu 1, 00100, Helsinki";
        String urlEncodedEfecteAddress = "this is url encoded value of efecteAddress";
        String expectedEfecteQuery = """
                SELECT entity
                FROM entity
                WHERE
                    template.code = 'avain'
                    AND $avain_tyyppi$ = 'iLOQ'
                    AND $avain_katuosoite$ = '%s'
                    AND $avain_external_id$ IS NULL
                """.formatted(urlEncodedEfecteAddress).replaceAll("\\s+", " ").trim();

        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        setDefaultResponses();
        when(redis.get(anyString())).thenReturn(null);
        when(configProvider.getEfecteAddressNameByILoqRealEstateId(any())).thenReturn(efecteAddress);
        when(helper.urlEncode(efecteAddress)).thenReturn(urlEncodedEfecteAddress);

        mocked.getGetEfecteEntity().expectedMessageCount(1);
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteEntityType", "key");
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteQuery", expectedEfecteQuery);

        verifyNoInteractions(helper);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(helper).urlEncode(efecteAddress);
        mocked.getGetEfecteEntity().assertIsSatisfied();
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped")
    void testShouldNotListEfecteKeysAgainIfTheRealEstateHasNotChanged() throws Exception {
        String iLoqRealEstateId = "xyz-456";

        EnrichedILoqKey enrichedILoqKey1 = new EnrichedILoqKey("irrelevant");
        enrichedILoqKey1.setRealEstateId(iLoqRealEstateId);
        enrichedILoqKey1.setSecurityAccesses(Set.of(new ILoqSecurityAccess("1")));
        EnrichedILoqKey enrichedILoqKey2 = new EnrichedILoqKey("irrelevant");
        enrichedILoqKey2.setRealEstateId(iLoqRealEstateId);
        enrichedILoqKey2.setSecurityAccesses(Set.of(new ILoqSecurityAccess("2")));

        List<EnrichedILoqKey> iLoqKeys = List.of(enrichedILoqKey1, enrichedILoqKey2);
        setDefaultResponses();
        when(redis.get(any())).thenReturn(null);

        mocked.getGetEfecteEntity().expectedMessageCount(1);

        for (EnrichedILoqKey key : iLoqKeys) {
            Exchange ex = testUtils.createExchange();
            ex.setProperty("enrichedILoqKey", key);

            efecteKeyProcessor.buildEfecteKey(ex);
        }

        mocked.getGetEfecteEntity().assertIsSatisfied();
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped")
    void testShouldBuildAnEqualEfecteKey() throws Exception {
        String iLoqKeyId = "abc-123";
        String expectedEfecteAddress = "Testikatu 1, 00100, Helsinki";

        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);
        setDefaultResponses();

        when(redis.get(any())).thenReturn(null);
        when(configProvider.getEfecteAddressNameByILoqRealEstateId(any())).thenReturn(expectedEfecteAddress);

        verifyNoInteractions(efecteKeyResolver);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(efecteKeyResolver).buildEqualEfecteKey(enrichedILoqKey, expectedEfecteAddress);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped")
    void testShouldUseTheBuiltEfecteKeyForSearchingTheActualEfecteKeyMatch() throws Exception {
        String iLoqKeyId = "abc-123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess()));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);
        setDefaultResponses();
        when(redis.get(anyString())).thenReturn(null);

        List<EfecteEntity> expectedEfecteKeys = List.of(new EfecteEntity());
        mocked.getGetEfecteEntity()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(expectedEfecteKeys));
        EfecteEntity expectedBuiltMatch = new EfecteEntity();
        when(efecteKeyResolver.buildEqualEfecteKey(any(), any())).thenReturn(expectedBuiltMatch);

        verifyNoInteractions(efecteKeyResolver);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(efecteKeyResolver).findMatchingEfecteKey(expectedBuiltMatch, expectedEfecteKeys);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped - a match for Efecte key IS NOT found")
    void testShouldBuildANewEfecteKeyWhenNoMatchIsFound() throws Exception {
        String iLoqKeyId = "abc-123";

        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);
        setDefaultResponses();
        when(redis.get(anyString())).thenReturn(null);
        when(efecteKeyResolver.findMatchingEfecteKey(any(), any())).thenReturn(null);

        verifyNoInteractions(efecteKeyMapper);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(efecteKeyMapper).buildNewEfecteEntitySetImport(enrichedILoqKey);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped - a match for Efecte key IS NOT found")
    void testShouldGetTheNewEfecteSecurityAccessEntityIds_CreateEfecteKey() throws Exception {
        String iLoqKeyId = "abc-123";

        Set<ILoqSecurityAccess> expectedILoqSecurityAccessIds = Set.of(new ILoqSecurityAccess("irrelevant"));
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(expectedILoqSecurityAccessIds);

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        EfecteEntityImport entityImport = new EfecteEntityImport(
                EnumEfecteTemplate.KEY,
                List.of(new EfecteAttributeImport(
                        EnumEfecteAttribute.KEY_VALIDITY_DATE, "foo")));
        EfecteEntitySetImport efecteEntitySetImport = new EfecteEntitySetImport();
        efecteEntitySetImport.setEntity(entityImport);

        when(redis.get(anyString())).thenReturn(null);
        when(efecteKeyResolver.findMatchingEfecteKey(any(), any())).thenReturn(null);
        when(efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey)).thenReturn(efecteEntitySetImport);

        verifyNoInteractions(efecteKeyResolver);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(efecteKeyResolver).getNewEfecteSecurityAccessEntityIds(expectedILoqSecurityAccessIds);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped - a match for Efecte key IS NOT found")
    void testShouldSetThePropertyValues_CreateKey() throws Exception {
        String iLoqKeyId = "abc-123";
        String expectedILoqSecurityAccessId1 = "1";
        String expectedILoqSecurityAccessId2 = "2";
        Set<ILoqSecurityAccess> iLoqSecurityAccesses = Set.of(
                new ILoqSecurityAccess(expectedILoqSecurityAccessId1),
                new ILoqSecurityAccess(expectedILoqSecurityAccessId2));
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(iLoqSecurityAccesses);

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        String expectedEfecteSecurityAccessId1 = "501";
        String expectedEfecteSecurityAccessId2 = "502";
        Set<String> efecteSecurityAccessEntityIds = Set.of(expectedEfecteSecurityAccessId1,
                expectedEfecteSecurityAccessId2);
        String validityDate = "foo";
        EfecteEntityImport entityImport = new EfecteEntityImport(
                EnumEfecteTemplate.KEY,
                List.of(new EfecteAttributeImport(
                        EnumEfecteAttribute.KEY_VALIDITY_DATE, validityDate)));
        EfecteEntitySetImport expectedPayload = new EfecteEntitySetImport();
        expectedPayload.setEntity(entityImport);
        PreviousEfecteKey expectedNewPreviousEfecteKey = new PreviousEfecteKey(
                EnumEfecteKeyState.AKTIIVINEN.getName(),
                efecteSecurityAccessEntityIds,
                validityDate);

        when(redis.get(any())).thenReturn(null);
        when(efecteKeyResolver.findMatchingEfecteKey(any(), any())).thenReturn(null);
        when(efecteKeyMapper.buildNewEfecteEntitySetImport(any())).thenReturn(expectedPayload);
        when(efecteKeyResolver.getNewEfecteSecurityAccessEntityIds(iLoqSecurityAccesses))
                .thenReturn(efecteSecurityAccessEntityIds);

        efecteKeyProcessor.buildEfecteKey(ex);

        boolean shouldUpdateEfecteKey = ex.getProperty("shouldUpdateEfecteKey", boolean.class);
        boolean shouldCreateEfecteKey = ex.getProperty("shouldCreateEfecteKey", boolean.class);
        EfecteEntitySetImport payload = ex.getProperty("efectePayload", EfecteEntitySetImport.class);
        Set<String> newILoqSecurityAccessIds = ex.getProperty("newILoqSecurityAccessIds", Set.class);
        PreviousEfecteKey newPreviousEfecteKey = ex.getProperty("newPreviousEfecteKey",
                PreviousEfecteKey.class);

        assertThat(shouldUpdateEfecteKey).isFalse();
        assertThat(shouldCreateEfecteKey).isTrue();
        assertThat(payload).isEqualTo(expectedPayload);
        assertThat(newILoqSecurityAccessIds).containsExactlyInAnyOrder(expectedILoqSecurityAccessId1,
                expectedILoqSecurityAccessId2);
        assertThat(newPreviousEfecteKey).isEqualTo(expectedNewPreviousEfecteKey);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped - a match for Efecte key IS found")
    void testShouldSaveTheMappedKeysWhenAMatchIsFound() throws Exception {
        String expectedILoqKeyId = "abc-123";

        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(expectedILoqKeyId);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        when(redis.get(anyString())).thenReturn(null);
        String entityId = "12345";
        String efecteId = "KEY-001";
        EfecteEntity matchinEfecteKey = new EfecteEntityBuilder()
                .withId(entityId)
                .withKeyEfecteId(efecteId)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        when(efecteKeyResolver.findMatchingEfecteKey(any(), any())).thenReturn(matchinEfecteKey);
        setDefaultResponses();

        EfecteEntityIdentifier expectedEfecteEntityIdentifier = new EfecteEntityIdentifier(
                entityId, efecteId);
        String expectedEfecteIdentifierJson = "this is efecte entity identifier json";

        String expectedEfectePrefix = ri.getMappedKeyEfectePrefix() + efecteId;
        String expectedILoqPrefix = ri.getMappedKeyILoqPrefix() + expectedILoqKeyId;

        when(helper.writeAsJson(any(EfecteEntityIdentifier.class))).thenReturn(expectedEfecteIdentifierJson);

        verifyNoInteractions(redis);
        verifyNoInteractions(helper);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(helper).writeAsJson(expectedEfecteEntityIdentifier);
        verify(redis).set(expectedEfectePrefix, expectedILoqKeyId);
        verify(redis).set(expectedILoqPrefix, expectedEfecteIdentifierJson);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped - a match for Efecte key IS found")
    void testShouldCreateTheILoqPayloadForPreviouslyUnmappedExistingEfecteKey() throws Exception {
        String expectedILoqKeyId = "abc-123";

        ILoqKeyResponse expectedILoqKeyResponse = new ILoqKeyResponse(expectedILoqKeyId);
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(expectedILoqKeyId);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);
        ex.setProperty("currentILoqKey", expectedILoqKeyResponse);

        when(redis.get(anyString())).thenReturn(null);
        String entityId = "12345";
        String efecteId = "KEY-001";
        EfecteEntity matchinEfecteKey = new EfecteEntityBuilder()
                .withId(entityId)
                .withKeyEfecteId(efecteId)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        when(efecteKeyResolver.findMatchingEfecteKey(any(), any())).thenReturn(matchinEfecteKey);
        setDefaultResponses();

        verifyNoInteractions(iLoqKeyMapper);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(iLoqKeyMapper).buildUpdatedILoqKey(matchinEfecteKey, expectedILoqKeyResponse);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped - a match for Efecte key IS found")
    void testShouldUpdateTheEfecteKeyWithExternalId() throws Exception {
        String expectedILoqKeyId = "abc-123";

        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(expectedILoqKeyId);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        when(redis.get(anyString())).thenReturn(null);
        String entityId = "12345";
        String expectedEfecteId = "KEY-001";
        EfecteEntity matchingEfecteKey = new EfecteEntityBuilder()
                .withId(entityId)
                .withKeyEfecteId(expectedEfecteId)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        when(efecteKeyResolver.findMatchingEfecteKey(any(), any())).thenReturn(matchingEfecteKey);
        setDefaultResponses();

        verifyNoInteractions(efecteKeyMapper);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(efecteKeyMapper).buildEfecteEntitySetUpdate(expectedILoqKeyId, expectedEfecteId);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped - a match for Efecte key IS found")
    void testShouldGetTheNewEfecteSecurityAccessEntityIds_UpdateMatchedKey() throws Exception {
        String iLoqKeyId = "abc-123";

        Set<ILoqSecurityAccess> expectedILoqSecurityAccessIds = Set.of(new ILoqSecurityAccess("irrelevant"));
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(expectedILoqSecurityAccessIds);

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        when(redis.get(anyString())).thenReturn(null);

        when(efecteKeyResolver.findMatchingEfecteKey(any(), any()))
                .thenReturn(new EfecteEntityBuilder().withDefaults(EnumEfecteTemplate.KEY).build());
        when(efecteKeyMapper.buildEfecteEntitySetUpdate(enrichedILoqKey, "irrelevant"))
                .thenReturn(new EfecteEntitySetImport());

        verifyNoInteractions(efecteKeyResolver);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(efecteKeyResolver).getNewEfecteSecurityAccessEntityIds(expectedILoqSecurityAccessIds);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS NOT previously mapped - a match for Efecte key IS found")
    void testShouldSetThePropertyValues_UpdateMatchedKey() throws Exception {
        String iLoqKeyId = "abc-123";
        String expectedILoqSecurityAccessId1 = "1";
        String expectedILoqSecurityAccessId2 = "2";
        Set<ILoqSecurityAccess> iLoqSecurityAccesses = Set.of(
                new ILoqSecurityAccess(expectedILoqSecurityAccessId1),
                new ILoqSecurityAccess(expectedILoqSecurityAccessId2));
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setRealEstateId("irrelevant, but not null");
        enrichedILoqKey.setSecurityAccesses(iLoqSecurityAccesses);

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        String expectedEfecteSecurityAccessId1 = "501";
        String expectedEfecteSecurityAccessId2 = "502";
        Set<String> efecteSecurityAccessEntityIds = Set.of(expectedEfecteSecurityAccessId1,
                expectedEfecteSecurityAccessId2);
        String validityDate = "06.08.2024 00:00";
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY_00123";
        EfecteEntity matchingEfecteKey = new EfecteEntityBuilder()
                .withId(expectedEntityId)
                .withKeyEfecteId(expectedEfecteId)
                .withValidityDate(validityDate)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        PreviousEfecteKey expectedNewPreviousEfecteKey = new PreviousEfecteKey(
                EnumEfecteKeyState.AKTIIVINEN.getName(),
                efecteSecurityAccessEntityIds,
                validityDate);
        EfecteEntitySetImport expectedEfectePayload = new EfecteEntitySetImport(new EfecteEntityImport());
        ILoqKeyImport expectedILoqKeyImport = new ILoqKeyImport(new ILoqKey(iLoqKeyId));

        when(redis.get(any())).thenReturn(null);
        when(efecteKeyResolver.findMatchingEfecteKey(any(), any()))
                .thenReturn(matchingEfecteKey);
        when(efecteKeyResolver.getNewEfecteSecurityAccessEntityIds(iLoqSecurityAccesses))
                .thenReturn(efecteSecurityAccessEntityIds);
        when(iLoqKeyMapper.buildUpdatedILoqKey(any(), any())).thenReturn(expectedILoqKeyImport);
        when(efecteKeyMapper.buildEfecteEntitySetUpdate(anyString(), anyString()))
                .thenReturn(expectedEfectePayload);

        efecteKeyProcessor.buildEfecteKey(ex);

        boolean shouldUpdateEfecteKey = ex.getProperty("shouldUpdateEfecteKey", boolean.class);
        boolean shouldCreateEfecteKey = ex.getProperty("shouldCreateEfecteKey", boolean.class);
        boolean shouldUpdateILoqKey = ex.getProperty("shouldUpdateILoqKey", boolean.class);
        Set<String> newILoqSecurityAccessIds = ex.getProperty("newILoqSecurityAccessIds", Set.class);
        PreviousEfecteKey newPreviousEfecteKey = ex.getProperty("newPreviousEfecteKey",
                PreviousEfecteKey.class);
        ILoqKeyImport iLoqPayload = ex.getProperty("iLoqPayload", ILoqKeyImport.class);
        String efecteKeyEntityId = ex.getProperty("efecteKeyEntityId", String.class);
        String efecteKeyEfecteId = ex.getProperty("efecteKeyEfecteId", String.class);
        EfecteEntitySetImport efectePayload = ex.getProperty("efectePayload", EfecteEntitySetImport.class);

        assertThat(shouldUpdateEfecteKey).isFalse();
        assertThat(shouldCreateEfecteKey).isFalse();
        assertThat(shouldUpdateILoqKey).isTrue();
        assertThat(newILoqSecurityAccessIds).containsExactlyInAnyOrder(expectedILoqSecurityAccessId1,
                expectedILoqSecurityAccessId2);
        assertThat(newPreviousEfecteKey).isEqualTo(expectedNewPreviousEfecteKey);
        assertThat(iLoqPayload).isEqualTo(expectedILoqKeyImport);
        assertThat(efecteKeyEntityId).isEqualTo(expectedEntityId);
        assertThat(efecteKeyEfecteId).isEqualTo(expectedEfecteId);
        assertThat(efectePayload).isEqualTo(expectedEfectePayload);
    }

    //////////////////////////////
    // Key IS previously mapped //
    //////////////////////////////

    @Test
    @DisplayName("buildEfecteKey - key IS previously mapped - previous iLOQ security accesses are EQUAL")
    void testShouldStopHandlingTheILoqKeyWhenTheKeyIsMappedAndTheSecurityAccessesHasNotChanged() throws Exception {
        String iLoqKeyId = "abc-123";

        String securityAccessId1 = "1";
        String securityAccessId2 = "2";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setSecurityAccesses(Set.of(
                new ILoqSecurityAccess(securityAccessId1),
                new ILoqSecurityAccess(securityAccessId2)));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        when(redis.get(anyString())).thenReturn("irrelevant but not null");
        when(redis.getSet(anyString())).thenReturn(Set.of(securityAccessId1, securityAccessId2));

        efecteKeyProcessor.buildEfecteKey(ex);

        boolean shouldUpdateEfecteKey = ex.getProperty("shouldUpdateEfecteKey", boolean.class);
        boolean shouldCreateEfecteKey = ex.getProperty("shouldCreateEfecteKey", boolean.class);
        EfecteEntitySetImport efectePayload = ex.getProperty("efectePayload", EfecteEntitySetImport.class);
        PreviousEfecteKey newPreviousEfecteKey = ex.getProperty("newPreviousEfecteKey",
                PreviousEfecteKey.class);
        ILoqKeyImport iLoqPayload = ex.getProperty("iLoqPayload", ILoqKeyImport.class);

        assertThat(shouldUpdateEfecteKey).isFalse();
        assertThat(shouldCreateEfecteKey).isFalse();
        assertThat(efectePayload).isNull();
        assertThat(newPreviousEfecteKey).isNull();
        assertThat(iLoqPayload).isNull();
    }

    @Test
    @DisplayName("buildEfecteKey - key IS previously mapped - previous iLOQ security accesses are NONEQUAL")
    void testShouldConvertTheEfecteEntityIdentifier() throws Exception {
        String iLoqKeyId = "abc-123";
        String expectedEfecteId = "KEY_001";

        String securityAccessId1 = "1";
        String securityAccessId2 = "2";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setInfoText(expectedEfecteId);
        enrichedILoqKey.setSecurityAccesses(Set.of(
                new ILoqSecurityAccess(securityAccessId1),
                new ILoqSecurityAccess(securityAccessId2)));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        String expectedEfecteEntityIdentifierJson = """
                {
                    "entityId": "irrelevant",
                    "efecteId": "irrelevant"
                }
                """;

        setDefaultResponses();
        when(redis.get(anyString())).thenReturn(expectedEfecteEntityIdentifierJson);
        when(redis.getSet(anyString())).thenReturn(Set.of(securityAccessId1));
        when(helper.writeAsPojo(any(), any()))
                .thenReturn(new EfecteEntityIdentifier())
                .thenReturn(new PreviousEfecteKey());

        verifyNoInteractions(helper);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(helper).writeAsPojo(expectedEfecteEntityIdentifierJson, EfecteEntityIdentifier.class);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS previously mapped - previous iLOQ security accesses are NONEQUAL")
    void testShouldBuildAnUpdatedEfecteKeyWhenILoqSecurityAccessesHasChanged() throws Exception {
        String iLoqKeyId = "abc-123";
        String expectedEfecteId = "KEY_001";

        String securityAccessId1 = "1";
        String securityAccessId2 = "2";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setInfoText(expectedEfecteId);
        enrichedILoqKey.setSecurityAccesses(Set.of(
                new ILoqSecurityAccess(securityAccessId1),
                new ILoqSecurityAccess(securityAccessId2)));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);
        PreviousEfecteKey previousEfecteKey = new PreviousEfecteKey(
                EnumEfecteKeyState.AKTIIVINEN.getName(),
                Set.of("old efecte security access entity id"),
                "irrelevant");

        setDefaultResponses();
        when(redis.getSet(anyString())).thenReturn(Set.of(securityAccessId1));
        when(helper.writeAsPojo(any(), any()))
                .thenReturn(new EfecteEntityIdentifier())
                .thenReturn(previousEfecteKey);

        verifyNoInteractions(efecteKeyMapper);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(efecteKeyMapper).buildEfecteEntitySetUpdate(enrichedILoqKey, expectedEfecteId);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS previously mapped - previous iLOQ security accesses are NONEQUAL")
    void testShouldGetTheNewEfecteSecurityAccessEntityIds_UpdateExistingKey() throws Exception {
        String iLoqKeyId = "abc-123";
        String efecteId = "KEY_00123";
        String securityAccessId1 = "1";
        String securityAccessId2 = "2";
        Set<ILoqSecurityAccess> expectedILoqSecurityAccessIds = Set.of(
                new ILoqSecurityAccess(securityAccessId1),
                new ILoqSecurityAccess(securityAccessId2));
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setInfoText(efecteId);
        enrichedILoqKey.setSecurityAccesses(expectedILoqSecurityAccessIds);

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        when(redis.getSet(anyString())).thenReturn(Set.of(securityAccessId1));
        when(efecteKeyMapper.buildEfecteEntitySetUpdate(enrichedILoqKey, efecteId))
                .thenReturn(new EfecteEntitySetImport());
        when(helper.writeAsPojo(any(), any()))
                .thenReturn(new EfecteEntityIdentifier())
                .thenReturn(new PreviousEfecteKey());

        verifyNoInteractions(efecteKeyResolver);

        efecteKeyProcessor.buildEfecteKey(ex);

        verify(efecteKeyResolver).getNewEfecteSecurityAccessEntityIds(expectedILoqSecurityAccessIds);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS previously mapped - previous iLOQ security accesses are NONEQUAL")
    void testShouldSetThePropertyValues_UpdateExistingKey() throws Exception {
        String iLoqKeyId = "abc-123";

        String expectedILoqSecurityAccessId1 = "1";
        String expectedILoqSecurityAccessId2 = "2";
        String expectedEfecteId = "KEY_00123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setInfoText(expectedEfecteId);
        enrichedILoqKey.setSecurityAccesses(Set.of(
                new ILoqSecurityAccess(expectedILoqSecurityAccessId1),
                new ILoqSecurityAccess(expectedILoqSecurityAccessId2)));

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);

        String efecteSecurityAccessEntityId1 = "501";
        String efecteSecurityAccessEntityId2 = "502";
        Set<String> efecteSecurityAccessEntityIds = Set.of(efecteSecurityAccessEntityId1,
                efecteSecurityAccessEntityId2);
        String validityDate = "06.08.2024 00:00";
        PreviousEfecteKey previousEfecteKey = new PreviousEfecteKey(
                EnumEfecteKeyState.AKTIIVINEN.getName(),
                Set.of("old efecte security access entity id"),
                validityDate);
        String expectedPreviousEfecteKeyJson = testUtils.writeAsJson(previousEfecteKey);
        PreviousEfecteKey expectedNewPreviousEfecteKey = new PreviousEfecteKey(
                EnumEfecteKeyState.AKTIIVINEN.getName(),
                efecteSecurityAccessEntityIds,
                validityDate);
        EfecteEntitySetImport expectedPayload = new EfecteEntitySetImport();
        expectedPayload.setEntity(new EfecteEntityImport());
        String expectedEntityId = "12345";
        String efecteEntityIdentifierJson = """
                {
                    "entityId": "%s",
                    "efecteId": "%s"
                }
                """.formatted(expectedEntityId, expectedEfecteId);
        EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier(expectedEntityId,
                expectedEfecteId);

        when(redis.get(anyString())).thenReturn(efecteEntityIdentifierJson);
        when(redis.getSet(anyString())).thenReturn(Set.of(expectedILoqSecurityAccessId1));
        when(efecteKeyMapper.buildEfecteEntitySetUpdate(enrichedILoqKey, expectedEfecteId))
                .thenReturn(expectedPayload);
        when(efecteKeyResolver.getNewEfecteSecurityAccessEntityIds(any()))
                .thenReturn(efecteSecurityAccessEntityIds);

        String expectedPrefix = ri.getPreviousKeyEfectePrefix() + expectedEfecteId;
        when(redis.get(expectedPrefix)).thenReturn(expectedPreviousEfecteKeyJson);
        when(helper.writeAsPojo(any(), any()))
                .thenReturn(efecteEntityIdentifier)
                .thenReturn(previousEfecteKey);

        verifyNoInteractions(redis);
        verifyNoInteractions(helper);

        efecteKeyProcessor.buildEfecteKey(ex);

        boolean shouldUpdateEfecteKey = ex.getProperty("shouldUpdateEfecteKey", boolean.class);
        boolean shouldCreateEfecteKey = ex.getProperty("shouldCreateEfecteKey", boolean.class);
        EfecteEntitySetImport efectePayload = ex.getProperty("efectePayload", EfecteEntitySetImport.class);
        Set<String> newILoqSecurityAccessIds = ex.getProperty("newILoqSecurityAccessIds", Set.class);
        PreviousEfecteKey newPreviousEfecteKey = ex.getProperty("newPreviousEfecteKey",
                PreviousEfecteKey.class);
        ILoqKeyImport iLoqPayload = ex.getProperty("iLoqPayload", ILoqKeyImport.class);
        String efecteKeyEntityId = ex.getProperty("efecteKeyEntityId", String.class);
        String efecteKeyEfecteId = ex.getProperty("efecteKeyEfecteId", String.class);

        verify(redis).get(expectedPrefix);
        verify(helper).writeAsPojo(expectedPreviousEfecteKeyJson, PreviousEfecteKey.class);
        assertThat(shouldUpdateEfecteKey).isTrue();
        assertThat(shouldCreateEfecteKey).isFalse();
        assertThat(efectePayload).isEqualTo(expectedPayload);
        assertThat(newILoqSecurityAccessIds).containsExactlyInAnyOrder(expectedILoqSecurityAccessId1,
                expectedILoqSecurityAccessId2);
        assertThat(newPreviousEfecteKey).isEqualTo(expectedNewPreviousEfecteKey);
        assertThat(iLoqPayload).isNull();
        assertThat(efecteKeyEntityId).isEqualTo(expectedEntityId);
        assertThat(efecteKeyEfecteId).isEqualTo(expectedEfecteId);
    }

    @Test
    @DisplayName("buildEfecteKey - key IS previously mapped - previous iLOQ security accesses are NONEQUAL")
    void testShouldGetTheEfecteIdFromRedisWhenItIsNotStoredAtTheILoqKey() throws Exception {
        String iLoqKeyId = "abc-123";
        ILoqKeyResponse expectedILoqKeyResponse = new ILoqKeyResponse(iLoqKeyId);

        String securityAccessId1 = "1";
        String securityAccessId2 = "2";
        Set<ILoqSecurityAccess> expectedILoqSecurityAccessIds = Set.of(
                new ILoqSecurityAccess(securityAccessId1),
                new ILoqSecurityAccess(securityAccessId2));
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setInfoText("");
        enrichedILoqKey.setSecurityAccesses(expectedILoqSecurityAccessIds);

        Exchange ex = testUtils.createExchange();
        ex.setProperty("enrichedILoqKey", enrichedILoqKey);
        ex.setProperty("currentILoqKey", expectedILoqKeyResponse);

        String expectedValidityDate = "foobar";
        PreviousEfecteKey previousEfecteKey = new PreviousEfecteKey(
                EnumEfecteKeyState.AKTIIVINEN.getName(),
                Set.of("old efecte security access entity id"),
                expectedValidityDate);
        String expectedEntityId = "12345";
        String expectedEfecteId = "KEY_00123";
        String efecteEntityIdentifierJson = """
                {
                    "entityId": "%s",
                    "efecteId": "%s"
                }
                """.formatted(expectedEntityId, expectedEfecteId);
        EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier(expectedEntityId,
                expectedEfecteId);
        ILoqKeyImport expectedILoqKeyImport = new ILoqKeyImport(new ILoqKey(iLoqKeyId));

        when(redis.getSet(anyString())).thenReturn(Set.of(securityAccessId1));
        when(redis.get(anyString())).thenReturn(efecteEntityIdentifierJson);
        when(efecteKeyMapper.buildEfecteEntitySetUpdate(enrichedILoqKey, expectedEfecteId))
                .thenReturn(new EfecteEntitySetImport());
        when(helper.writeAsPojo(any(), any()))
                .thenReturn(efecteEntityIdentifier)
                .thenReturn(previousEfecteKey);
        when(iLoqKeyMapper.buildUpdatedILoqKey(any(), any())).thenReturn(expectedILoqKeyImport);

        EfecteEntity builtEfecteEntity = new EfecteEntityBuilder()
                .withKeyEfecteId(expectedEfecteId)
                .withValidityDate(expectedValidityDate)
                .build();

        verifyNoInteractions(efecteKeyMapper);
        verifyNoInteractions(iLoqKeyMapper);

        efecteKeyProcessor.buildEfecteKey(ex);

        ILoqKeyImport iLoqPayload = ex.getProperty("iLoqPayload", ILoqKeyImport.class);
        String efecteKeyEntityId = ex.getProperty("efecteKeyEntityId", String.class);
        String efecteKeyEfecteId = ex.getProperty("efecteKeyEfecteId", String.class);

        verify(efecteKeyMapper).buildEfecteEntitySetUpdate(enrichedILoqKey, expectedEfecteId);
        verify(iLoqKeyMapper).buildUpdatedILoqKey(builtEfecteEntity, expectedILoqKeyResponse);
        assertThat(iLoqPayload).isEqualTo(expectedILoqKeyImport);
        assertThat(efecteKeyEntityId).isEqualTo(expectedEntityId);
        assertThat(efecteKeyEfecteId).isEqualTo(expectedEfecteId);
    }

    private void setDefaultResponses() throws Exception {

        EfecteEntityImport entityImport = new EfecteEntityImport(
                EnumEfecteTemplate.KEY,
                List.of(new EfecteAttributeImport(
                        EnumEfecteAttribute.KEY_VALIDITY_DATE, "foo")));
        EfecteEntitySetImport efecteEntitySet = new EfecteEntitySetImport();
        efecteEntitySet.setEntity(entityImport);
        // when(efecteKeyMapper.buildEfecteEntitySetUpdate(any(), any())).thenReturn(efecteEntitySet);
        when(efecteKeyMapper.buildNewEfecteEntitySetImport(any())).thenReturn(efecteEntitySet);
    }
}
