package fi.hel.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.configurations.ConfigProvider;
import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteReference;
import fi.hel.models.ILoqKey;
import fi.hel.models.ILoqKeyImport;
import fi.hel.models.ILoqKeyResponse;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import fi.hel.processors.AuditExceptionProcessor;
import fi.hel.processors.Helper;
import fi.hel.processors.ILoqPersonProcessor;
import fi.hel.processors.ResourceInjector;
import fi.hel.resolvers.ILoqPersonResolver;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.sentry.util.Objects;
import jakarta.inject.Inject;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
public class ILoqKeyMapperTest {

    @Inject
    ResourceInjector ri;
    @Inject
    MockEndpointInjector mocked;
    @Inject
    ILoqKeyMapper iLoqKeyMapper;
    @Inject
    TestUtils testUtils;
    @InjectMock
    Redis redis;
    @InjectMock
    ILoqPersonProcessor iLoqPersonProcessor;
    @InjectMock
    ILoqPersonResolver iLoqPersonResolver;
    @InjectMock
    ConfigProvider configProvider;
    @InjectMock
    Helper helper;
    @InjectMock
    AuditExceptionProcessor auditExceptionProcessor;

    @BeforeEach
    void resetMocks() {
        mocked.getGetILoqKey().reset();
    }

    @Test
    @DisplayName("buildNewILoqKey - Key mandatory default fields")
    void testShouldMapTheDefaultFieldsForILoqKey() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        ILoqKeyImport iLoqKeyImport = iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        assertThat(iLoqKeyImport.getKey().getKeyTypeMask()).isEqualTo(264);
        assertThat(iLoqKeyImport.getKey().getManufacturingInfo()).isEmpty();
        assertThat(iLoqKeyImport.getKey().getOnlineAccessCode()).isEmpty();
        assertThat(iLoqKeyImport.getKey().getRomId()).isEmpty();
        assertThat(iLoqKeyImport.getKey().getStamp()).isEmpty();
        assertThat(iLoqKeyImport.getKey().getTagKey()).isEmpty();
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.FNKey_ID")
    void testShouldCreateAGUIDForKeyId() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String expectedFNKeyId = "7cc7b0e3-733e-4537-aeae-67da59f128b1";
        when(helper.createGUID()).thenReturn(expectedFNKeyId);

        verifyNoInteractions(helper);

        ILoqKeyImport iLoqKeyImport = iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        verify(helper).createGUID();
        assertThat(iLoqKeyImport.getKey().getFnKeyId()).isEqualTo(expectedFNKeyId);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.Description")
    void testShouldMapTheDescription() throws Exception {
        String streetAddressName = "Malminkatu 3, 00100, Helsinki";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withStreetAddress("123", streetAddressName)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        ILoqKeyImport iLoqKeyImport = iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        assertThat(iLoqKeyImport.getKey().getDescription()).isEqualTo(streetAddressName);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.InfoText")
    void testShouldMapTheInfoText() throws Exception {
        String expectedInfoText = "KEY-00123";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withKeyEfecteId(expectedInfoText)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        ILoqKeyImport iLoqKeyImport = iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        assertThat(iLoqKeyImport.getKey().getInfoText()).isEqualTo(expectedInfoText);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.ExpireDate")
    void testShouldMapTheExpireDate() throws Exception {
        String keyValidityDate = "20.05.2025 00:00";
        String expectedExpireDate = "2025-05-20T00:00:00Z";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withValidityDate(keyValidityDate)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        ILoqKeyImport iLoqKeyImport = iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        assertThat(iLoqKeyImport.getKey().getExpireDate()).isEqualTo(expectedExpireDate);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.RealEstate_ID")
    void testShouldResolveILoqRealEstateId() throws Exception {
        String expectedEfecteAddressEntityId = "1234";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withStreetAddress(expectedEfecteAddressEntityId, "Testikatu 1, 00100, Helsinki")
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        verifyNoInteractions(configProvider);

        iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        verify(configProvider).getILoqRealEstateIdByEfecteAddressEntityId(expectedEfecteAddressEntityId);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.RealEstate_ID")
    void testShouldMapTheResolvedRealEstateId() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String expectedRealEstateId = "123";

        when(configProvider.getILoqRealEstateIdByEfecteAddressEntityId(any()))
                .thenReturn(expectedRealEstateId);

        ILoqKeyImport iLoqKeyImport = iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        assertThat(iLoqKeyImport.getKey().getRealEstateId()).isEqualTo(expectedRealEstateId);
    }

    @Test
    @DisplayName("buildNewILoqKey - SecurityAccessIds")
    void testShouldResolveILoqSecurityAccessIdsForTheKey() throws Exception {
        String expectedSecurityAccessEntityId1 = "1";
        String expectedSecurityAccessEntityId2 = "2";
        EfecteReference reference1 = new EfecteReference(expectedSecurityAccessEntityId1,
                "valid security access 1");
        EfecteReference reference2 = new EfecteReference(expectedSecurityAccessEntityId2,
                "valid security access 2");
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withSecurityAccesses(reference1, reference2)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        verifyNoInteractions(configProvider);

        iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        verify(configProvider)
                .getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(expectedSecurityAccessEntityId1);
        verify(configProvider)
                .getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(expectedSecurityAccessEntityId2);
    }

    @Test
    @DisplayName("buildNewILoqKey - SecurityAccessIds")
    void testShouldMapTheResolvedSecurityAccessIds() throws Exception {
        EfecteReference reference1 = new EfecteReference("1", "Valid security access 1");
        EfecteReference reference2 = new EfecteReference("2", "Valid security access 2");
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withSecurityAccesses(reference1, reference2)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String securityAccessId1 = "123";
        String securityAccessId2 = "456";
        List<String> expectedSecurityAccessIds = List.of(securityAccessId1, securityAccessId2);

        when(configProvider.getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(any()))
                .thenReturn(securityAccessId1)
                .thenReturn(securityAccessId2);

        ILoqKeyImport iLoqKeyImport = iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        assertThat(iLoqKeyImport.getSecurityAccessIds()).isEqualTo(expectedSecurityAccessIds);
    }

    @Test
    @DisplayName("buildNewILoqKey - ZoneIds")
    void testShouldResolveILoqZoneIds() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withSecurityAccesses(new EfecteReference("123", "Valid security access"))
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String iLoqSecurityAccessId = "1";
        when(configProvider.getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(any()))
                .thenReturn(iLoqSecurityAccessId);

        verifyNoInteractions(configProvider);

        iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        verify(configProvider).getILoqZoneIdByILoqSecurityAccessId(iLoqSecurityAccessId);
    }

    @Test
    @DisplayName("buildNewILoqKey - ZoneIds")
    void testShouldMapTheResolvedZoneIds() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String zoneId = "123";
        List<String> expectedZoneIds = List.of(zoneId);

        when(configProvider.getILoqZoneIdByILoqSecurityAccessId(any()))
                .thenReturn(zoneId);

        ILoqKeyImport iLoqKeyImport = iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        assertThat(iLoqKeyImport.getZoneIds()).isEqualTo(expectedZoneIds);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.Person_ID")
    void testShouldResolveILoqPersonId_KeyHolder() throws Exception {
        String keyHolderEntityId = "12345";
        EfecteReference expectedReference = new EfecteReference(keyHolderEntityId, "Doe John");
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withKeyHolderReference(expectedReference)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        verifyNoInteractions(iLoqPersonResolver);

        iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        verify(iLoqPersonResolver).resolveILoqPersonId(keyHolderEntityId);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.Person_ID")
    void testShouldResolveILoqPersonId_Outsider() throws Exception {
        String outsiderName = "John Smith";
        String outsiderEmail = "john.smith@outsider.com";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(outsiderName)
                .withOutsiderEmail(outsiderEmail)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        verifyNoInteractions(iLoqPersonResolver);

        iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        verify(iLoqPersonResolver).resolveILoqPersonIdForOutsider(outsiderEmail, outsiderName);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.Person_ID")
    void testShouldMapTheResolvedPersonId_KeyHolder() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String expectedPersonId = "123";

        when(iLoqPersonResolver.resolveILoqPersonId(anyString()))
                .thenReturn(expectedPersonId);

        ILoqKeyImport iLoqKeyImport = iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        assertThat(iLoqKeyImport.getKey().getPersonId()).isEqualTo(expectedPersonId);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.Person_ID")
    void testShouldMapTheResolvedPersonId_Outsider() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String expectedPersonId = "123";

        when(iLoqPersonResolver.resolveILoqPersonIdForOutsider(anyString(), anyString()))
                .thenReturn(expectedPersonId);

        ILoqKeyImport iLoqKeyImport = iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        assertThat(iLoqKeyImport.getKey().getPersonId()).isEqualTo(expectedPersonId);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.Person_ID")
    void testShouldCreateTheMissingPerson_KeyHolder() throws Exception {
        EfecteEntity expectedEfecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String zoneId = "123";
        List<String> expectedZoneIds = List.of(zoneId);

        when(configProvider.getILoqZoneIdByILoqSecurityAccessId(any())).thenReturn(zoneId);
        when(iLoqPersonResolver.resolveILoqPersonId(anyString())).thenReturn(null);

        verifyNoInteractions(iLoqPersonProcessor);

        iLoqKeyMapper.buildNewILoqKey(expectedEfecteEntity);

        verify(iLoqPersonProcessor).createILoqPerson(expectedEfecteEntity, expectedZoneIds);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.Person_ID")
    void testShouldCreateTheMissingPerson_Outsider() throws Exception {
        EfecteEntity expectedEfecteEntity = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String zoneId = "123";
        List<String> expectedZoneIds = List.of(zoneId);

        when(configProvider.getILoqZoneIdByILoqSecurityAccessId(any())).thenReturn(zoneId);
        when(iLoqPersonResolver.resolveILoqPersonIdForOutsider(anyString(), anyString())).thenReturn(null);

        verifyNoInteractions(iLoqPersonProcessor);

        iLoqKeyMapper.buildNewILoqKey(expectedEfecteEntity);

        verify(iLoqPersonProcessor).createILoqPerson(expectedEfecteEntity, expectedZoneIds);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.Person_ID")
    void testShouldReturnTheCreatedILoqPersonId_KeyHolder() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String expectedILoqPersonId = "12345";

        when(iLoqPersonResolver.resolveILoqPersonId(anyString())).thenReturn(null);
        when(iLoqPersonProcessor.createILoqPerson(any(), any())).thenReturn(expectedILoqPersonId);

        ILoqKeyImport iLoqKeyImport = iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        assertThat(iLoqKeyImport.getKey().getPersonId()).isEqualTo(expectedILoqPersonId);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.Person_ID")
    void testShouldReturnTheCreatedILoqPersonId_Outsider() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String expectedILoqPersonId = "12345";

        when(iLoqPersonResolver.resolveILoqPersonIdForOutsider(anyString(), anyString())).thenReturn(null);
        when(iLoqPersonProcessor.createILoqPerson(any(), any())).thenReturn(expectedILoqPersonId);

        ILoqKeyImport iLoqKeyImport = iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        assertThat(iLoqKeyImport.getKey().getPersonId()).isEqualTo(expectedILoqPersonId);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.Person_ID")
    void testShouldThrowAnAuditExceptionWhenTheAuditMessageHasBeenSet_KeyHolder() throws Exception {
        String entityId = "123";
        String efecteId = "KEY-0001";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withId(entityId)
                .withKeyEfecteId(efecteId)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String iLoqPersonId = null;
        String auditMessage = "foobar";

        when(iLoqPersonResolver.resolveILoqPersonId(anyString())).thenReturn(null);
        when(redis.get(ri.getAuditMessagePrefix())).thenReturn(auditMessage);

        verifyNoInteractions(auditExceptionProcessor);

        iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        verify(redis).del(ri.getAuditMessagePrefix());
        verify(auditExceptionProcessor).throwAuditException(
                EnumDirection.EFECTE,
                EnumDirection.ILOQ,
                entityId,
                efecteId,
                iLoqPersonId,
                auditMessage);
    }

    @Test
    @DisplayName("buildNewILoqKey - Key.Person_ID")
    void testShouldThrowAnAuditExceptionWhenTheAuditMessageHasBeenSet_Outsider() throws Exception {
        String entityId = "123";
        String efecteId = "KEY-0001";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withId(entityId)
                .withKeyEfecteId(efecteId)
                .withIsOutsider(true)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String iLoqPersonId = null;
        String auditMessage = "foobar";

        when(iLoqPersonResolver.resolveILoqPersonIdForOutsider(anyString(), anyString())).thenReturn(null);
        when(redis.get(ri.getAuditMessagePrefix())).thenReturn(auditMessage);

        verifyNoInteractions(auditExceptionProcessor);

        iLoqKeyMapper.buildNewILoqKey(efecteEntity);

        verify(redis).del(ri.getAuditMessagePrefix());
        verify(auditExceptionProcessor).throwAuditException(
                EnumDirection.EFECTE,
                EnumDirection.ILOQ,
                entityId,
                efecteId,
                iLoqPersonId,
                auditMessage);
    }

    /////////////////////
    // iLOQ KEY UPDATE //
    /////////////////////

    @Test
    @DisplayName("buildUpdatedILoqKey - Efecte -> iLOQ")
    void testShouldFetchTheCurrentILoqKey() throws Exception {
        String expectedILoqKeyId = "abc-123";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withExternalId(expectedILoqKeyId)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        mocked.getGetILoqKey()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(new ILoqKeyResponse()));

        mocked.getGetILoqKey().expectedMessageCount(1);
        mocked.getGetILoqKey().expectedPropertyReceived("iLoqKeyId", expectedILoqKeyId);

        iLoqKeyMapper.buildUpdatedILoqKey(efecteEntity);

        mocked.getGetILoqKey().assertIsSatisfied();
    }

    @Test
    @DisplayName("buildUpdatedILoqKey - Efecte -> iLOQ")
    void testShouldThrowAnExceptionWhenFetchingTheILoqKeyFails() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withExternalId("abc-123")
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String message = "Oh no! An exception!";
        String expectedExceptionMessage = "ILoqKeyMapper: Fetching the iLOQ key failed: " + message;
        mocked.getGetILoqKey().whenAnyExchangeReceived(exchange -> {
            Exception exception = new Exception(message);
            exchange.setException(exception);
            exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);
        });

        assertThatThrownBy(() -> iLoqKeyMapper.buildUpdatedILoqKey(efecteEntity))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("buildUpdatedILoqKey - iLOQ -> Efecte")
    void testShouldNotFetchTheCurrentILoqKey() throws Exception {
        String expectedILoqKeyId = "abc-123";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withExternalId(expectedILoqKeyId)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        mocked.getGetILoqKey().expectedMessageCount(0);

        iLoqKeyMapper.buildUpdatedILoqKey(efecteEntity, new ILoqKeyResponse());

        mocked.getGetILoqKey().assertIsSatisfied();
    }

    @Test
    @DisplayName("buildUpdatedILoqKey - Efecte -> iLoq")
    void testShouldReturnAnILoqKeyImportWithUpdatedExpireDate_WithoutPrefetchedILoqKey() throws Exception {
        String iLoqKeyId = "abc-123";
        String efecteValidityDate = "20.05.2025 00:00";
        String expectedExpireDate = "2025-05-20T00:00:00Z";
        String infoText = "KEY_00123";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withExternalId(iLoqKeyId)
                .withKeyEfecteId(infoText)
                .withValidityDate(efecteValidityDate)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String description = "foo";
        String personId = "xyz-123";
        String realEstateId = "obv-345";
        String romId = "foo";
        String stamp = "bar";
        String tagkey = "baz";
        Integer state = 1;
        ILoqKeyResponse iLoqKeyResponse = new ILoqKeyResponse(iLoqKeyId);
        iLoqKeyResponse.setDescription(description);
        iLoqKeyResponse.setExpireDate(null);
        iLoqKeyResponse.setInfoText(null);
        iLoqKeyResponse.setPersonId(personId);
        iLoqKeyResponse.setRealEstateId(realEstateId);
        iLoqKeyResponse.setRomId(romId);
        iLoqKeyResponse.setStamp(stamp);
        iLoqKeyResponse.setTagKey(tagkey);
        iLoqKeyResponse.setState(state);

        ILoqKey expectedILoqKey = new ILoqKey(iLoqKeyId);
        expectedILoqKey.setDescription(description);
        expectedILoqKey.setExpireDate(expectedExpireDate);
        expectedILoqKey.setInfoText(infoText);
        expectedILoqKey.setPersonId(personId);
        expectedILoqKey.setRealEstateId(realEstateId);
        expectedILoqKey.setRomId(romId);
        expectedILoqKey.setStamp(stamp);
        expectedILoqKey.setTagKey(tagkey);
        expectedILoqKey.setState(state);

        mocked.getGetILoqKey().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(iLoqKeyResponse));

        ILoqKeyImport result = iLoqKeyMapper.buildUpdatedILoqKey(efecteEntity);

        assertThat(result.getKey()).isEqualTo(expectedILoqKey);
        assertThat(result.getSecurityAccessIds()).isNull();
        assertThat(result.getZoneIds()).isNull();
    }

    @Test
    @DisplayName("buildUpdatedILoqKey - iLOQ -> Efecte")
    void testShouldReturnAnILoqKeyImportWithUpdatedExpireDate_WithPrefetchedILoqKey() throws Exception {
        String iLoqKeyId = "abc-123";
        String efecteValidityDate = "20.05.2025 00:00";
        String expectedExpireDate = "2025-05-20T00:00:00Z";
        String infoText = "KEY_00123";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withExternalId(iLoqKeyId)
                .withKeyEfecteId(infoText)
                .withValidityDate(efecteValidityDate)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String description = "foo";
        String personId = "xyz-123";
        String realEstateId = "obv-345";
        String romId = "foo";
        String stamp = "bar";
        String tagkey = "baz";
        Integer state = 1;
        ILoqKeyResponse iLoqKeyResponse = new ILoqKeyResponse(iLoqKeyId);
        iLoqKeyResponse.setDescription(description);
        iLoqKeyResponse.setExpireDate(null);
        iLoqKeyResponse.setInfoText(null);
        iLoqKeyResponse.setPersonId(personId);
        iLoqKeyResponse.setRealEstateId(realEstateId);
        iLoqKeyResponse.setRomId(romId);
        iLoqKeyResponse.setStamp(stamp);
        iLoqKeyResponse.setTagKey(tagkey);
        iLoqKeyResponse.setState(state);

        ILoqKey expectedILoqKey = new ILoqKey(iLoqKeyId);
        expectedILoqKey.setDescription(description);
        expectedILoqKey.setExpireDate(expectedExpireDate);
        expectedILoqKey.setInfoText(infoText);
        expectedILoqKey.setPersonId(personId);
        expectedILoqKey.setRealEstateId(realEstateId);
        expectedILoqKey.setRomId(romId);
        expectedILoqKey.setStamp(stamp);
        expectedILoqKey.setTagKey(tagkey);
        expectedILoqKey.setState(state);

        ILoqKeyImport result = iLoqKeyMapper.buildUpdatedILoqKey(efecteEntity, iLoqKeyResponse);

        assertThat(result.getKey()).isEqualTo(expectedILoqKey);
        assertThat(result.getSecurityAccessIds()).isNull();
        assertThat(result.getZoneIds()).isNull();
    }

    @Test
    @DisplayName("buildUpdatedILoqSecurityAccesses")
    void testShouldResolveTheILoqSecurityAccessIdForEachEfecteSecurityAccessEntityId() throws Exception {
        String id1 = "1";
        String id2 = "2";
        String id3 = "3";

        Set<String> efecteSecurityAccessEntityIds = Set.of(id1, id2, id3);

        verifyNoInteractions(configProvider);

        iLoqKeyMapper.buildUpdatedILoqSecurityAccesses(efecteSecurityAccessEntityIds);

        verify(configProvider)
                .getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(id2);
        verify(configProvider)
                .getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(id3);
        verify(configProvider)
                .getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(id3);
    }

    @Test
    @DisplayName("buildUpdatedILoqSecurityAccesses")
    void testShouldReturnTheResolvedILoqSecurityAccessIds() throws Exception {
        String id1 = "1";
        String id2 = "2";
        String id3 = "3";

        Set<String> efecteSecurityAccessEntityIds = Set.of(id1, id2, id3);

        String iLoqId1 = "abc-1";
        String iLoqId2 = "xyz-2";
        String iLoqId3 = "ict-3";

        when(configProvider.getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(id1)).thenReturn(iLoqId1);
        when(configProvider.getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(id2)).thenReturn(iLoqId2);
        when(configProvider.getILoqSecurityAccessIdByEfecteSecurityAccessEntityId(id3)).thenReturn(iLoqId3);

        Set<String> result = iLoqKeyMapper.buildUpdatedILoqSecurityAccesses(efecteSecurityAccessEntityIds);

        assertThat(result).containsExactlyInAnyOrder(iLoqId1, iLoqId2, iLoqId3);
    }

    @Test
    void testaus() {
        Objects.equals(null, null);
    }
}
