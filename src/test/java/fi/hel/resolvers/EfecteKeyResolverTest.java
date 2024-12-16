package fi.hel.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.configurations.ConfigProvider;
import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.EfecteReference;
import fi.hel.models.EnrichedILoqKey;
import fi.hel.models.ILoqSecurityAccess;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import fi.hel.processors.Helper;
import fi.hel.processors.ResourceInjector;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class EfecteKeyResolverTest extends CamelQuarkusTestSupport {

    @Inject
    ResourceInjector ri;
    @Inject
    TestUtils testUtils;
    @Inject
    MockEndpointInjector mocked;
    @Inject
    EfecteKeyResolver efecteKeyResolver;
    @InjectMock
    Redis redis;
    @InjectMock
    ConfigProvider configProvider;
    @InjectMock
    EfectePersonResolver efecteKeyHolderResolver;
    @InjectMock
    Helper helper;

    @BeforeEach
    void setup() {
        mocked.getGetILoqPerson().reset();
    }

    @Override
    protected void doPreSetup() throws Exception {
        super.doPostSetup();
        testConfiguration().withUseRouteBuilder(false);
    }

    @Test
    @DisplayName("buildEqualEfecteKey")
    void testShouldResolveTheEfecteKeyHolderEntityId() throws Exception {
        String expectedILoqPersonId = "irrelevant";
        String efecteAddress = "Testikatu 1, 00100, Helsinki";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        iLoqKey.setPersonId(expectedILoqPersonId);
        iLoqKey.setSecurityAccesses(Set.of());

        when(efecteKeyHolderResolver.resolveEfectePersonIdentifier(anyString()))
                .thenReturn("irrelevant");

        verifyNoInteractions(efecteKeyHolderResolver);

        efecteKeyResolver.buildEqualEfecteKey(iLoqKey, efecteAddress);

        verify(efecteKeyHolderResolver).resolveEfectePersonIdentifier(expectedILoqPersonId);
    }

    @Test
    @DisplayName("buildEqualEfecteKey")
    void testShouldSetTheResolvedEfecteKeyHolderEntityIdToTheResponseEntity() throws Exception {
        String personId = "irrelevant";
        String efecteAddress = "Testikatu 1, 00100, Helsinki";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        iLoqKey.setPersonId(personId);
        iLoqKey.setSecurityAccesses(Set.of());

        String expectedEfectePersonEntityId = "123456";

        when(efecteKeyHolderResolver.resolveEfectePersonIdentifier(personId))
                .thenReturn(expectedEfectePersonEntityId);

        EfecteEntity efecteEntity = efecteKeyResolver.buildEqualEfecteKey(iLoqKey, efecteAddress);

        EfecteReference keyHolderReference = efecteEntity
                .getAttributeReferences(EnumEfecteAttribute.KEY_HOLDER).get(0);

        assertThat(keyHolderReference.getId()).isEqualTo(expectedEfectePersonEntityId);
    }

    @Test
    @DisplayName("buildEqualEfecteKey")
    void testShouldSetTheResolvedEfecteOutsiderToTheResponseEntity() throws Exception {
        String personId = "irrelevant";
        String efecteAddress = "Testikatu 1, 00100, Helsinki";
        String expectedOutsiderName = "John Smith";
        String expectedOutsiderEmail = "john.smith@outsider.com";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        iLoqKey.setPersonId(personId);
        iLoqKey.setSecurityAccesses(Set.of());

        String efectePersonIdentifierValue = """
                {
                    "entityId": "",
                    "efecteId": "",
                    "outsiderName": "%s",
                    "outsiderEmail": "%s"
                }
                """.formatted(expectedOutsiderName, expectedOutsiderEmail);

        EfecteEntityIdentifier efecteEntityIdentifier = testUtils.writeAsPojo(efectePersonIdentifierValue,
                EfecteEntityIdentifier.class);

        when(efecteKeyHolderResolver.resolveEfectePersonIdentifier(personId))
                .thenReturn(efectePersonIdentifierValue);
        when(helper.writeAsPojo(efectePersonIdentifierValue, EfecteEntityIdentifier.class))
                .thenReturn(efecteEntityIdentifier);

        EfecteEntity efecteEntity = efecteKeyResolver.buildEqualEfecteKey(iLoqKey, efecteAddress);

        String isOutsider = efecteEntity
                .getAttributeValue(EnumEfecteAttribute.KEY_IS_OUTSIDER);
        String outsiderName = efecteEntity
                .getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_NAME);
        String outsiderEmail = efecteEntity
                .getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_EMAIL);

        assertThat(isOutsider).isEqualTo("Kyllä");
        assertThat(outsiderName).isEqualTo(expectedOutsiderName);
        assertThat(outsiderEmail).isEqualTo(expectedOutsiderEmail);
    }

    @Test
    @DisplayName("buildEqualEfecteKey")
    void testShouldSetTheResolvedEfecteOutsiderNameToTheResponseEntity() throws Exception {
        String personId = "irrelevant";
        String efecteAddress = "Testikatu 1, 00100, Helsinki";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        iLoqKey.setPersonId(personId);
        iLoqKey.setSecurityAccesses(Set.of());

        String expectedPersonIdentifierValue = "John Doe";

        when(efecteKeyHolderResolver.resolveEfectePersonIdentifier(personId))
                .thenReturn(expectedPersonIdentifierValue);

        EfecteEntity efecteEntity = efecteKeyResolver.buildEqualEfecteKey(iLoqKey, efecteAddress);

        String isOutsider = efecteEntity
                .getAttributeValue(EnumEfecteAttribute.KEY_IS_OUTSIDER);
        String outsiderName = efecteEntity
                .getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_NAME);

        assertThat(isOutsider).isEqualTo("Kyllä");
        assertThat(outsiderName).isEqualTo(expectedPersonIdentifierValue);
    }

    @Test
    @DisplayName("buildEqualEfecteKey")
    void testShouldGetTheMatchingEfecteSecurityAccessIdsFromCustomerConfiguration() throws Exception {
        String expectedILoqSecurityAccessId1 = "abc-123";
        String expectedILoqSecurityAccessId2 = "xyz-456";
        String personId = "irrelevant";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        iLoqKey.setPersonId(personId);
        iLoqKey.setSecurityAccesses(Set.of(
                new ILoqSecurityAccess(expectedILoqSecurityAccessId1),
                new ILoqSecurityAccess(expectedILoqSecurityAccessId2)));
        String expectedEfecteAddress = "Testikatu 1, 00100, Helsinki";

        when(efecteKeyHolderResolver.resolveEfectePersonIdentifier(anyString()))
                .thenReturn("irrelevant");
        when(redis.get(anyString())).thenReturn("something but not null person id");

        verifyNoInteractions(configProvider);

        efecteKeyResolver.buildEqualEfecteKey(iLoqKey, expectedEfecteAddress);

        verify(configProvider)
                .getEfecteSecurityAccessEntityIdByILoqSecurityAccessId(expectedILoqSecurityAccessId1);
        verify(configProvider)
                .getEfecteSecurityAccessEntityIdByILoqSecurityAccessId(expectedILoqSecurityAccessId2);
    }

    @Test
    @DisplayName("buildEqualEfecteKey")
    void testShouldSetTheSecurityAccessEntityIdsToTheResponseEntity()
            throws Exception {
        String iLoqSecurityAccessId1 = "abc-123";
        String iLoqSecurityAccessId2 = "xyz-456";
        String personId = "irrelevant";
        String efecteAddress = "Testikatu 1, 00100, Helsinki";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        iLoqKey.setPersonId(personId);
        iLoqKey.setSecurityAccesses(Set.of(
                new ILoqSecurityAccess(iLoqSecurityAccessId1),
                new ILoqSecurityAccess(iLoqSecurityAccessId2)));

        String expectedEfecteSecurityAccessEntityId1 = "efecte_sa_entity_id_1";
        String expectedEfecteSecurityAccessEntityId2 = "efecte_sa_entity_id_2";

        when(efecteKeyHolderResolver.resolveEfectePersonIdentifier(anyString()))
                .thenReturn("irrelevant");
        when(redis.get(anyString())).thenReturn("something but not null person id");
        when(configProvider.getEfecteSecurityAccessEntityIdByILoqSecurityAccessId(any()))
                .thenReturn(expectedEfecteSecurityAccessEntityId1)
                .thenReturn(expectedEfecteSecurityAccessEntityId2);

        EfecteEntity efecteEntity = efecteKeyResolver.buildEqualEfecteKey(iLoqKey, efecteAddress);

        List<EfecteReference> securityAccessReferences = efecteEntity
                .getAttributeReferences(EnumEfecteAttribute.KEY_SECURITY_ACCESS);

        assertThat(securityAccessReferences.size()).isEqualTo(iLoqKey.getSecurityAccesses().size());
        assertThat(securityAccessReferences.stream().map(s -> s.getId()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(
                        expectedEfecteSecurityAccessEntityId1,
                        expectedEfecteSecurityAccessEntityId2);
    }

    @Test
    @DisplayName("getNewEfecteSecurityAccessEntityIds")
    void testShouldReturnTheEfecteKeySecurityAccessEntityIds() throws Exception {
        String expectedSecurityAccessEntityId1 = "SA0001";
        String expectedSecurityAccessEntityId2 = "SA0002";
        String iLoqSecurityAccessId1 = "abc-123";
        String iLoqSecurityAccessId2 = "xyz-456";
        Set<ILoqSecurityAccess> expectedILoqSecurityAccesses = Set.of(
                new ILoqSecurityAccess(iLoqSecurityAccessId1),
                new ILoqSecurityAccess(iLoqSecurityAccessId2));

        when(configProvider.getEfecteSecurityAccessEntityIdByILoqSecurityAccessId(any()))
                .thenReturn(expectedSecurityAccessEntityId1)
                .thenReturn(expectedSecurityAccessEntityId2);

        verifyNoInteractions(configProvider);

        Set<String> result = efecteKeyResolver
                .getNewEfecteSecurityAccessEntityIds(expectedILoqSecurityAccesses);

        verify(configProvider).getEfecteSecurityAccessEntityIdByILoqSecurityAccessId(iLoqSecurityAccessId1);
        verify(configProvider).getEfecteSecurityAccessEntityIdByILoqSecurityAccessId(iLoqSecurityAccessId2);
        assertThat(result).containsExactlyInAnyOrder(
                expectedSecurityAccessEntityId1, expectedSecurityAccessEntityId2);
    }

    @Test
    @DisplayName("findMatchingEfecteKey - with key holder")
    void testShouldReturnTheMatchingKeyWithKeyHolder() throws Exception {
        String matchinKeyHolderEntityId = "123456";
        String matchingSecurityAccessEntityId1 = "234567";
        String matchingSecurityAccessEntityId2 = "987654";
        EfecteEntity builtEqualEfecteKey = new EfecteEntityBuilder()
                .withKeyHolderReference(matchinKeyHolderEntityId)
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId1),
                        new EfecteReference(matchingSecurityAccessEntityId2))
                .build();
        EfecteEntity nonMatchingKey1 = new EfecteEntityBuilder()
                .withKeyHolderReference("non-matching")
                .withSecurityAccesses(new EfecteReference("non-matching"))
                .build();
        EfecteEntity nonMatchingKey2 = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName("John Doe")
                .withSecurityAccesses(new EfecteReference("non-matching"))
                .build();
        EfecteEntity matchingKey = new EfecteEntityBuilder()
                .withKeyHolderReference(matchinKeyHolderEntityId, "Smith John")
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId2),
                        new EfecteReference(matchingSecurityAccessEntityId1))
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        List<EfecteEntity> keys = List.of(nonMatchingKey1, nonMatchingKey2, matchingKey);

        EfecteEntity result = efecteKeyResolver.findMatchingEfecteKey(builtEqualEfecteKey, keys);

        assertThat(result).isEqualTo(matchingKey);
    }

    @Test
    @DisplayName("findMatchingEfecteKey - with key holder")
    void testShouldReturnNullWhenNoKeyWithAMatchingKeyHolderIsFound() throws Exception {
        String matchinKeyHolderEntityId = "123456";
        String matchingSecurityAccessEntityId = "234567";
        EfecteEntity builtEqualEfecteKey = new EfecteEntityBuilder()
                .withKeyHolderReference(matchinKeyHolderEntityId)
                .withSecurityAccesses(new EfecteReference(matchingSecurityAccessEntityId))
                .build();
        EfecteEntity nonMatchingKey1 = new EfecteEntityBuilder()
                .withKeyHolderReference("non-matching")
                .withSecurityAccesses(new EfecteReference("non-matching"))
                .build();
        EfecteEntity nonMatchingKey2 = new EfecteEntityBuilder()
                .withKeyHolderReference("non-matching")
                .withSecurityAccesses(new EfecteReference(matchingSecurityAccessEntityId))
                .build();

        List<EfecteEntity> keys = List.of(nonMatchingKey1, nonMatchingKey2, nonMatchingKey1);

        EfecteEntity result = efecteKeyResolver.findMatchingEfecteKey(builtEqualEfecteKey, keys);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("findMatchingEfecteKey - with key holder")
    void testShouldReturnNullWhenNoKeyWithMatchingSecurityAccessesIsFound_KeyHolder() throws Exception {
        String matchinKeyHolderEntityId = "123456";
        String matchingSecurityAccessEntityId1 = "234567";
        String matchingSecurityAccessEntityId2 = "345678";
        EfecteEntity builtEqualEfecteKey = new EfecteEntityBuilder()
                .withKeyHolderReference(matchinKeyHolderEntityId)
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId1),
                        new EfecteReference(matchingSecurityAccessEntityId2))
                .build();
        EfecteEntity nonMatchingKey1 = new EfecteEntityBuilder()
                .withKeyHolderReference(matchinKeyHolderEntityId)
                .withSecurityAccesses(new EfecteReference(matchingSecurityAccessEntityId1))
                .build();
        EfecteEntity nonMatchingKey2 = new EfecteEntityBuilder()
                .withKeyHolderReference(matchinKeyHolderEntityId)
                .withSecurityAccesses(new EfecteReference(matchingSecurityAccessEntityId2))
                .build();
        EfecteEntity nonMatchingKey3 = new EfecteEntityBuilder()
                .withKeyHolderReference(matchinKeyHolderEntityId)
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId2),
                        new EfecteReference(matchingSecurityAccessEntityId2),
                        new EfecteReference("some other security access id"))
                .build();

        List<EfecteEntity> keys = List.of(nonMatchingKey1, nonMatchingKey2, nonMatchingKey3);

        EfecteEntity result = efecteKeyResolver.findMatchingEfecteKey(builtEqualEfecteKey, keys);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("findMatchingEfecteKey - with outsider - email and name")
    void testShouldReturnTheMatchingKeyWithOutsider() throws Exception {
        String matchingOutsiderName = "John Smith";
        String matchingOutsiderEmail = "john.smith@outsider.com";
        String matchingSecurityAccessEntityId1 = "234567";
        String matchingSecurityAccessEntityId2 = "987654";
        EfecteEntity builtEqualEfecteKey = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(matchingOutsiderName)
                .withOutsiderEmail(matchingOutsiderEmail)
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId1),
                        new EfecteReference(matchingSecurityAccessEntityId2))
                .build();
        EfecteEntity nonMatchingKey1 = new EfecteEntityBuilder()
                .withKeyHolderReference("irrelevant")
                .withSecurityAccesses(new EfecteReference(matchingSecurityAccessEntityId1))
                .build();
        EfecteEntity nonMatchingKey2 = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderEmail("jane.smith@outsider.com")
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId2),
                        new EfecteReference(matchingSecurityAccessEntityId1))
                .build();
        EfecteEntity matchingKey = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderEmail(matchingOutsiderEmail)
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId2),
                        new EfecteReference(matchingSecurityAccessEntityId1))
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        EfecteEntity nonMatchingKey3 = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName("non matching outsider name")
                .withOutsiderEmail(matchingOutsiderEmail)
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId2),
                        new EfecteReference(matchingSecurityAccessEntityId1))
                .build();

        List<EfecteEntity> keys = List.of(nonMatchingKey1, nonMatchingKey3, nonMatchingKey2, matchingKey);

        EfecteEntity result = efecteKeyResolver.findMatchingEfecteKey(builtEqualEfecteKey, keys);

        assertThat(result).isEqualTo(matchingKey);
    }

    @Test
    @DisplayName("findMatchingEfecteKey - with outsider - name")
    void testShouldReturnTheMatchingKeyWithOutsiderName() throws Exception {
        String matchingOutsiderName = "John Smith";
        String matchingSecurityAccessEntityId1 = "234567";
        String matchingSecurityAccessEntityId2 = "987654";
        EfecteEntity builtEqualEfecteKey = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(matchingOutsiderName)
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId1),
                        new EfecteReference(matchingSecurityAccessEntityId2))
                .build();
        EfecteEntity nonMatchingKey1 = new EfecteEntityBuilder()
                .withKeyHolderReference("irrelevant")
                .withSecurityAccesses(new EfecteReference(matchingSecurityAccessEntityId1))
                .build();
        EfecteEntity nonMatchingKey2 = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName("Jane Smith")
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId2),
                        new EfecteReference(matchingSecurityAccessEntityId1))
                .build();
        EfecteEntity matchingKey = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(matchingOutsiderName)
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId2),
                        new EfecteReference(matchingSecurityAccessEntityId1))
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        List<EfecteEntity> keys = List.of(nonMatchingKey1, nonMatchingKey2, matchingKey);

        EfecteEntity result = efecteKeyResolver.findMatchingEfecteKey(builtEqualEfecteKey, keys);

        assertThat(result).isEqualTo(matchingKey);
    }

    @Test
    @DisplayName("findMatchingEfecteKey - with outsider")
    void testShouldReturnNullWhenNoKeyWithAMatchingOutsiderIsFound() throws Exception {
        String matchingOutsiderName = "John Smith";
        String matchingSecurityAccessEntityId = "234567";
        EfecteEntity builtEqualEfecteKey = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(matchingOutsiderName)
                .withSecurityAccesses(new EfecteReference(matchingSecurityAccessEntityId))
                .build();
        EfecteEntity nonMatchingKey1 = new EfecteEntityBuilder()
                .withKeyHolderReference("irrelevant")
                .withSecurityAccesses(new EfecteReference(matchingSecurityAccessEntityId))
                .build();
        EfecteEntity nonMatchingKey2 = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName("Jane Smith")
                .withSecurityAccesses(new EfecteReference(matchingSecurityAccessEntityId))
                .build();
        EfecteEntity nonMatchingKey3 = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName("non-matching")
                .withSecurityAccesses(new EfecteReference("non-matching"))
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        List<EfecteEntity> keys = List.of(nonMatchingKey1, nonMatchingKey3, nonMatchingKey2);

        EfecteEntity result = efecteKeyResolver.findMatchingEfecteKey(builtEqualEfecteKey, keys);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("findMatchingEfecteKey - with outsider")
    void testShouldReturnNullWhenNoKeyWithMatchingSecurityAccessesIsFound_Outsider() throws Exception {
        String matchingOutsiderName = "John Smith";
        String matchingSecurityAccessEntityId1 = "234567";
        String matchingSecurityAccessEntityId2 = "345678";
        EfecteEntity builtEqualEfecteKey = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(matchingOutsiderName)
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId1),
                        new EfecteReference(matchingSecurityAccessEntityId2))
                .build();
        EfecteEntity nonMatchingKey1 = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(matchingOutsiderName)
                .withSecurityAccesses(new EfecteReference(matchingSecurityAccessEntityId1))
                .build();
        EfecteEntity nonMatchingKey2 = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(matchingOutsiderName)
                .withSecurityAccesses(new EfecteReference(matchingSecurityAccessEntityId2))
                .build();
        EfecteEntity nonMatchingKey3 = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(matchingOutsiderName)
                .withSecurityAccesses(
                        new EfecteReference(matchingSecurityAccessEntityId2),
                        new EfecteReference(matchingSecurityAccessEntityId2),
                        new EfecteReference("some other security access id"))
                .build();

        List<EfecteEntity> keys = List.of(nonMatchingKey1, nonMatchingKey3, nonMatchingKey2);

        EfecteEntity result = efecteKeyResolver.findMatchingEfecteKey(builtEqualEfecteKey, keys);

        assertThat(result).isNull();
    }

}
