package fi.hel.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.test_utils.TestUtils;
import com.devikone.transports.Redis;

import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.ILoqPerson;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import fi.hel.processors.Helper;
import fi.hel.processors.ResourceInjector;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

@TestInstance(Lifecycle.PER_CLASS)
@TestProfile(ILoqPersonResolverTest.class)
@QuarkusTest
public class ILoqPersonResolverTest extends CamelQuarkusTestSupport {

    @Inject
    ILoqPersonResolver iLoqPersonResolver;
    @Inject
    ResourceInjector ri;
    @Inject
    TestUtils testUtils;
    @Inject
    MockEndpointInjector mocked;
    @InjectMock
    Redis redis;
    @InjectMock
    Helper helper;

    @Override
    protected void doPreSetup() throws Exception {
        super.doPostSetup();
        testConfiguration().withUseRouteBuilder(false);
    }

    @BeforeEach
    void reset() {
        iLoqPersonResolver.resetCache();
    }

    ////////////////////
    // Efecte -> iLOQ //
    ////////////////////

    @Test
    @DisplayName("resolveILoqPersonId")
    void testShouldGetTheStoredILoqPersonIdFromRedis_KeyHolderId() throws Exception {
        String externalId = "irrelevant";
        String expectedPrefix = ri.getMappedPersonEfectePrefix() + externalId;

        when(redis.get(expectedPrefix)).thenReturn("something but not null");

        verifyNoInteractions(redis);

        iLoqPersonResolver.resolveILoqPersonId(externalId);

        verify(redis).get(expectedPrefix);
    }

    @Test
    @DisplayName("resolveILoqPersonId/resolveILoqPersonIdForOutsider")
    void testShouldReturnTheStoredILoqEntityId() throws Exception {
        String externalId = "12345";
        String expectedILoqPersonId = "5678";

        when(redis.get(anyString())).thenReturn(expectedILoqPersonId);

        String iLoqPersonId1 = iLoqPersonResolver.resolveILoqPersonId(externalId);
        String iLoqPersonId2 = iLoqPersonResolver.resolveILoqPersonIdForOutsider(externalId,
                "irrelevant outsider name");

        assertThat(iLoqPersonId1).isEqualTo(expectedILoqPersonId);
        assertThat(iLoqPersonId2).isEqualTo(expectedILoqPersonId);
    }

    @Test
    @DisplayName("resolveILoqPersonId - person id is not found at Redis")
    void testShouldGetTheILoqPersonByExternalId() throws Exception {
        String expectedExternalId = "12345";

        mocked.getGetILoqPersonByExternalId()
                .whenAnyExchangeReceived(
                        exchange -> exchange.getIn()
                                .setBody(List.of(new ILoqPerson("irrelevant"))));

        when(helper.urlEncode(expectedExternalId)).thenReturn(expectedExternalId);
        mocked.getGetILoqPersonByExternalId().expectedMessageCount(1);
        mocked.getGetILoqPersonByExternalId().expectedPropertyReceived("externalPersonId", expectedExternalId);
        mocked.getListILoqPersons().expectedMessageCount(0);
        verifyNoInteractions(helper);

        iLoqPersonResolver.resolveILoqPersonId(expectedExternalId);

        verify(helper).urlEncode(expectedExternalId);
        MockEndpoint.assertIsSatisfied(
                mocked.getGetILoqPersonByExternalId(),
                mocked.getListILoqPersons());
    }

    @Test
    @DisplayName("resolveILoqPersonId/resolveILoqPersonIdForOutsider - person id is not found at Redis")
    void testShouldRetunTheResolvedILoqPersonIdByExternalPersonId() throws Exception {
        String externalId = "12345";
        String expectedILoqPersonId = "abc-123";

        ILoqPerson iLoqPerson = new ILoqPerson(expectedILoqPersonId);

        mocked.getGetILoqPersonByExternalId()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(iLoqPerson)));

        String iLoqPersonId1 = iLoqPersonResolver.resolveILoqPersonId(externalId);
        String iLoqPersonId2 = iLoqPersonResolver.resolveILoqPersonIdForOutsider(externalId,
                "irrelevant outsider name");

        assertThat(iLoqPersonId1).isEqualTo(expectedILoqPersonId);
        assertThat(iLoqPersonId2).isEqualTo(expectedILoqPersonId);
    }

    @Test
    @DisplayName("resolveILoqPersonId - person id is not found at Redis")
    void testShouldSaveTheMappedKeyForTheResolvedILoqPersonId_ByExternalId_KeyHolder() throws Exception {
        String externalId = "12345";
        String expectedILoqPersonId = "2";
        ILoqPerson iLoqPerson = new ILoqPerson(expectedILoqPersonId);

        String expectedEfectePrefix = ri.getMappedPersonEfectePrefix() + externalId;

        mocked.getGetILoqPersonByExternalId().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(
                List.of(iLoqPerson)));

        verifyNoInteractions(redis);

        iLoqPersonResolver.resolveILoqPersonId(externalId);

        verify(redis).set(expectedEfectePrefix, expectedILoqPersonId);
    }

    @Test
    @DisplayName("resolveILoqPersonId/resolveILoqPersonIdForOutsider - person id is not found at Redis")
    void testShouldThrowAnExceptionWhenGettingILoqPersonByExternalIdFails() throws Exception {
        String externalId = "12345";

        String exceptionMessage = "Oh no! An exception!";
        String expectedExceptionMessage = "ILoqPersonResolver: Fetching iLOQ persons by external person id failed: "
                + exceptionMessage;

        mocked.getGetILoqPersonByExternalId()
                .whenAnyExchangeReceived(
                        exchange -> {
                            Exception exception = new Exception(exceptionMessage);
                            exchange.setException(exception);
                            exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);
                        });

        assertThatThrownBy(() -> iLoqPersonResolver.resolveILoqPersonId(externalId))
                .hasMessage(expectedExceptionMessage);
        assertThatThrownBy(
                () -> iLoqPersonResolver.resolveILoqPersonIdForOutsider(externalId,
                        "irrelevant outsider name"))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("resolveILoqPersonId - person id is not found at Redis")
    void testShouldSetAnAuditMessageWhenMultipleILoqPersonsAreFound_ByExternalId_KeyHolder() throws Exception {
        String externalId = "12345";

        ILoqPerson iLoqPerson1 = new ILoqPerson("1");
        ILoqPerson iLoqPerson2 = new ILoqPerson("2");

        String expectedAuditMessage = "Found multiple iLOQ persons when using '" + externalId
                + "' as the external id";

        mocked.getGetILoqPersonByExternalId()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(
                        List.of(iLoqPerson1, iLoqPerson2)));
        when(redis.exists(anyString())).thenReturn(true);

        verifyNoInteractions(redis);

        String iLoqPersonId = iLoqPersonResolver.resolveILoqPersonId(externalId);

        assertThat(iLoqPersonId).isNull();
        verify(redis).set(ri.getAuditMessagePrefix(), expectedAuditMessage);
    }

    @Test
    @DisplayName("resolveILoqPersonId/resolveILoqPersonIdForOutsider - person id is not found at Redis")
    void testShouldFetchAllILoqPersonsWhenILoqPersonIdIsNotStoredAtRedis() throws Exception {
        String keyHolderEntityId = "12345";

        setDefaultResponses();

        mocked.getListILoqPersons().expectedMessageCount(2);

        iLoqPersonResolver.resolveILoqPersonId(keyHolderEntityId);
        iLoqPersonResolver.resetCache();
        iLoqPersonResolver.resolveILoqPersonIdForOutsider(keyHolderEntityId, "irrelevant outsider name");

        mocked.getListILoqPersons().assertIsSatisfied();
    }

    @Test
    @DisplayName("resolveILoqPersonId - person id is not found at Redis")
    void testShouldGetTheEfectePersonEntity() throws Exception {
        String keyHolderEntityId = "12345";

        String expectedHttpQuery = "SELECT entity FROM entity WHERE entity.id = '%s'"
                .formatted(keyHolderEntityId);

        setDefaultResponses();

        mocked.getGetEfecteEntity().expectedMessageCount(1);
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteEntityType", "person");
        mocked.getGetEfecteEntity().expectedPropertyReceived("efecteQuery", expectedHttpQuery);

        iLoqPersonResolver.resolveILoqPersonId(keyHolderEntityId);

        mocked.getGetEfecteEntity().assertIsSatisfied();
    }

    @Test
    @DisplayName("resolveILoqPersonId - person id is not found at Redis")
    void testShouldTemporarilySaveTheReturnedEfectePersonToRedis() throws Exception {
        String keyHolderEntityId = "12345";

        EfecteEntity expectedEfectePerson = new EfecteEntityBuilder()
                .withId(keyHolderEntityId)
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();

        String expectedPrefix = ri.getTempEfectePersonPrefix() + keyHolderEntityId;
        String expectedValue = "fake efecte person xml";

        setDefaultResponses();

        when(helper.writeAsJson(any())).thenReturn(expectedValue);
        mocked.getGetEfecteEntity()
                .whenAnyExchangeReceived(
                        exchange -> exchange.getIn().setBody(List.of(expectedEfectePerson)));

        verifyNoInteractions(redis);
        verifyNoInteractions(helper);

        iLoqPersonResolver.resolveILoqPersonId(keyHolderEntityId);

        verify(helper).writeAsJson(expectedEfectePerson);
        verify(redis).set(expectedPrefix, expectedValue);
    }

    @Test
    @DisplayName("resolveILoqPersonId - person id is not found at Redis")
    void testShouldFilterTheMatchingILoqPersonByEfectePersonNameAndReturnTheILoqPersonId() throws Exception {
        String keyHolderEntityId = "12345";

        when(redis.get(anyString())).thenReturn(null);

        String firstName = "John Robert";
        String lastName = "Smith";
        EfecteEntity efectePerson = new EfecteEntityBuilder()
                .withId(keyHolderEntityId)
                .withFirstName(firstName)
                .withLastName(lastName)
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();

        String expectedILoqPersonId = "2";
        ILoqPerson iLoqPerson1 = new ILoqPerson("foo", "bar", "1");
        ILoqPerson iLoqPerson2 = new ILoqPerson(firstName, lastName, expectedILoqPersonId);
        ILoqPerson iLoqPerson3 = new ILoqPerson("Jane", "Doe", "3");

        setDefaultResponses();

        mocked.getGetEfecteEntity()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(efectePerson)));
        mocked.getListILoqPersons().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(
                iLoqPerson1, iLoqPerson2, iLoqPerson3)));

        String iLoqPersonId = iLoqPersonResolver.resolveILoqPersonId(keyHolderEntityId);

        assertThat(iLoqPersonId).isEqualTo(expectedILoqPersonId);
    }

    @Test
    @DisplayName("resolveILoqPersonId/resolveILoqPersonIdForOutsider - person id is not found at Redis")
    void testShouldNormalizeTheNamesBeforeMatching() throws Exception {
        String keyHolderEntityId = "12345";

        when(redis.get(anyString())).thenReturn(null);

        String firstName = "John Robert";
        String lastName = "Smith";
        EfecteEntity efectePerson = new EfecteEntityBuilder()
                .withId(keyHolderEntityId)
                .withFirstName(firstName)
                .withLastName(lastName)
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();

        String expectedILoqPersonId = "2";
        ILoqPerson iLoqPerson1 = new ILoqPerson("foo", "bar", "1");
        ILoqPerson iLoqPerson2 = new ILoqPerson("john Robert ", " SMITH", expectedILoqPersonId);
        ILoqPerson iLoqPerson3 = new ILoqPerson("Jane", "Doe", "3");

        setDefaultResponses();

        mocked.getGetEfecteEntity()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(efectePerson)));
        mocked.getListILoqPersons().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(
                iLoqPerson1, iLoqPerson2, iLoqPerson3)));

        String iLoqPersonId1 = iLoqPersonResolver.resolveILoqPersonId(keyHolderEntityId);
        String iLoqPersonId2 = iLoqPersonResolver.resolveILoqPersonIdForOutsider("irrelevant outsider email",
                firstName + " " + lastName);

        assertThat(iLoqPersonId1).isEqualTo(expectedILoqPersonId);
        assertThat(iLoqPersonId2).isEqualTo(expectedILoqPersonId);
    }

    @Test
    @DisplayName("resolveILoqPersonId - person id is not found at Redis")
    void testShouldReturnNullWhenNoMatchForEfectePersonIsFoundInILoq() throws Exception {
        String keyHolderEntityId = "12345";

        when(redis.get(anyString())).thenReturn(null);

        String firstName = "John Robert";
        String lastName = "Smith";
        EfecteEntity efectePerson = new EfecteEntityBuilder()
                .withId(keyHolderEntityId)
                .withFirstName(firstName)
                .withLastName(lastName)
                .build();

        setDefaultResponses();

        mocked.getGetEfecteEntity()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(efectePerson)));
        mocked.getListILoqPersons().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of()));

        String iLoqPersonId = iLoqPersonResolver.resolveILoqPersonId(keyHolderEntityId);

        assertThat(iLoqPersonId).isNull();
    }

    @Test
    @DisplayName("resolveILoqPersonId - person id is not found at Redis")
    void testShouldSaveTheEfectePersonNameToRedisWhenNoILoqPersonMatchIsFound() throws Exception {
        String keyHolderEntityId = "12345";

        when(redis.get(anyString())).thenReturn(null);

        String firstName = "John Robert";
        String lastName = "Smith";
        EfecteEntity efectePerson = new EfecteEntityBuilder()
                .withId(keyHolderEntityId)
                .withFirstName(firstName)
                .withLastName(lastName)
                .build();

        setDefaultResponses();

        mocked.getGetEfecteEntity()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(efectePerson)));
        mocked.getListILoqPersons().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of()));

        String iLoqPersonId = iLoqPersonResolver.resolveILoqPersonId(keyHolderEntityId);

        assertThat(iLoqPersonId).isNull();
    }

    @Test
    @DisplayName("resolveILoqPersonId - person id is not found at Redis")
    void testShouldThrowAnExceptionWhenListingILoqPersonsFails() throws Exception {
        String keyHolderEntityId = "12345";

        setDefaultResponses();

        String exceptionMessage = "Oh no! An exception!";
        String expectedExceptionMessage = "ILoqPersonResolver: Listing iLOQ persons failed: "
                + exceptionMessage;

        mocked.getListILoqPersons()
                .whenAnyExchangeReceived(
                        exchange -> {
                            Exception exception = new Exception(exceptionMessage);
                            exchange.setException(exception);
                            exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);
                        });

        assertThatThrownBy(() -> iLoqPersonResolver.resolveILoqPersonId(keyHolderEntityId))
                .hasMessage(expectedExceptionMessage);
    }

    @Test
    @DisplayName("resolveILoqPersonIdForOutsider")
    void testShouldGetTheStoredILoqPersonIdFromRedis_Outsider() throws Exception {
        String outsiderName = "John Smith";
        String outsiderEmail = "john.smith@outsider.com";
        String uniqueIdentifier = "this is a unique identifier value of email and name";
        String expectedEfectePrefix = ri.getMappedPersonEfectePrefix() + uniqueIdentifier;

        when(redis.get(expectedEfectePrefix)).thenReturn("something but not null");
        when(helper.createIdentifier(anyString(), anyString())).thenReturn(uniqueIdentifier);

        verifyNoInteractions(redis);

        iLoqPersonResolver.resolveILoqPersonIdForOutsider(outsiderEmail, outsiderName);

        verify(redis).get(expectedEfectePrefix);
        verify(helper).createIdentifier(outsiderEmail, outsiderName);
    }

    @Test
    @DisplayName("resolveILoqPersonIdForOutsider - person id is not found at Redis")
    void testShouldGetTheILoqPersonByExternalId_Outsider() throws Exception {
        String outsiderEmail = "john.smith@outsider.com";
        String outsiderName = "John Smith";
        String expectedUniqueIdentifier = "irrelevant unique identifier value of email and name";
        String expectedUrlEncodedValue = "irrelevant URL encoded value of unique identifier value";

        when(helper.createIdentifier(any(), any())).thenReturn(expectedUniqueIdentifier);
        when(helper.urlEncode(any())).thenReturn(expectedUrlEncodedValue);
        mocked.getGetILoqPersonByExternalId()
                .whenAnyExchangeReceived(
                        exchange -> exchange.getIn()
                                .setBody(List.of(new ILoqPerson("irrelevant"))));

        mocked.getGetILoqPersonByExternalId().expectedMessageCount(1);
        mocked.getGetILoqPersonByExternalId().expectedPropertyReceived("externalPersonId",
                expectedUrlEncodedValue);
        mocked.getListILoqPersons().expectedMessageCount(0);
        verifyNoInteractions(helper);

        iLoqPersonResolver.resolveILoqPersonIdForOutsider(outsiderEmail, outsiderName);

        verify(helper).createIdentifier(outsiderEmail, outsiderName);
        verify(helper).urlEncode(expectedUniqueIdentifier);
        MockEndpoint.assertIsSatisfied(
                mocked.getGetILoqPersonByExternalId(),
                mocked.getListILoqPersons());
    }

    @Test
    @DisplayName("resolveILoqPersonId - person id is not found at Redis")
    void testShouldSaveTheMappedKeyForTheResolvedILoqPersonId_ByExternalId_Outsider() throws Exception {
        String firstName = "John Robert";
        String lastName = "Smith";
        String outsiderName = firstName + " " + lastName;
        String outsiderEmail = "john.smith@outsider.com";
        String expectedILoqPersonId = "abc-123";
        String uniqueIdentifier = "this is a unique identifier of email and name";
        ILoqPerson iLoqPerson = new ILoqPerson(expectedILoqPersonId);

        String expectedEfectePrefix = ri.getMappedPersonEfectePrefix() + uniqueIdentifier;

        when(helper.createIdentifier(any(), any())).thenReturn(uniqueIdentifier);
        mocked.getGetILoqPersonByExternalId().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(
                List.of(iLoqPerson)));

        verifyNoInteractions(redis);

        iLoqPersonResolver.resolveILoqPersonIdForOutsider(outsiderEmail, outsiderName);

        verify(redis).set(expectedEfectePrefix, expectedILoqPersonId);
    }

    @Test
    @DisplayName("resolveILoqPersonId/resolveILoqPersonIdForOutsider - person id is not found at Redis")
    void testShouldSetAnAuditMessageWhenMultipleILoqPersonsAreFound_ByName() throws Exception {
        String keyHolderEntityId = "12345";

        when(redis.get(anyString())).thenReturn(null);

        String firstName = "John Robert";
        String lastName = "Smith";
        EfecteEntity efectePerson = new EfecteEntityBuilder()
                .withId(keyHolderEntityId)
                .withFirstName(firstName)
                .withLastName(lastName)
                .build();

        ILoqPerson iLoqPerson1 = new ILoqPerson(firstName, lastName, "1");
        ILoqPerson iLoqPerson2 = new ILoqPerson(firstName, lastName, "2");

        String expectedAuditMessage = "Could not find a unique match for the key holder named '" + firstName
                + " "
                + lastName + "' (multiple iLOQ persons found)";

        setDefaultResponses();

        mocked.getGetEfecteEntity()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(efectePerson)));
        mocked.getListILoqPersons().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(
                iLoqPerson1, iLoqPerson2)));

        verifyNoInteractions(redis);

        String iLoqPersonId1 = iLoqPersonResolver.resolveILoqPersonId(keyHolderEntityId);
        String iLoqPersonId2 = iLoqPersonResolver.resolveILoqPersonIdForOutsider("irrelevant outsider email",
                firstName + " " + lastName);

        assertThat(iLoqPersonId1).isNull();
        assertThat(iLoqPersonId2).isNull();
        verify(redis, times(2)).set(ri.getAuditMessagePrefix(), expectedAuditMessage);
    }

    @Test
    @DisplayName("resolveILoqPersonIdForOutsider - person id is not found at Redis")
    void testShouldFilterTheMatchingILoqPersonByEfecteKeyOutsiderNameAndReturnTheILoqPersonId() throws Exception {
        String firstName = "John Robert";
        String lastName = "Smith";
        String outsiderName = firstName + " " + lastName;

        String expectedILoqPersonId = "2";
        ILoqPerson iLoqPerson1 = new ILoqPerson("foo", "bar", "1");
        ILoqPerson iLoqPerson2 = new ILoqPerson(firstName, lastName, expectedILoqPersonId);
        ILoqPerson iLoqPerson3 = new ILoqPerson("Jane", "Doe", "3");

        setDefaultResponses();

        mocked.getListILoqPersons().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of(
                iLoqPerson1, iLoqPerson2, iLoqPerson3)));

        String iLoqPersonId = iLoqPersonResolver.resolveILoqPersonIdForOutsider("irrelevant outsider email",
                outsiderName);

        assertThat(iLoqPersonId).isEqualTo(expectedILoqPersonId);
    }

    @Test
    @DisplayName("resolveILoqPersonIdForOutsider - person id is not found at Redis")
    void testShouldSaveTheMappedKeysForTheResolvedILoqPersonId_ByName_Outsider() throws Exception {
        String firstName = "John Robert";
        String lastName = "Smith";
        String outsiderName = firstName + " " + lastName;
        String outsiderEmail = "john.smith@outsider.com";

        EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier();
        efecteEntityIdentifier.setOutsiderName(outsiderName);
        efecteEntityIdentifier.setOutsiderEmail(outsiderEmail);
        String expectedEfecteEntityIdentifier = "fake json";
        String uniqueIdentifier = "this is a unique identifier value of email and name";
        String expectedILoqPersonId = "2";
        ILoqPerson iLoqPerson = new ILoqPerson(firstName, lastName, expectedILoqPersonId);

        String expectedEfectePrefix = ri.getMappedPersonEfectePrefix() + uniqueIdentifier;
        String expectedILoqPrefix = ri.getMappedPersonILoqPrefix() + expectedILoqPersonId;

        setDefaultResponses();

        when(helper.writeAsJson(any())).thenReturn(expectedEfecteEntityIdentifier);
        when(helper.createIdentifier(any(), any())).thenReturn(uniqueIdentifier);
        mocked.getListILoqPersons().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(
                List.of(iLoqPerson)));

        verifyNoInteractions(redis);

        iLoqPersonResolver.resolveILoqPersonIdForOutsider(outsiderEmail, outsiderName);

        verify(redis).set(expectedEfectePrefix, expectedILoqPersonId);
        verify(redis).set(expectedILoqPrefix, expectedEfecteEntityIdentifier);
        verify(helper).writeAsJson(efecteEntityIdentifier);
    }

    @Test
    @DisplayName("resolveILoqPersonId/resolveILoqPersonIdForOutsider - person id is not found at Redis")
    void testShouldSetAnAuditMessageWhenMultipleILoqPersonsAreFound_ByExternalId_Outsider() throws Exception {
        String outsiderName = "John Smith";
        String outsiderEmail = "john.smith@outsider.com";
        String uniqueIdentifier = "this is a unique identifier value of email and name";

        ILoqPerson iLoqPerson1 = new ILoqPerson("1");
        ILoqPerson iLoqPerson2 = new ILoqPerson("2");

        String expectedAuditMessage = "Found multiple iLOQ persons when using '" + uniqueIdentifier
                + "' as the external id";

        when(helper.createIdentifier(any(), any())).thenReturn(uniqueIdentifier);
        mocked.getGetILoqPersonByExternalId()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(
                        List.of(iLoqPerson1, iLoqPerson2)));
        when(redis.exists(anyString())).thenReturn(true);

        verifyNoInteractions(redis);

        String iLoqPersonId = iLoqPersonResolver.resolveILoqPersonIdForOutsider(outsiderEmail, outsiderName);

        assertThat(iLoqPersonId).isNull();
        verify(redis).set(ri.getAuditMessagePrefix(), expectedAuditMessage);
    }

    ////////////////////
    // iLOQ -> Efecte //
    ////////////////////

    // @Test
    // @DisplayName("resolveILoqKeyPerson")
    // void testShouldGetTheEfecteKeyHolderIdFromRedis() throws Exception {
    //     String iLoqPersonId = "abc-123";
    //     ILoqKeyResponse iLoqKeyResponse = new ILoqKeyResponse();
    //     iLoqKeyResponse.setPersonId(iLoqPersonId);
    //     Exchange ex = testUtils.createExchange();
    //     ex.setProperty("currentILoqKey", iLoqKeyResponse);

    //     String expectedPrefix = ri.getMappedPersonILoqPrefix() + iLoqPersonId;

    //     verifyNoInteractions(redis);

    //     iLoqPersonResolver.resolveILoqKeyPerson(ex);

    //     verify(redis).get(expectedPrefix);
    // }

    // @Test
    // @DisplayName("resolveILoqKeyPerson")
    // void testShouldGetTheEfecteKeyHolderIdFromRedis() throws Exception {
    //     String iLoqPersonId = "abc-123";
    //     ILoqKeyResponse iLoqKeyResponse = new ILoqKeyResponse();
    //     iLoqKeyResponse.setPersonId(iLoqPersonId);
    //     Exchange ex = testUtils.createExchange();
    //     ex.setProperty("currentILoqKey", iLoqKeyResponse);

    //     String expectedPrefix = ri.getMappedPersonILoqPrefix() + iLoqPersonId;

    //     verifyNoInteractions(redis);

    //     iLoqPersonResolver.resolveILoqKeyPerson(ex);

    //     verify(redis).get(expectedPrefix);
    // }

    private void setDefaultResponses() throws Exception {
        when(redis.get(anyString())).thenReturn(null);
        mocked.getGetILoqPersonByExternalId()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of()));
        mocked.getGetEfecteEntity().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(
                List.of(new EfecteEntityBuilder().withDefaults(EnumEfecteTemplate.PERSON).build())));
        mocked.getListILoqPersons().whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(List.of()));
    }

}
