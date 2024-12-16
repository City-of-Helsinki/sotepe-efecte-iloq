package fi.hel.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.quarkus.test.CamelQuarkusTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.devikone.test_utils.MockEndpointInjector;
import com.devikone.transports.Redis;

import fi.hel.mappers.ILoqPersonMapper;
import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.ILoqPersonImport;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class ILoqPersonProcessorTest extends CamelQuarkusTestSupport {

    @Inject
    MockEndpointInjector mocked;
    @Inject
    ResourceInjector ri;
    @Inject
    ILoqPersonProcessor iLoqPersonProcessor;
    @InjectMock
    ILoqPersonMapper iLoqPersonMapper;
    @InjectMock
    Helper helper;
    @InjectMock
    Redis redis;

    @Override
    protected void doPreSetup() throws Exception {
        super.doPostSetup();
        testConfiguration().withUseRouteBuilder(false);
    }

    @Test
    @DisplayName("createILoqPerson - Efecte key has a key holder")
    void testShouldFetchTheEfecteKeyHolder() throws Exception {
        String keyHolderEntityId = "12345";
        EfecteEntity efecteKey = new EfecteEntityBuilder()
                .withKeyHolderReference(keyHolderEntityId)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String expectedPrefix = ri.getTempEfectePersonPrefix() + keyHolderEntityId;
        String expectedEfectePersonXml = "irrelevant person xml";

        when(iLoqPersonMapper.mapToNewILoqPerson(any())).thenReturn(new ILoqPersonImport());
        when(redis.get(anyString())).thenReturn(expectedEfectePersonXml);
        when(helper.writeAsPojo(expectedEfectePersonXml, EfecteEntity.class)).thenReturn(
                new EfecteEntityBuilder().withDefaults(EnumEfecteTemplate.PERSON).build());
        mocked.getCreateILoqPerson()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("irrelevant"));
        when(helper.writeAsJson(any())).thenReturn("efecte entity identifier json");

        verifyNoInteractions(redis);

        iLoqPersonProcessor.createILoqPerson(efecteKey, List.of());

        verify(redis).get(expectedPrefix);
    }

    @Test
    @DisplayName("createILoqPerson - Efecte key has a key holder")
    void testShouldMapANewILoqPersonWithTheEfectePersonEntity() throws Exception {
        EfecteEntity efecteKey = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        EfecteEntity expectedEfectePersonEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();

        when(helper.writeAsPojo(any(), any())).thenReturn(expectedEfectePersonEntity);
        when(iLoqPersonMapper.mapToNewILoqPerson(expectedEfectePersonEntity))
                .thenReturn(new ILoqPersonImport());
        mocked.getCreateILoqPerson()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("irrelevant"));
        when(helper.writeAsJson(any())).thenReturn("efecte entity identifier json");

        verifyNoInteractions(iLoqPersonMapper);

        iLoqPersonProcessor.createILoqPerson(efecteKey, List.of());

        verify(iLoqPersonMapper).mapToNewILoqPerson(expectedEfectePersonEntity);
    }

    @Test
    @DisplayName("createILoqPerson - Efecte key has a key holder")
    void testShouldForwardTheCreatedILoqPersonImportEnrichedWithZoneIdsToILoq_KeyHolder() throws Exception {
        EfecteEntity efecteKey = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        List<String> expectedZoneIds = List.of("1", "2");
        ILoqPersonImport iLoqPersonImport = new ILoqPersonImport();

        when(iLoqPersonMapper.mapToNewILoqPerson(any())).thenReturn(iLoqPersonImport);
        when(helper.writeAsPojo(any(), any()))
                .thenReturn(new EfecteEntityBuilder().withDefaults(EnumEfecteTemplate.PERSON).build());
        mocked.getCreateILoqPerson()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("irrelevant"));
        when(helper.writeAsJson(any())).thenReturn("efecte entity identifier json");

        mocked.getCreateILoqPerson().expectedMessageCount(1);
        mocked.getCreateILoqPerson().expectedPropertyReceived("newILoqPerson", iLoqPersonImport);

        iLoqPersonProcessor.createILoqPerson(efecteKey, expectedZoneIds);

        mocked.getCreateILoqPerson().assertIsSatisfied();

        ILoqPersonImport newILoqPerson = mocked.getCreateILoqPerson().getExchanges().get(0)
                .getProperty("newILoqPerson", ILoqPersonImport.class);

        assertThat(newILoqPerson.getZoneIds()).isEqualTo(expectedZoneIds);
    }

    @Test
    @DisplayName("createILoqPerson - Efecte key has a key holder")
    void testShouldSaveTheMappedIdsForThePerson_KeyHolder() throws Exception {
        EfecteEntity efecteKey = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String personEntityId = "12345";
        String personEfecteId = "PER_00123";
        String expectedILoqPersonId = "abc-123";
        String expectedEfecteEntityIdentifierJson = "this is fake json";

        EfecteEntity efectePersonEntity = new EfecteEntityBuilder()
                .withId(personEntityId)
                .withPersonEfecteId(personEfecteId)
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();
        EfecteEntityIdentifier entityIdentifier = new EfecteEntityIdentifier(personEntityId, personEfecteId);

        when(iLoqPersonMapper.mapToNewILoqPerson(any())).thenReturn(new ILoqPersonImport());
        when(helper.writeAsPojo(any(), any())).thenReturn(efectePersonEntity);
        mocked.getCreateILoqPerson()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(expectedILoqPersonId));
        when(helper.writeAsJson(entityIdentifier)).thenReturn(expectedEfecteEntityIdentifierJson);

        String expectedEfectePrefix = ri.getMappedPersonEfectePrefix() + personEntityId;
        String expectedILoqPrefix = ri.getMappedPersonILoqPrefix() + expectedILoqPersonId;

        verifyNoInteractions(helper);
        verifyNoInteractions(redis);

        iLoqPersonProcessor.createILoqPerson(efecteKey, List.of());

        verify(helper).writeAsJson(entityIdentifier);
        verify(redis).set(expectedEfectePrefix, expectedILoqPersonId);
        verify(redis).set(expectedILoqPrefix, expectedEfecteEntityIdentifierJson);
    }

    @Test
    @DisplayName("createILoqPerson - Efecte key has an outsider key holder")
    void testShouldMapANewILoqPersonWithTheEfecteKeyEntity() throws Exception {
        EfecteEntity expectedEfecteKey = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        when(iLoqPersonMapper.mapToNewILoqPersonForOutsider(expectedEfecteKey))
                .thenReturn(new ILoqPersonImport());
        mocked.getCreateILoqPerson()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("irrelevant"));
        when(helper.writeAsJson(any())).thenReturn("efecte entity identifier json");

        verifyNoInteractions(iLoqPersonMapper);

        iLoqPersonProcessor.createILoqPerson(expectedEfecteKey, List.of());

        verify(iLoqPersonMapper).mapToNewILoqPersonForOutsider(expectedEfecteKey);
    }

    @Test
    @DisplayName("createILoqPerson - Efecte key has an outsider key holder")
    void testShouldForwardTheCreatedILoqPersonImportEnrichedWithZoneIdsToILoq_Outsider() throws Exception {
        EfecteEntity efecteKey = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        List<String> expectedZoneIds = List.of("1", "2");
        ILoqPersonImport iLoqPersonImport = new ILoqPersonImport();

        when(iLoqPersonMapper.mapToNewILoqPersonForOutsider(any())).thenReturn(iLoqPersonImport);
        mocked.getCreateILoqPerson()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("irrelevant"));
        when(helper.writeAsJson(any())).thenReturn("efecte entity identifier json");

        mocked.getCreateILoqPerson().expectedMessageCount(1);
        mocked.getCreateILoqPerson().expectedPropertyReceived("newILoqPerson", iLoqPersonImport);

        iLoqPersonProcessor.createILoqPerson(efecteKey, expectedZoneIds);

        mocked.getCreateILoqPerson().assertIsSatisfied();

        ILoqPersonImport newILoqPerson = mocked.getCreateILoqPerson().getExchanges().get(0)
                .getProperty("newILoqPerson", ILoqPersonImport.class);

        assertThat(newILoqPerson.getZoneIds()).isEqualTo(expectedZoneIds);
    }

    @Test
    @DisplayName("createILoqPerson - Efecte key has an outsider key holder")
    void testShouldSaveTheMappedIdsForThePerson_Outsider() throws Exception {
        String outsiderName = "Some Name";
        String outsiderEmail = "john.smith@outsider.com";
        EfecteEntity efecteKey = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(outsiderName)
                .withOutsiderEmail(outsiderEmail)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String expectedILoqPersonId = "abc-123";
        String expectedEfecteEntityIdentifierJson = "this is fake json";
        String efectePersonIdentifier = "this is a unique identifier value of email and name";

        EfecteEntityIdentifier entityIdentifier = new EfecteEntityIdentifier();
        entityIdentifier.setOutsiderName(outsiderName);
        entityIdentifier.setOutsiderEmail(outsiderEmail);

        when(iLoqPersonMapper.mapToNewILoqPersonForOutsider(any())).thenReturn(new ILoqPersonImport());
        mocked.getCreateILoqPerson()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody(expectedILoqPersonId));
        when(helper.writeAsJson(entityIdentifier)).thenReturn(expectedEfecteEntityIdentifierJson);
        when(helper.createIdentifier(outsiderEmail, outsiderName)).thenReturn(efectePersonIdentifier);

        String expectedEfectePrefix = ri.getMappedPersonEfectePrefix() + efectePersonIdentifier;
        String expectedILoqPrefix = ri.getMappedPersonILoqPrefix() + expectedILoqPersonId;

        verifyNoInteractions(helper);
        verifyNoInteractions(redis);

        iLoqPersonProcessor.createILoqPerson(efecteKey, List.of());

        verify(helper).writeAsJson(entityIdentifier);
        verify(helper).createIdentifier(outsiderEmail, outsiderName);
        verify(redis).set(expectedEfectePrefix, expectedILoqPersonId);
        verify(redis).set(expectedILoqPrefix, expectedEfecteEntityIdentifierJson);
    }

    @Test
    @DisplayName("createILoqPerson")
    void testShouldThrowAnExceptionWhenCreatingAnILoqPersonFails() throws Exception {
        EfecteEntity efecteKey = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        ILoqPersonImport iLoqPersonImport = new ILoqPersonImport();

        when(iLoqPersonMapper.mapToNewILoqPerson(any())).thenReturn(iLoqPersonImport);
        when(helper.writeAsPojo(any(), any()))
                .thenReturn(new EfecteEntityBuilder().withDefaults(EnumEfecteTemplate.PERSON).build());
        mocked.getCreateILoqPerson()
                .whenAnyExchangeReceived(exchange -> exchange.getIn().setBody("irrelevant"));
        when(helper.writeAsJson(any())).thenReturn("efecte entity identifier json");

        String exceptionMessage = "Oh no! An exception!";
        String expectedExceptionMessage = "ILoqPersonProcessor: Creating an iLOQ person failed: "
                + exceptionMessage;

        mocked.getCreateILoqPerson()
                .whenAnyExchangeReceived(exchange -> {

                    Exception exception = new Exception(exceptionMessage);
                    exchange.setException(exception);
                    exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);
                });

        assertThatThrownBy(() -> iLoqPersonProcessor.createILoqPerson(efecteKey, List.of()))
                .hasMessage(expectedExceptionMessage);

    }
}
