package fi.hel.resolvers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;

import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.ILoqPerson;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.processors.ResourceInjector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named("iLoqPersonResolver")
@SuppressWarnings("unchecked")
public class ILoqPersonResolver {

    @Inject
    ResourceInjector ri;

    ////////////////////
    // Efecte -> iLOQ //
    ////////////////////

    private List<ILoqPerson> iLoqPersons;

    public String resolveILoqPersonId(String keyHolderEntityId)
            throws Exception {
        String iLoqPersonId = getMappedId(keyHolderEntityId);

        if (iLoqPersonId != null) {
            return iLoqPersonId;
        }

        if (ri.getRedis().exists(ri.getAuditMessagePrefix())) {
            return null;
        }

        EfecteEntity efectePerson = getEfectePerson(keyHolderEntityId);
        String firstName = efectePerson.getAttributeValue(EnumEfecteAttribute.PERSON_FIRSTNAME);
        String lastName = efectePerson.getAttributeValue(EnumEfecteAttribute.PERSON_LASTNAME);

        if (iLoqPersons == null) {
            iLoqPersons = listILoqPersons();
        }

        iLoqPersonId = findMatchingPersonId(iLoqPersons, firstName, lastName);

        if (iLoqPersonId != null) {
            String personEfecteId = efectePerson.getAttributeValue(EnumEfecteAttribute.PERSON_EFECTE_ID);
            EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier(
                    keyHolderEntityId, personEfecteId);

            ri.getRedis().set(ri.getMappedPersonEfectePrefix() + keyHolderEntityId, iLoqPersonId);
            ri.getRedis().set(ri.getMappedPersonILoqPrefix() + iLoqPersonId,
                    ri.getHelper().writeAsJson(efecteEntityIdentifier));
        }

        return iLoqPersonId;
    };

    public String resolveILoqPersonIdForOutsider(String outsiderEmail, String outsiderName) throws Exception {
        String uniqueIdentifier = ri.getHelper().createIdentifier(outsiderEmail, outsiderName);
        String iLoqPersonId = getMappedId(uniqueIdentifier);

        if (iLoqPersonId != null) {
            return iLoqPersonId;
        }

        if (ri.getRedis().exists(ri.getAuditMessagePrefix())) {
            return null;
        }

        if (iLoqPersons == null) {
            iLoqPersons = listILoqPersons();
        }

        Map<String, String> nameMap = getName(outsiderName);
        iLoqPersonId = findMatchingPersonId(iLoqPersons, nameMap.get("firstName"), nameMap.get("lastName"));

        if (iLoqPersonId != null) {
            EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier();
            efecteEntityIdentifier.setOutsiderName(outsiderName);
            efecteEntityIdentifier.setOutsiderEmail(outsiderEmail);

            ri.getRedis().set(
                    ri.getMappedPersonEfectePrefix() + uniqueIdentifier, iLoqPersonId);
            ri.getRedis().set(ri.getMappedPersonILoqPrefix() + iLoqPersonId,
                    ri.getHelper().writeAsJson(efecteEntityIdentifier));
        }

        return iLoqPersonId;
    }

    public void resetCache() {
        this.iLoqPersons = null;
    }

    private String getMappedId(String externalId) throws Exception {
        String iLoqPersonId = ri.getRedis().get(ri.getMappedPersonEfectePrefix() + externalId);

        if (iLoqPersonId != null) {
            return iLoqPersonId;
        }

        List<ILoqPerson> iLoqPersonsByExternalId = getILoqPersonByExternalId(externalId);

        if (iLoqPersonsByExternalId.size() == 1) {
            iLoqPersonId = iLoqPersonsByExternalId.get(0).getPersonId();
            ri.getRedis().set(ri.getMappedPersonEfectePrefix() + externalId, iLoqPersonId);

            return iLoqPersonId;
        } else if (iLoqPersonsByExternalId.size() > 1) {
            ri.getRedis().set(ri.getAuditMessagePrefix(),
                    "Found multiple iLOQ persons when using '" + externalId + "' as the external id");
        }

        return null;
    }

    private EfecteEntity getEfectePerson(String personEntityId) throws Exception {
        String query = "SELECT entity FROM entity WHERE entity.id = '%s'"
                .formatted(personEntityId);

        Exchange ex = new ExchangeBuilder(ri.getContext())
                .withProperty("efecteEntityType", "person")
                .withProperty("efecteQuery", query)
                .build();

        List<EfecteEntity> efectePersons = ri.getTemplate().send(ri.getGetEfecteEntityEndpointUri(), ex)
                .getIn().getBody(List.class);
        EfecteEntity efectePerson = efectePersons.get(0);

        ri.getRedis().set(
                ri.getTempEfectePersonPrefix() + personEntityId,
                ri.getHelper().writeAsJson(efectePerson));

        return efectePerson;
    }

    private List<ILoqPerson> getILoqPersonByExternalId(String efecteId)
            throws Exception {
        String urlEncodedExternalId = ri.getHelper().urlEncode(efecteId);
        Exchange ex = new ExchangeBuilder(ri.getContext())
                .withProperty("externalPersonId", urlEncodedExternalId)
                .build();

        List<ILoqPerson> list = ri.getTemplate().send(ri.getGetILoqPersonByExternalIdEndpointUri(), ex)
                .getIn()
                .getBody(List.class);

        if (ex.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw new Exception(
                    "ILoqPersonResolver: Fetching iLOQ persons by external person id failed: "
                            + ex.getException().getMessage());
        }

        return list;
    }

    private List<ILoqPerson> listILoqPersons()
            throws Exception {
        Exchange ex = new ExchangeBuilder(ri.getContext()).build();

        List<ILoqPerson> list = ri.getTemplate().send(ri.getListILoqPersonsEndpointUri(), ex)
                .getIn()
                .getBody(List.class);

        if (ex.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw new Exception(
                    "ILoqPersonResolver: Listing iLOQ persons failed: "
                            + ex.getException().getMessage());
        }

        return list;
    }

    private String findMatchingPersonId(List<ILoqPerson> iLoqPersons, String firstName, String lastName)
            throws Exception {
        List<ILoqPerson> filteredPersons = iLoqPersons.stream()
                .filter(person -> getNormalizedString(person.getFirstName()).equals(getNormalizedString(firstName))
                        && getNormalizedString(person.getLastName()).equals(getNormalizedString(lastName)))
                .toList();

        if (filteredPersons.isEmpty()) {
            System.out.println("No matching persons found");
            return null;
        }

        if (filteredPersons.size() > 1) {
            System.out.println("Multiple matches found");
            ri.getRedis().set(ri.getAuditMessagePrefix(),
                    "Could not find a unique match for the key holder named '" + firstName + " "
                            + lastName + "' (multiple iLOQ persons found)");
            return null;
        }

        System.out.println("Found one match");

        return filteredPersons.get(0).getPersonId();
    }

    private String getNormalizedString(String str) {
        return str.trim().replaceAll("\\s+", " ").toLowerCase();
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

}
