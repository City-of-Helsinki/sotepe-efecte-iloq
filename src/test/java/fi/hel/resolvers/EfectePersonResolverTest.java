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

        String expectedPrefix = ri.getMappedPersonILoqPrefix() + iLoqPersonId;
        when(redis.get(expectedPrefix)).thenReturn("irrelevant");
        when(helper.writeAsPojo(any(), any())).thenReturn(new EfecteEntityIdentifier());

        verifyNoInteractions(redis);

        efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPersonId);

        verify(redis).get(expectedPrefix);
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldReturnTheEfecteKeyHolderEntityIdFoundFromRedis() throws Exception {
        String iLoqPersonId = "123";
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

        String result = efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPersonId);

        assertThat(result).isEqualTo(expectedKeyHolderEntityId);
        verify(helper).writeAsPojo(response, EfecteEntityIdentifier.class);
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldReturnTheMatchingEfecteIdentifierJsonFromRedisForOutsiders() throws Exception {
        String iLoqPersonId = "123";
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

        String result = efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPersonId);

        assertThat(result).isEqualTo(expectedEfectePersonIdentifierValue);
        verify(helper).writeAsPojo(expectedEfectePersonIdentifierValue, EfecteEntityIdentifier.class);
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldFetchTheILoqPersonWhenTheMatchingEfecteKeyHolderEntityIdentifierIsNotFoundAtRedis()
            throws Exception {
        String expectedPersonId = "123";

        when(redis.get(anyString())).thenReturn(null);
        mocked.getGetILoqPerson().whenAnyExchangeReceived(
                exchange -> exchange.getIn()
                        .setBody(new ILoqPerson("irrelevant", "irrelevant", "irrelevant")));
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of()));

        mocked.getGetILoqPerson().expectedMessageCount(1);
        mocked.getGetILoqPerson().expectedPropertyReceived("iLoqPersonId", expectedPersonId);

        efecteKeyHolderResolver.resolveEfectePersonIdentifier(expectedPersonId);

        mocked.getGetILoqPerson().assertIsSatisfied();
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldThrowAnExceptionWhenAnILoqPersonIsNotFound() throws Exception {
        String iLoqPersonId = "123";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        iLoqKey.setPersonId(iLoqPersonId);
        iLoqKey.setSecurityAccesses(Set.of());
        String exceptionMessage = "Oh no! an exception!";
        String expectedExceptionMessage = "EfecteKeyHolderResolver.resolveEfecteKeyHolderEntityId: Fetching the iLOQ person failed: "
                + exceptionMessage;

        mocked.getGetILoqPerson().whenAnyExchangeReceived(exchange -> {
            Exception exception = new Exception(exceptionMessage);
            exchange.setException(exception);
            exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);
        });

        assertThatThrownBy(() -> efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPersonId))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("resolveEfecteKeyHolderEntityId")
    void testShouldFetchTheEfecteKeyHolderWithTheILoqPersonName() throws Exception {
        String iLoqPersonId = "123";
        String firstName = "John";
        String lastName = "Doe";
        ILoqPerson iLoqPerson = new ILoqPerson(firstName, lastName, iLoqPersonId);
        String expectedEfecteQuery = """
                SELECT entity
                FROM entity
                WHERE
                    template.code = 'person'
                    AND $first_name$ = '%s'
                    AND $last_name$ = '%s'
                """.formatted(firstName, lastName).replaceAll("\\s+", " ").trim();

        mocked.getGetILoqPerson().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(iLoqPerson));
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of()));

        mocked.getGetEfecteEntity().expectedMessageCount(1);
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteEntityType", "person");
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteQuery", expectedEfecteQuery);

        efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPersonId);

        mocked.getGetEfecteEntity().assertIsSatisfied();
    }

    @Test
    @DisplayName("resolveEfecteKeyHolderEntityId")
    void testShouldNormalizeILoqPersonNameBeforeFetchingTheEfecteKeyHolder() throws Exception {
        String iLoqPersonId = "123";
        String firstName = "John ";
        String lastName = " Doe";
        String expectedFirstName = "John";
        String expectedLastName = "Doe";
        ILoqPerson iLoqPerson = new ILoqPerson(firstName, lastName, iLoqPersonId);
        String expectedEfecteQuery = """
                SELECT entity
                FROM entity
                WHERE
                    template.code = 'person'
                    AND $first_name$ = '%s'
                    AND $last_name$ = '%s'
                """
                .formatted(expectedFirstName, expectedLastName)
                .trim().replaceAll("\\s+", " ");

        mocked.getGetILoqPerson().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(iLoqPerson));
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of()));

        mocked.getGetEfecteEntity().expectedMessageCount(1);
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteQuery", expectedEfecteQuery);

        efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPersonId);

        mocked.getGetEfecteEntity().assertIsSatisfied();
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldThrowAnExceptionWhenFetchingTheEfectePersonFails() throws Exception {
        String iLoqPersonId = "123";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        iLoqKey.setPersonId(iLoqPersonId);
        iLoqKey.setSecurityAccesses(Set.of());
        String exceptionMessage = "Oh no! an exception!";
        String expectedExceptionMessage = "EfecteKeyHolderResolver.resolveEfecteKeyHolderEntityId: Fetching the Efecte person failed: "
                + exceptionMessage;

        mocked.getGetILoqPerson().whenAnyExchangeReceived(
                exchange -> exchange.getIn()
                        .setBody(new ILoqPerson("irrelevant", "irrelevant", iLoqPersonId)));
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> {
            Exception exception = new Exception(exceptionMessage);
            exchange.setException(exception);
            exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);
        });

        assertThatThrownBy(() -> efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPersonId))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldReturnTheILoqPersonNameInsteadOfTheKeyHolderEntityIdWhenNoEfecteKeyHoldersAreFound()
            throws Exception {
        String iLoqPersonId = "123";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        iLoqKey.setPersonId(iLoqPersonId);
        iLoqKey.setSecurityAccesses(Set.of());

        String firstName = "John";
        String lastName = "Doe";
        String expectedResult = firstName + " " + lastName;
        ILoqPerson iLoqPerson = new ILoqPerson(firstName, lastName, iLoqPersonId);

        mocked.getGetILoqPerson().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(iLoqPerson));
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(
                exchange -> exchange.getIn().setBody(List.of()));

        String result = efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPersonId);

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldThrowAnAuditExceptionWhenMultiplePersonsAreFoundAtEfecte() throws Exception {
        String expectedILoqPersonId = "123";
        EnrichedILoqKey iLoqKey = new EnrichedILoqKey();
        iLoqKey.setPersonId(expectedILoqPersonId);
        iLoqKey.setSecurityAccesses(Set.of());

        String firstName = "John";
        String lastName = "Doe";
        ILoqPerson iLoqPerson = new ILoqPerson(firstName, lastName, expectedILoqPersonId);

        mocked.getGetILoqPerson().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(iLoqPerson));
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

        assertThatThrownBy(() -> efecteKeyHolderResolver.resolveEfectePersonIdentifier(expectedILoqPersonId));

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

        String expectedKeyHolderEntityId = "12345";
        EfecteEntity efecteKeyHolderEntity = new EfecteEntityBuilder()
                .withId(expectedKeyHolderEntityId)
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();

        mocked.getGetILoqPerson().whenAnyExchangeReceived(
                exchange -> exchange.getIn().setBody(new ILoqPerson("John", "Doe", iLoqPersonId)));
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(
                exchange -> exchange.getIn().setBody(List.of(efecteKeyHolderEntity)));

        String result = efecteKeyHolderResolver.resolveEfectePersonIdentifier(iLoqPersonId);

        assertThat(result).isEqualTo(expectedKeyHolderEntityId);
    }

    @Test
    @DisplayName("resolveEfectePersonIdentifier")
    void testShouldSaveTheMappedKeysForTheResolvedPerson() throws Exception {
        String expectedPersonId = "abc-123";
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
        mocked.getGetILoqPerson().whenAnyExchangeReceived(
                exchange -> exchange.getIn()
                        .setBody(new ILoqPerson("irrelevant", "irrelevant", expectedPersonId)));
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(
                exchange -> exchange.getIn().setBody(List.of(keyHolderEfecteEntity)));
        when(helper.writeAsJson(efecteEntityIdentifier)).thenReturn(expectedEfecteEntityIdentifierJson);

        String expectedEfectePrefix = ri.getMappedPersonEfectePrefix() + keyHolderEntityId;
        String expectedILoqPrefix = ri.getMappedPersonILoqPrefix() + expectedPersonId;

        verifyNoInteractions(redis);
        verifyNoInteractions(helper);

        efecteKeyHolderResolver.resolveEfectePersonIdentifier(expectedPersonId);

        verify(redis).set(expectedEfectePrefix, expectedPersonId);
        verify(redis).set(expectedILoqPrefix, expectedEfecteEntityIdentifierJson);
        verify(helper).writeAsJson(efecteEntityIdentifier);
    }
}
