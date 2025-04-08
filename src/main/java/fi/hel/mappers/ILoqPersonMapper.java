package fi.hel.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fi.hel.models.EfecteEntity;
import fi.hel.models.ILoqPerson;
import fi.hel.models.ILoqPersonImport;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.processors.ResourceInjector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ILoqPersonMapper {

    @Inject
    ResourceInjector ri;

    public ILoqPersonImport mapToNewILoqPerson(EfecteEntity efecteKeyHolder) throws Exception {
        ILoqPersonImport newPerson = new ILoqPersonImport();
        ILoqPerson person = new ILoqPerson();

        person.setPersonId(ri.getHelper().createGUID());
        person.setFirstName(efecteKeyHolder.getAttributeValue(EnumEfecteAttribute.PERSON_FIRSTNAME));
        person.setLastName(efecteKeyHolder.getAttributeValue(EnumEfecteAttribute.PERSON_LASTNAME));
        person.setExternalPersonId(efecteKeyHolder.getId());
        person.setEmail("");

        mapDefaultFields(person);

        newPerson.setPerson(person);
        newPerson.setZoneIds(new ArrayList<>());

        return newPerson;
    }

    public ILoqPersonImport mapToNewILoqPersonForOutsider(EfecteEntity efecteKey) throws Exception {
        ILoqPersonImport newPerson = new ILoqPersonImport();
        ILoqPerson person = new ILoqPerson();

        String outsiderName = efecteKey.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_NAME);
        String outsiderEmail = efecteKey.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_EMAIL);

        Map<String, String> nameMap = getName(outsiderName);

        if (nameMap == null) {
            String auditMessage = """
                    The outsider name '%s' contains multiple parts, making it impossible to distinguish first and last names when creating an iLOQ person.
                    """
                    .formatted(efecteKey.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_NAME));
            ri.getAuditExceptionProcessor().throwAuditException(
                    EnumDirection.EFECTE, EnumDirection.ILOQ, efecteKey.getId(),
                    efecteKey.getAttributeValue(EnumEfecteAttribute.KEY_EFECTE_ID), null, auditMessage);
        }

        person.setPersonId(ri.getHelper().createGUID());
        person.setFirstName(nameMap.get("firstName"));
        person.setLastName(nameMap.get("lastName"));
        person.setEmail(outsiderEmail);
        person.setExternalPersonId(ri.getHelper().createIdentifier(outsiderEmail, outsiderName));

        mapDefaultFields(person);

        newPerson.setPerson(person);
        newPerson.setZoneIds(new ArrayList<>());

        return newPerson;
    }

    private Map<String, String> getName(String keyHolderName) {
        String[] keyHolderNameParts = keyHolderName.split(" ");

        String lastName;
        String firstName;

        if (keyHolderNameParts.length == 2) {
            firstName = keyHolderNameParts[0];
            lastName = keyHolderNameParts[1];
        } else if (keyHolderNameParts.length == 3) {
            firstName = keyHolderNameParts[0] + " " + keyHolderNameParts[1];
            lastName = keyHolderNameParts[2];
        } else {
            return null;
        }

        Map<String, String> name = new HashMap<>();
        name.put("firstName", firstName);
        name.put("lastName", lastName);

        return name;
    }

    private void mapDefaultFields(ILoqPerson person) {
        person.setAddress("");
        person.setPersonCode("");
        person.setCompanyName("");
        person.setContactInfo("");
        person.setCountry("");
        person.setDescription("");
        person.setEmploymentEndDate("");
        person.setExternalCanEdit(true);
        person.setLanguageCode("");
        person.setPhone1("");
        person.setPhone2("");
        person.setPhone3("");
        person.setPostOffice("");
        person.setState(0);
        person.setWorkTitle("");
        person.setZipCode("");
    }
}
