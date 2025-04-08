package fi.hel.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import fi.hel.models.EfecteEntity;
import fi.hel.models.ILoqPersonImport;
import fi.hel.models.builders.EfecteEntityBuilder;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteTemplate;
import fi.hel.processors.AuditExceptionProcessor;
import fi.hel.processors.Helper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class ILoqPersonMapperTest {

    @Inject
    ILoqPersonMapper iLoqPersonMapper;
    @InjectMock
    AuditExceptionProcessor auditExceptionProcessor;
    @InjectMock
    Helper helper;

    @Test
    @DisplayName("mapToNewILoqPerson - Person_ID")
    void testShouldCreateAGUIDForPersonId_MapToNewILoqPerson() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();
        String expectedPersonId = "7cc7b0e3-733e-4537-aeae-67da59f128b1";
        when(helper.createGUID()).thenReturn(expectedPersonId);

        verifyNoInteractions(helper);

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPerson(efecteEntity);

        verify(helper).createGUID();
        assertThat(newPerson.getPerson().getPersonId()).isEqualTo(expectedPersonId);
    }

    @Test
    @DisplayName("mapToNewILoqPerson - FirstName")
    void testShouldMapTheFirstName_MapToNewILoqPerson() throws Exception {
        String expectedFirstName = "John";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withFirstName(expectedFirstName)
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPerson(efecteEntity);

        assertThat(newPerson.getPerson().getFirstName()).isEqualTo(expectedFirstName);
    }

    @Test
    @DisplayName("mapToNewILoqPerson - LastName")
    void testShouldMapTheLasttName_MapToNewILoqPerson() throws Exception {
        String expectedLastName = "Smith";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withLastName(expectedLastName)
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPerson(efecteEntity);

        assertThat(newPerson.getPerson().getLastName()).isEqualTo(expectedLastName);
    }

    @Test
    @DisplayName("mapToNewILoqPerson - ExternalPersonId")
    void testShouldMapTheExternalPersonId_MapToNewILoqPerson() throws Exception {
        String expectedPersonId = "12345";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withId(expectedPersonId)
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPerson(efecteEntity);

        assertThat(newPerson.getPerson().getExternalPersonId()).isEqualTo(expectedPersonId);
    }

    @Test
    @DisplayName("mapToNewILoqPerson - default fields")
    void testShouldMapTheDefaultFields_MapToNewILoqPerson() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPerson(efecteEntity);

        assertThat(newPerson.getPerson().getAddress()).isEmpty();
        assertThat(newPerson.getPerson().getPersonCode()).isEmpty();
        assertThat(newPerson.getPerson().getCompanyName()).isEmpty();
        assertThat(newPerson.getPerson().getContactInfo()).isEmpty();
        assertThat(newPerson.getPerson().getCountry()).isEmpty();
        assertThat(newPerson.getPerson().getDescription()).isEmpty();
        assertThat(newPerson.getPerson().getEmail()).isEmpty();
        assertThat(newPerson.getPerson().getEmploymentEndDate()).isEmpty();
        assertThat(newPerson.getPerson().getExternalCanEdit()).isTrue();
        assertThat(newPerson.getPerson().getLanguageCode()).isEmpty();
        assertThat(newPerson.getPerson().getPhone1()).isEmpty();
        assertThat(newPerson.getPerson().getPhone2()).isEmpty();
        assertThat(newPerson.getPerson().getPhone3()).isEmpty();
        assertThat(newPerson.getPerson().getPostOffice()).isEmpty();
        assertThat(newPerson.getPerson().getState()).isEqualTo(0);
        assertThat(newPerson.getPerson().getWorkTitle()).isEmpty();
        assertThat(newPerson.getPerson().getZipCode()).isEmpty();
    }

    @Test
    @DisplayName("mapToNewILoqPerson - Zones")
    void testShouldMapAnEmptyArrayAsZones_MapToNewILoqPerson() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withDefaults(EnumEfecteTemplate.PERSON)
                .build();

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPerson(efecteEntity);

        assertThat(newPerson.getZoneIds()).isInstanceOf(List.class);
        assertThat(newPerson.getZoneIds()).isEmpty();
    }

    ///////////////////////////////////
    // mapToNewILoqPersonForOutsider //
    ///////////////////////////////////

    @Test
    @DisplayName("mapToNewILoqPersonForOutsider - Person_ID")
    void testShouldCreateAGUIDForPersonId_MapToNewILoqPersonForOutsider() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();
        String expectedPersonId = "7cc7b0e3-733e-4537-aeae-67da59f128b1";
        when(helper.createGUID()).thenReturn(expectedPersonId);

        verifyNoInteractions(helper);

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPersonForOutsider(efecteEntity);

        verify(helper).createGUID();
        assertThat(newPerson.getPerson().getPersonId()).isEqualTo(expectedPersonId);
    }

    @Test
    @DisplayName("mapToNewILoqPersonForOutsider - FirstName")
    void testShouldMapTheFirstName_MapToNewILoqPersonForOutsider() throws Exception {
        String expectedFirstName = "John";
        String outsiderName = expectedFirstName + " Smith";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(outsiderName)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPersonForOutsider(efecteEntity);

        assertThat(newPerson.getPerson().getFirstName()).isEqualTo(expectedFirstName);
    }

    @Test
    @DisplayName("mapToNewILoqPersonForOutsider - FirstName")
    void testShouldMapTheFirstNameWhenKeyHolderNameHasThreeParts() throws Exception {
        String expectedFirstName = "John Robert";
        String outsiderName = expectedFirstName + " Smith";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(outsiderName)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPersonForOutsider(efecteEntity);

        assertThat(newPerson.getPerson().getFirstName()).isEqualTo(expectedFirstName);
    }

    @Test
    @DisplayName("mapToNewILoqPersonForOutsider - LastName")
    void testShouldMapTheLastName_MapToNewILoqPersonForOutsider() throws Exception {
        String expectedLastName = "Smith";
        String outsiderName = "John " + expectedLastName;
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(outsiderName)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPersonForOutsider(efecteEntity);

        assertThat(newPerson.getPerson().getLastName()).isEqualTo(expectedLastName);
    }

    @Test
    @DisplayName("mapToNewILoqPersonForOutsider - LastName")
    void testShouldMapTheLastNameWhenKeyHolderNameHasThreeParts() throws Exception {
        String expectedLastName = "Smith";
        String outsiderName = "John Robert " + expectedLastName;
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderName(outsiderName)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPersonForOutsider(efecteEntity);

        assertThat(newPerson.getPerson().getLastName()).isEqualTo(expectedLastName);
    }

    @Test
    @DisplayName("mapToNewILoqPersonForOutsider - Name")
    void testShouldThrowAnAuditExceptionWhenOutsiderNameContainsMoreThanThreeParts() throws Exception {
        String entityId = "12345";
        String efecteId = "KEY_00012";
        String outsiderName = "John Robert jr Smith";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withId(entityId)
                .withKeyEfecteId(efecteId)
                .withIsOutsider(true)
                .withOutsiderName(outsiderName)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        String auditMessage = """
                The outsider name '%s' contains multiple parts, making it impossible to distinguish first and last names when creating an iLOQ person.
                """
                .formatted(outsiderName);

        doAnswer(i -> {
            throw new Exception();
        }).when(auditExceptionProcessor).throwAuditException(any(), any(), any(), any(), any(), any());

        verifyNoInteractions(auditExceptionProcessor);

        assertThatThrownBy(() -> iLoqPersonMapper.mapToNewILoqPersonForOutsider(efecteEntity));

        verify(auditExceptionProcessor).throwAuditException(
                EnumDirection.EFECTE, EnumDirection.ILOQ, entityId, efecteId, null, auditMessage);
    }

    @Test
    @DisplayName("mapToNewILoqPersonForOutsider - ExternalPersonId")
    void testShouldMapTheExternalPersonId_MapToNewILoqPersonForOutsider() throws Exception {
        String outsiderEmail = "john.smith@outsider.com";
        String outsiderName = "John Smith";
        String expectedExternalPersonId = "unique identifier value of email and name";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderEmail(outsiderEmail)
                .withOutsiderName(outsiderName)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        when(helper.createIdentifier(outsiderEmail, outsiderName)).thenReturn(expectedExternalPersonId);

        verifyNoInteractions(helper);

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPersonForOutsider(efecteEntity);

        verify(helper).createIdentifier(outsiderEmail, outsiderName);

        assertThat(newPerson.getPerson().getExternalPersonId()).isEqualTo(expectedExternalPersonId);
    }

    @Test
    @DisplayName("mapToNewILoqPersonForOutsider - Email")
    void testShouldMapTheEmail() throws Exception {
        String expectedExternalPersonId = "john.smith@outsider.com";
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withOutsiderEmail(expectedExternalPersonId)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPersonForOutsider(efecteEntity);

        assertThat(newPerson.getPerson().getEmail()).isEqualTo(expectedExternalPersonId);
    }

    @Test
    @DisplayName("mapToNewILoqPerson - Zones")
    void testShouldMapAnEmptyArrayAsZones_mMpToNewILoqPersonForOutsider() throws Exception {
        EfecteEntity efecteEntity = new EfecteEntityBuilder()
                .withIsOutsider(true)
                .withDefaults(EnumEfecteTemplate.KEY)
                .build();

        ILoqPersonImport newPerson = iLoqPersonMapper.mapToNewILoqPersonForOutsider(efecteEntity);

        assertThat(newPerson.getZoneIds()).isInstanceOf(List.class);
        assertThat(newPerson.getZoneIds()).isEmpty();
    }
}
