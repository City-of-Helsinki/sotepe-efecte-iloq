package fi.hel.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.exceptions.AuditException;
import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.EnrichedILoqKey;
import fi.hel.models.ILoqPerson;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import fi.hel.processors.AuditExceptionProcessor;
import fi.hel.processors.Helper;
import fi.hel.processors.ResourceInjector;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class EfectePersonResolverTest extends CamelQuarkusTestSupport {

    @Inject
    EfectePersonResolver efecteKeyHolderResolver;
    @Inject
    ResourceInjector ri;
    @Inject
    TestUtils testUtils;
    @Inject
    MockEndpointInjector mocked;
    @InjectMock
    Redis redis;
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
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldGetTheMatchingEfecteKeyHolderEntityIdentifierFromRedis() throws Exception {
        String iLoqPersonId = "123";
        ILoqPerson iLoqPerson = new ILoqPerson(iLoqPersonId);

        String expectedPrefix = ri.getMappedPersonILoqPrefix() + iLoqPersonId;
        when(redis.get(expectedPrefix)).thenReturn("irrelevant");
        when(helper.writeAsPojo(any(), any())).thenReturn(new EfecteEntityIdentifier());

        verifyNoInteractions(redis);

        efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPerson);

        verify(redis).get(expectedPrefix);
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldReturnTheEfecteKeyHolderEntityIdFoundFromRedis() throws Exception {
        String iLoqPersonId = "123";
        ILoqPerson iLoqPerson = new ILoqPerson(iLoqPersonId);
        String expectedKeyHolderEntityId = "12345";

        String response = """
                {
                    "entityId": "%s",
                    "efecteId": "irrelevant"
                }
                """.formatted(expectedKeyHolderEntityId);
        EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier(expectedKeyHolderEntityId,
                "irrelevant");

        when(redis.get(anyString())).thenReturn(response);
        when(helper.writeAsPojo(any(), any())).thenReturn(efecteEntityIdentifier);

        String result = efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPerson);

        assertThat(result).isEqualTo(expectedKeyHolderEntityId);
        verify(helper).writeAsPojo(response, EfecteEntityIdentifier.class);
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldReturnTheMatchingEfecteIdentifierJsonFromRedisForOutsiders() throws Exception {
        String iLoqPersonId = "123";
        ILoqPerson iLoqPerson = new ILoqPerson(iLoqPersonId);
        String outsiderName = "John Smith";
        String outsiderEmail = "john.smith@outsider.com";

        String expectedEfectePersonIdentifierValue = """
                {
                    "entityId": "",
                    "efecteId": "",
                    "outsiderName": "%s",
                    "outsiderEmail": "%s"
                }
                """.formatted(outsiderName, outsiderEmail);
        EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier();
        efecteEntityIdentifier.setOutsiderEmail(outsiderEmail);

        when(redis.get(anyString())).thenReturn(expectedEfectePersonIdentifierValue);
        when(helper.writeAsPojo(any(), any())).thenReturn(efecteEntityIdentifier);

        String result = efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPerson);

        assertThat(result).isEqualTo(expectedEfectePersonIdentifierValue);
        verify(helper).writeAsPojo(expectedEfectePersonIdentifierValue, EfecteEntityIdentifier.class);
    }

    @Test
    @DisplayName("resolveEfecteKeyHolderEntityId")
    void testShouldFetchTheEfecteKeyHolderWithTheILoqPersonName() throws Exception {
        String iLoqPersonId = "123";
        String firstName = "John";
        String urlEncodedFirstName = "url encoded value of John";
        String lastName = "Doe";
        String urlEncodedLastName = "url encoded value of Doe";
        ILoqPerson iLoqPerson = new ILoqPerson(firstName, lastName, iLoqPersonId);
        String expectedEfecteQuery = """
                SELECT entity
                FROM entity
                WHERE
                    template.code = 'person'
                    AND $first_name$ = '%s'
                    AND $last_name$ = '%s'
                """.formatted(urlEncodedFirstName, urlEncodedLastName).replaceAll("\\s+", " ").trim();

        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of()));
        when(helper.urlEncode(firstName)).thenReturn(urlEncodedFirstName);
        when(helper.urlEncode(lastName)).thenReturn(urlEncodedLastName);

        mocked.getGetEfecteEntity().expectedMessageCount(1);
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteEntityType", "person");
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteQuery", expectedEfecteQuery);

        verifyNoInteractions(helper);

        efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPerson);

        verify(helper).urlEncode(firstName);
        verify(helper).urlEncode(lastName);
        mocked.getGetEfecteEntity().assertIsSatisfied();
    }

    @Test
    @DisplayName("resolveEfecteKeyHolderEntityId")
    void testShouldNormalizeILoqPersonNameBeforeFetchingTheEfecteKeyHolder() throws Exception {
        String iLoqPersonId = "123";
        String firstName = "John ";
        String lastName = " Doe";
        String expectedFirstName = "John";
        String urlEncodedFirstName = "url encoded value of John";
        String expectedLastName = "Doe";
        String urlEncodedLastName = "url encoded value of Doe";
        ILoqPerson iLoqPerson = new ILoqPerson(firstName, lastName, iLoqPersonId);
        String expectedEfecteQuery = """
                SELECT entity
                FROM entity
                WHERE
                    template.code = 'person'
                    AND $first_name$ = '%s'
                    AND $last_name$ = '%s'
                """
                .formatted(urlEncodedFirstName, urlEncodedLastName)
                .trim().replaceAll("\\s+", " ");

        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of()));
        when(helper.urlEncode(expectedFirstName)).thenReturn(urlEncodedFirstName);
        when(helper.urlEncode(expectedLastName)).thenReturn(urlEncodedLastName);

        mocked.getGetEfecteEntity().expectedMessageCount(1);
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteQuery", expectedEfecteQuery);

        verifyNoInteractions(helper);

        efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPerson);

        verify(helper).urlEncode(expectedFirstName);
        verify(helper).urlEncode(expectedLastName);
        mocked.getGetEfecteEntity().assertIsSatisfied();
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldThrowAnExceptionWhenFetchingTheEfectePersonFails() throws Exception {
        String iLoqPersonId = "123";
        ILoqPerson iLoqPerson = new ILoqPerson("John", "Doe", iLoqPersonId);
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        iLoqKey.setPerson(new ILoqPerson(iLoqPersonId));
        iLoqKey.setSecurityAccesses(Set.of());
        String exceptionMessage = "Oh no! an exception!";
        String expectedExceptionMessage = "EfecteKeyHolderResolver.resolveEfecteKeyHolderEntityId: Fetching the Efecte person failed: "
                + exceptionMessage;
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> {
            Exception exception = new Exception(exceptionMessage);
            exchange.setException(exception);
            exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);
        });

        assertThatThrownBy(() -> efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPerson))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldReturnTheILoqPersonNameInsteadOfTheKeyHolderEntityIdWhenNoEfecteKeyHoldersAreFound()
            throws Exception {
        String iLoqPersonId = "123";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        iLoqKey.setPerson(new ILoqPerson(iLoqPersonId));
        iLoqKey.setSecurityAccesses(Set.of());

        String firstName = "John";
        String lastName = "Doe";
        String expectedResult = firstName + " " + lastName;
        ILoqPerson iLoqPerson = new ILoqPerson(firstName, lastName, iLoqPersonId);

        mocked.getGetEfecteEntity().whenAnyExchangeReceived(
                exchange -> exchange.getIn().setBody(List.of()));

        String result = efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPerson);

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldSaveTheAuditRecordForTheILoqPersonWhenMultiplePersonsAreFoundAtEfecte() throws Exception {
        String iLoqPersonId = "123";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        String firstName = "John";
        String lastName = "Doe";
        ILoqPerson iLoqPerson = new ILoqPerson(firstName, lastName, iLoqPersonId);
        iLoqKey.setPerson(iLoqPerson);
        iLoqKey.setSecurityAccesses(Set.of());

        EfecteEntity efecteEntity1 = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.PERSON)
                .withPersonEfecteId("1")
                .build();
        EfecteEntity efecteEntity2 = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.PERSON)
                .withPersonEfecteId("2")
                .build();
        List<EfecteEntity> efecteEntities = List.of(efecteEntity1, efecteEntity2);
        String expectedValue = efecteEntities.toString();

        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(efecteEntities));

        String expectedPrefix = ri.getAuditRecordPersonPrefix() + iLoqPersonId;

        verifyNoInteractions(redis);

        efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPerson);

        verify(redis).set(expectedPrefix, expectedValue);
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldThrowAnAuditExceptionWhenMultiplePersonsAreFoundAtEfecte() throws Exception {
        String expectedILoqPersonId = "123";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        String firstName = "John";
        String lastName = "Doe";
        ILoqPerson iLoqPerson = new ILoqPerson(firstName, lastName, expectedILoqPersonId);
        iLoqKey.setPerson(iLoqPerson);
        iLoqKey.setSecurityAccesses(Set.of());

        mocked.getGetEfecteEntity().whenAnyExchangeReceived(
                exchange -> exchange.getIn().setBody(List.of(
                        new EfecteEntity("1"), new EfecteEntity("2"))));

        String expectedAuditMessage = "Multiple matching key holders found for iLOQ person '" + firstName + " "
                + lastName
                + "' at Efecte";

        doAnswer(i -> {
            throw new AuditException();
        }).when(auditExceptionProcessor).throwAuditException(
                any(), any(), any(), any(), any(), any());

        verifyNoInteractions(auditExceptionProcessor);

        assertThatThrownBy(() -> efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPerson));

        verify(auditExceptionProcessor).throwAuditException(
                EnumDirection.ILOQ,
                EnumDirection.EFECTE,
                null,
                null,
                expectedILoqPersonId,
                expectedAuditMessage);
    }

    @Test
    @DisplayName("resolveEfecteKeyHolderEntityId")
    void testShouldReturnTheResolvedEfecteKeyHolderEntityId() throws Exception {
        String iLoqPersonId = "123";
        ILoqPerson iLoqPerson = new ILoqPerson("John", "Doe", iLoqPersonId);

        String expectedKeyHolderEntityId = "12345";
        EfecteEntity efecteKeyHolderEntity = new EfecteEntityBuilder()
                .withId(expectedKeyHolderEntityId)
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();

        mocked.getGetEfecteEntity().whenAnyExchangeReceived(
                exchange -> exchange.getIn().setBody(List.of(efecteKeyHolderEntity)));

        String result = efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPerson);

        assertThat(result).isEqualTo(expectedKeyHolderEntityId);
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldSaveTheMappedKeysForTheResolvedPerson() throws Exception {
        String expectedPersonId = "abc-123";
        ILoqPerson iLoqPerson = new ILoqPerson("John", "Doe", expectedPersonId);
        String keyHolderEntityId = "123456";
        String keyHolderEfecteId = "PER-0001";
        EfecteEntity keyHolderEfecteEntity = new EfecteEntityBuilder()
                .withId(keyHolderEntityId)
                .withPersonEfecteId(keyHolderEfecteId)
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();

        EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier(
                keyHolderEntityId, keyHolderEfecteId);
        String expectedEfecteEntityIdentifierJson = "efecte entity identifier json";
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(
                exchange -> exchange.getIn().setBody(List.of(keyHolderEfecteEntity)));
        when(helper.writeAsJson(efecteEntityIdentifier)).thenReturn(expectedEfecteEntityIdentifierJson);

        String expectedEfectePrefix = ri.getMappedPersonEfectePrefix() + keyHolderEntityId;
        String expectedILoqPrefix = ri.getMappedPersonILoqPrefix() + expectedPersonId;

        verifyNoInteractions(redis);
        verifyNoInteractions(helper);

        efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPerson);

        verify(redis).set(expectedEfectePrefix, expectedPersonId);
        verify(redis).set(expectedILoqPrefix, expectedEfecteEntityIdentifierJson);
        verify(helper).writeAsJson(efecteEntityIdentifier);
    }
}
