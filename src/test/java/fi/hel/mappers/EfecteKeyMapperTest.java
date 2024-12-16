package fi.hel.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.configurations.ConfigProvider;
import fi.hel.models.EfecteAttributeImport;
import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.EfecteEntityImport;
import fi.hel.models.EfecteEntitySetImport;
import fi.hel.models.EnrichedILoqKey;
import fi.hel.models.ILoqSecurityAccess;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import fi.hel.processors.AuditExceptionProcessor;
import fi.hel.processors.Helper;
import fi.hel.processors.ResourceInjector;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class EfecteKeyMapperTest {

    @Inject
    ResourceInjector ri;
    @Inject
    EfecteKeyMapper efecteKeyMapper;
    @Inject
    TestUtils testUtils;
    @InjectMock
    Redis redis;
    @InjectMock
    AuditExceptionProcessor auditExceptionProcessor;
    @InjectMock
    ConfigProvider configProvider;
    @InjectMock
    Helper helper;

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldGetTheKeyHolderEfecteIdentifierFromRedis() throws Exception {
        String iLoqPersonId = "abc-123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setPersonId(iLoqPersonId);
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        when(helper.writeAsPojo(any(), any()))
                .thenReturn(new EfecteEntityIdentifier("irrelevant", "irrelevant"));

        String expectedPrefix = ri.getMappedPersonILoqPrefix() + iLoqPersonId;

        verifyNoInteractions(redis);

        efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey);

        verify(redis).get(expectedPrefix);
    }

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldThrowAnAuditExceptionWhenAnEfecteIdentifierCouldNotBeFoundAtRedis() throws Exception {
        String iLoqPersonId = "abc-123";
        String iLoqKeyId = "xyz-456";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(iLoqKeyId);
        enrichedILoqKey.setPersonId(iLoqPersonId);

        String expectedAuditMessage = "Unable to create a new key card in Efecte. No matching key holder was found for the specified iLOQ person (%s)."
                .formatted(iLoqPersonId);

        when(redis.get(anyString())).thenReturn(null);
        doAnswer(i -> {
            throw new Exception();
        }).when(auditExceptionProcessor).throwAuditException(any(), any(), any(), any(), any(), any());

        verifyNoInteractions(auditExceptionProcessor);

        try {
            efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey);
        } catch (Exception e) {
            verify(auditExceptionProcessor).throwAuditException(
                    EnumDirection.ILOQ, EnumDirection.EFECTE, null, null, iLoqKeyId,
                    expectedAuditMessage);
        }

    }

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldResolveTheEfecteStreetAddress() throws Exception {
        String iLoqPersonId = "abc-123";
        String expectedILoqRealEstateId = "xyz-456";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setPersonId(iLoqPersonId);
        enrichedILoqKey.setRealEstateId(expectedILoqRealEstateId);
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        when(redis.get(anyString())).thenReturn("irrelevant but not null");
        when(helper.writeAsPojo(any(), any()))
                .thenReturn(new EfecteEntityIdentifier("irrelevant", "irrelevant"));

        verifyNoInteractions(configProvider);

        efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey);

        verify(configProvider).getEfecteAddressEfecteIdByILoqRealEstateId(expectedILoqRealEstateId);
    }

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldSetTheResolvedEfecteStreetAddressToTheEntitySet() throws Exception {
        String iLoqPersonId = "abc-123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setPersonId(iLoqPersonId);
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        String expectedStreedAddressEfecteId = "LOC_00123";

        when(redis.get(anyString())).thenReturn("irrelevant but not null");
        when(configProvider.getEfecteAddressEfecteIdByILoqRealEstateId(any()))
                .thenReturn(expectedStreedAddressEfecteId);
        when(helper.writeAsPojo(any(), any()))
                .thenReturn(new EfecteEntityIdentifier("irrelevant", "irrelevant"));

        EfecteEntitySetImport result = efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey);

        EfecteEntityImport newKeyCard = result.getEntity();
        EfecteAttributeImport streetAddressAttribute = newKeyCard
                .getAttributeByType(EnumEfecteAttribute.KEY_STREET_ADDRESS);

        assertThat(streetAddressAttribute.getCode())
                .isEqualTo(EnumEfecteAttribute.KEY_STREET_ADDRESS.getCode());
        assertThat(streetAddressAttribute.getValues().size()).isEqualTo(1);
        assertThat(streetAddressAttribute.getValues().get(0)).isEqualTo(expectedStreedAddressEfecteId);
    }

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldResolveTheEfecteSecurityAccesses_BuildNewEfecteEntitySet() throws Exception {
        String iLoqPersonId = "abc-123";
        String iLoqSecurityAccessId1 = "xyz-123";
        String iLoqSecurityAccessId2 = "zyx-321";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setPersonId(iLoqPersonId);
        enrichedILoqKey.setSecurityAccesses(Set.of(
                new ILoqSecurityAccess(iLoqSecurityAccessId1),
                new ILoqSecurityAccess(iLoqSecurityAccessId2)));

        when(redis.get(anyString())).thenReturn("irrelevant but not null");
        when(helper.writeAsPojo(any(), any()))
                .thenReturn(new EfecteEntityIdentifier("irrelevant", "irrelevant"));

        verifyNoInteractions(configProvider);

        efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey);

        verify(configProvider).getEfecteSecurityAccessEfecteIdByILoqSecurityAccessId(iLoqSecurityAccessId1);
        verify(configProvider).getEfecteSecurityAccessEfecteIdByILoqSecurityAccessId(iLoqSecurityAccessId2);
    }

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldSetTheResolvedEfecteSecurityAccessesToTheEntitySet_BuildNewEfecteEntitySet() throws Exception {
        String iLoqPersonId = "abc-123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setPersonId(iLoqPersonId);
        enrichedILoqKey.setSecurityAccesses(Set.of(
                new ILoqSecurityAccess("irrelevant1"),
                new ILoqSecurityAccess("irrelevant2")));

        String expectedSecurityAccessEfecteId1 = "AA_00123";
        String expectedSecurityAccessEfecteId2 = "AA_00456";

        when(redis.get(anyString())).thenReturn("irrelevant but not null");
        when(configProvider.getEfecteSecurityAccessEfecteIdByILoqSecurityAccessId(any()))
                .thenReturn(expectedSecurityAccessEfecteId1)
                .thenReturn(expectedSecurityAccessEfecteId2);
        when(helper.writeAsPojo(any(), any()))
                .thenReturn(new EfecteEntityIdentifier("irrelevant", "irrelevant"));

        EfecteEntitySetImport result = efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey);

        EfecteEntityImport newKeyCard = result.getEntity();
        EfecteAttributeImport securityAccessAttribute = newKeyCard
                .getAttributeByType(EnumEfecteAttribute.KEY_SECURITY_ACCESS);

        assertThat(securityAccessAttribute.getCode())
                .isEqualTo(EnumEfecteAttribute.KEY_SECURITY_ACCESS.getCode());
        assertThat(securityAccessAttribute.getValues().size()).isEqualTo(2);
        assertThat(securityAccessAttribute.getValues().get(0)).isEqualTo(expectedSecurityAccessEfecteId1);
        assertThat(securityAccessAttribute.getValues().get(1)).isEqualTo(expectedSecurityAccessEfecteId2);
    }

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldSetTheEfecteKeyHolderToTheEntitySet() throws Exception {
        String iLoqPersonId = "abc-123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setPersonId(iLoqPersonId);
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        String expectedEfecteId = "PER_00123";
        String efecteEntityIdentifierJson = """
                {
                    "entityId": "irrelevant",
                    "efecteid": "%s"
                }
                """.formatted(expectedEfecteId);

        when(redis.get(anyString())).thenReturn(efecteEntityIdentifierJson);
        when(helper.writeAsPojo(efecteEntityIdentifierJson, EfecteEntityIdentifier.class))
                .thenReturn(new EfecteEntityIdentifier("irrelevant", expectedEfecteId));

        verifyNoInteractions(helper);

        EfecteEntitySetImport result = efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey);

        EfecteEntityImport newKeyCard = result.getEntity();
        EfecteAttributeImport keyHolderAttribute = newKeyCard.getAttributeByType(EnumEfecteAttribute.KEY_HOLDER);

        verify(helper).writeAsPojo(efecteEntityIdentifierJson, EfecteEntityIdentifier.class);
        assertThat(keyHolderAttribute.getCode()).isEqualTo(EnumEfecteAttribute.KEY_HOLDER.getCode());
        assertThat(keyHolderAttribute.getValues().size()).isEqualTo(1);
        assertThat(keyHolderAttribute.getValues().get(0)).isEqualTo(expectedEfecteId);
    }

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldSetTheExternalIdAttribute_BuildNewEfecteEntitySet() throws Exception {
        String iLoqPersonId = "abc-123";
        String expectedILoqKeyId = "xyz-123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey(expectedILoqKeyId);
        enrichedILoqKey.setPersonId(iLoqPersonId);
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        when(redis.get(anyString())).thenReturn("irrelevant");
        when(helper.writeAsPojo(any(), any())).thenReturn(new EfecteEntityIdentifier());

        EfecteEntitySetImport result = efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey);

        EfecteEntityImport newKeyCard = result.getEntity();
        EfecteAttributeImport externalIdAttribute = newKeyCard.getAttributeByType(EnumEfecteAttribute.KEY_EXTERNAL_ID);

        assertThat(externalIdAttribute.getCode()).isEqualTo(EnumEfecteAttribute.KEY_EXTERNAL_ID.getCode());
        assertThat(externalIdAttribute.getValues().size()).isEqualTo(1);
        assertThat(externalIdAttribute.getValues().get(0)).isEqualTo(expectedILoqKeyId);
    }

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldSetTheStateAttribute_BuildNewEfecteEntitySet() throws Exception {
        String iLoqPersonId = "abc-123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setPersonId(iLoqPersonId);
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        when(redis.get(anyString())).thenReturn("irrelevant");
        when(helper.writeAsPojo(any(), any())).thenReturn(new EfecteEntityIdentifier());

        EfecteEntitySetImport result = efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey);

        EfecteEntityImport newKeyCard = result.getEntity();
        EfecteAttributeImport stateAttribute = newKeyCard.getAttributeByType(EnumEfecteAttribute.KEY_STATE);

        assertThat(stateAttribute.getCode()).isEqualTo(EnumEfecteAttribute.KEY_STATE.getCode());
        assertThat(stateAttribute.getValues().size()).isEqualTo(1);
        assertThat(stateAttribute.getValues().get(0)).isEqualTo("Aktiivinen");
    }

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldSetTheTypeAttribute_BuildNewEfecteEntitySet() throws Exception {
        String iLoqPersonId = "abc-123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setPersonId(iLoqPersonId);
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        when(redis.get(anyString())).thenReturn("irrelevant");
        when(helper.writeAsPojo(any(), any())).thenReturn(new EfecteEntityIdentifier());

        EfecteEntitySetImport result = efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey);

        EfecteEntityImport newKeyCard = result.getEntity();
        EfecteAttributeImport keyTypeAttribute = newKeyCard.getAttributeByType(EnumEfecteAttribute.KEY_TYPE);

        assertThat(keyTypeAttribute.getCode()).isEqualTo(EnumEfecteAttribute.KEY_TYPE.getCode());
        assertThat(keyTypeAttribute.getValues().size()).isEqualTo(1);
        assertThat(keyTypeAttribute.getValues().get(0)).isEqualTo("iLOQ");
    }

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldSetTheValidityDateAttribute_BuildNewEfecteEntitySet() throws Exception {
        String iLoqPersonId = "abc-123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setPersonId(iLoqPersonId);
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        when(redis.get(anyString())).thenReturn("irrelevant");
        when(helper.writeAsPojo(any(), any())).thenReturn(new EfecteEntityIdentifier());

        String datePattern = "dd.MM.yyyy";
        String expectedValidityDate = testUtils.createDatePlusDays(
                testUtils.createDatetimeNow(datePattern), 365, datePattern) + " 00:00";

        EfecteEntitySetImport result = efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey);

        EfecteEntityImport newKeyCard = result.getEntity();
        EfecteAttributeImport keyValidityDateAttribute = newKeyCard
                .getAttributeByType(EnumEfecteAttribute.KEY_VALIDITY_DATE);

        assertThat(keyValidityDateAttribute.getCode()).isEqualTo(EnumEfecteAttribute.KEY_VALIDITY_DATE.getCode());
        assertThat(keyValidityDateAttribute.getValues().size()).isEqualTo(1);
        assertThat(keyValidityDateAttribute.getValues().get(0)).isEqualTo(expectedValidityDate);
    }

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldSetTheKeyTemplateCode() throws Exception {
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setPersonId("irrelevant");
        enrichedILoqKey.setSecurityAccesses(Set.of(new ILoqSecurityAccess("irrelevant")));

        when(redis.get(anyString())).thenReturn("irrelevant here");
        when(helper.writeAsPojo(any(), any())).thenReturn(new EfecteEntityIdentifier("irrelevant", "irrelevant"));

        EfecteEntitySetImport result = efecteKeyMapper.buildNewEfecteEntitySetImport(enrichedILoqKey);

        EfecteEntityImport newKeyCard = result.getEntity();

        assertThat(newKeyCard.getTemplate().getCode()).isEqualTo(EnumEfecteTemplate.KEY.getCode());
    }

    //////////////////////////////////////////////
    // buildEfecteEntitySetUpdate  - Mapped key //
    //////////////////////////////////////////////

    @Test
    @DisplayName("buildEfecteEntitySetUpdate")
    void testShouldResolveTheEfecteSecurityAccesses_BuildEfecteEntitySetUpdate() throws Exception {
        String iLoqSecurityAccessId1 = "xyz-123";
        String iLoqSecurityAccessId2 = "zyx-321";
        String keyEfecteId = "KEY_000123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setSecurityAccesses(Set.of(
                new ILoqSecurityAccess(iLoqSecurityAccessId1),
                new ILoqSecurityAccess(iLoqSecurityAccessId2)));

        verifyNoInteractions(configProvider);

        efecteKeyMapper.buildEfecteEntitySetUpdate(enrichedILoqKey, keyEfecteId);

        verify(configProvider).getEfecteSecurityAccessEfecteIdByILoqSecurityAccessId(iLoqSecurityAccessId1);
        verify(configProvider).getEfecteSecurityAccessEfecteIdByILoqSecurityAccessId(iLoqSecurityAccessId2);
    }

    @Test
    @DisplayName("buildNewEfecteEntitySet")
    void testShouldSetTheResolvedEfecteSecurityAccessesToTheEntitySet_BuildEfecteEntitySetUpdate() throws Exception {
        String keyEfecteId = "KEY_000123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setSecurityAccesses(Set.of(
                new ILoqSecurityAccess("irrelevant1"),
                new ILoqSecurityAccess("irrelevant2")));

        String expectedSecurityAccessEfecteId1 = "AA_00123";
        String expectedSecurityAccessEfecteId2 = "AA_00456";

        when(configProvider.getEfecteSecurityAccessEfecteIdByILoqSecurityAccessId(any()))
                .thenReturn(expectedSecurityAccessEfecteId1)
                .thenReturn(expectedSecurityAccessEfecteId2);

        EfecteEntitySetImport result = efecteKeyMapper.buildEfecteEntitySetUpdate(enrichedILoqKey, keyEfecteId);

        EfecteEntityImport updatedKeyCard = result.getEntity();
        EfecteAttributeImport securityAccessAttribute = updatedKeyCard
                .getAttributeByType(EnumEfecteAttribute.KEY_SECURITY_ACCESS);

        assertThat(securityAccessAttribute.getCode())
                .isEqualTo(EnumEfecteAttribute.KEY_SECURITY_ACCESS.getCode());
        assertThat(securityAccessAttribute.getValues().size()).isEqualTo(2);
        assertThat(securityAccessAttribute.getValues().get(0)).isEqualTo(expectedSecurityAccessEfecteId1);
        assertThat(securityAccessAttribute.getValues().get(1)).isEqualTo(expectedSecurityAccessEfecteId2);
    }

    @Test
    @DisplayName("buildEfecteEntitySetUpdate")
    void testShouldSetTheEfecteIdAttribute_MappedKey() throws Exception {
        String iLoqSecurityAccessId1 = "xyz-123";
        String iLoqSecurityAccessId2 = "zyx-321";
        String expectedEfecteId = "KEY_000123";
        EnrichedILoqKey enrichedILoqKey = new EnrichedILoqKey();
        enrichedILoqKey.setSecurityAccesses(Set.of(
                new ILoqSecurityAccess(iLoqSecurityAccessId1),
                new ILoqSecurityAccess(iLoqSecurityAccessId2)));

        EfecteEntitySetImport result = efecteKeyMapper.buildEfecteEntitySetUpdate(enrichedILoqKey, expectedEfecteId);

        EfecteEntityImport updatedKeyCard = result.getEntity();
        EfecteAttributeImport efecteIdAttribute = updatedKeyCard
                .getAttributeByType(EnumEfecteAttribute.KEY_EFECTE_ID);

        assertThat(efecteIdAttribute.getCode())
                .isEqualTo(EnumEfecteAttribute.KEY_EFECTE_ID.getCode());
        assertThat(efecteIdAttribute.getValues().size()).isEqualTo(1);
        assertThat(efecteIdAttribute.getValues().get(0)).isEqualTo(expectedEfecteId);
    }

    ////////////////////////////////////////////////
    // buildEfecteEntitySetUpdate  - Unmapped key //
    ////////////////////////////////////////////////

    @Test
    @DisplayName("buildEfecteEntitySetUpdate")
    void testShouldSetTheExternalIdAttribute_BuildEfecteEntitySetUpdate() throws Exception {
        String expectedILoqKeyId = "xyz-123";
        String keyEfecteId = "KEY_000123";

        when(redis.get(anyString())).thenReturn("irrelevant");
        when(helper.writeAsPojo(any(), any())).thenReturn(new EfecteEntityIdentifier());

        EfecteEntitySetImport result = efecteKeyMapper.buildEfecteEntitySetUpdate(expectedILoqKeyId, keyEfecteId);

        EfecteEntityImport newKeyCard = result.getEntity();
        EfecteAttributeImport keyHolderAttribute = newKeyCard.getAttributeByType(EnumEfecteAttribute.KEY_EXTERNAL_ID);

        assertThat(keyHolderAttribute.getCode()).isEqualTo(EnumEfecteAttribute.KEY_EXTERNAL_ID.getCode());
        assertThat(keyHolderAttribute.getValues().size()).isEqualTo(1);
        assertThat(keyHolderAttribute.getValues().get(0)).isEqualTo(expectedILoqKeyId);
    }

    @Test
    @DisplayName("buildEfecteEntitySetUpdate")
    void testShouldSetTheStateAttribute_BuildEfecteEntitySetUpdate() throws Exception {
        String keyEfecteId = "KEY_000123";

        when(redis.get(anyString())).thenReturn("irrelevant");
        when(helper.writeAsPojo(any(), any())).thenReturn(new EfecteEntityIdentifier());

        EfecteEntitySetImport result = efecteKeyMapper.buildEfecteEntitySetUpdate("iLoqKeyId", keyEfecteId);

        EfecteEntityImport newKeyCard = result.getEntity();
        EfecteAttributeImport keyHolderAttribute = newKeyCard.getAttributeByType(EnumEfecteAttribute.KEY_STATE);

        assertThat(keyHolderAttribute.getCode()).isEqualTo(EnumEfecteAttribute.KEY_STATE.getCode());
        assertThat(keyHolderAttribute.getValues().size()).isEqualTo(1);
        assertThat(keyHolderAttribute.getValues().get(0)).isEqualTo("Aktiivinen");
    }

    @Test
    @DisplayName("buildEfecteEntitySetUpdate")
    void testShouldSetTheEfecteIdAttribute_UnmappedKey() throws Exception {
        String expectedEfecteId = "KEY_000123";

        EfecteEntitySetImport result = efecteKeyMapper.buildEfecteEntitySetUpdate("iLoqKeyId", expectedEfecteId);

        EfecteEntityImport updatedKeyCard = result.getEntity();
        EfecteAttributeImport efecteIdAttribute = updatedKeyCard
                .getAttributeByType(EnumEfecteAttribute.KEY_EFECTE_ID);

        assertThat(efecteIdAttribute.getCode())
                .isEqualTo(EnumEfecteAttribute.KEY_EFECTE_ID.getCode());
        assertThat(efecteIdAttribute.getValues().size()).isEqualTo(1);
        assertThat(efecteIdAttribute.getValues().get(0)).isEqualTo(expectedEfecteId);
    }
}
