package fi.hel.resolvers;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.ILoqPerson;
import fi.hel.models.enumerations.EnumDirection;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.processors.ResourceInjector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@SuppressWarnings("unchecked")
public class EfectePersonResolver {

    @Inject
    ResourceInjector ri;
    @ConfigProperty(name = "AUDIT_EXCEPTION_EXPIRATION_SECONDS")
    long auditExceptionExpirationTime;

    private static final String ACTIVE_STATUS = "Active";

    ////////////////////
    // iLOQ -> Efecte //
    ////////////////////

    public String resolveEfectePersonIdentifier(ILoqPerson iLoqPerson) throws Exception {
        String iLoqPersonId = iLoqPerson.getPersonId();

        String cc = ri.getRedis().get(ri.getILoqCurrentCustomerCodePrefix());
        String efectePersonIdentifierJson = ri.getRedis().get(ri.getMappedPersonILoqPrefix() + cc + ":" + iLoqPersonId);
        String efectePersonIdentifierValue = null;

        if (efectePersonIdentifierJson == null) {
            // The iLOQ person's ExternalPersonId is the Efecte person entity id when the person has been linked earlier
            EfecteEntity efecteKeyHolder = getEfectePersonByExternalPersonId(iLoqPerson.getExternalPersonId());

            if (efecteKeyHolder == null) {
                String firstName = getNormalizedString(iLoqPerson.getFirstName());
                String lastName = getNormalizedString(iLoqPerson.getLastName());

                List<EfecteEntity> efectePersons = getEfectePerson(firstName, lastName);

                if (efectePersons.isEmpty()) {
                    return firstName + " " + lastName;
                } else if (efectePersons.size() > 1) {
                    List<EfecteEntity> activeEfectePersons = getActiveEfectePersons(efectePersons);

                    if (activeEfectePersons.size() == 1) {
                        efectePersons = activeEfectePersons;
                    } else {
                        // When multiple Efecte persons match, store them in auditRecord to allow the user to select the correct match in the UI
                        String prefix = ri.getAuditRecordPersonPrefix() + iLoqPersonId;
                        ri.getRedis().setex(prefix, efectePersons.toString(), auditExceptionExpirationTime);

                        String auditMessage = "Multiple matching key holders found for iLOQ person '" + firstName + " "
                                + lastName
                                + "' at Efecte";

                        ri.getAuditExceptionProcessor().throwAuditException(
                                EnumDirection.ILOQ, EnumDirection.EFECTE, null, null, iLoqPersonId, auditMessage);
                    }
                }

                efecteKeyHolder = efectePersons.get(0);
            }

            efectePersonIdentifierValue = efecteKeyHolder.getId();
            saveMappedKeys(cc, iLoqPersonId, efecteKeyHolder);
        } else {
            EfecteEntityIdentifier efecteEntityIdentifier = ri.getHelper().writeAsPojo(efectePersonIdentifierJson,
                    EfecteEntityIdentifier.class);

            if (efecteEntityIdentifier.getOutsiderEmail() != null) {
                efectePersonIdentifierValue = efectePersonIdentifierJson;
            } else {
                efectePersonIdentifierValue = efecteEntityIdentifier.getEntityId();
            }
        }

        return efectePersonIdentifierValue;
    }

    private List<EfecteEntity> getEfectePerson(String firstName, String lastName) throws Exception {
        String query = """
                SELECT entity
                FROM entity
                WHERE
                    template.code = 'person'
                    AND $%s$ = '%s'
                    AND $%s$ = '%s'
                """.formatted(
                EnumEfecteAttribute.PERSON_FIRSTNAME.getCode(), ri.getHelper().urlEncode(firstName),
                EnumEfecteAttribute.PERSON_LASTNAME.getCode(), ri.getHelper().urlEncode(lastName))
                .replaceAll("\\s+", " ")
                .trim();

        return fetchEfectePersons(query);
    }

    private List<EfecteEntity> fetchEfectePersons(String query) throws Exception {
        Exchange ex = new ExchangeBuilder(ri.getContext())
                .withProperty("efecteEntityType", "person")
                .withProperty("efecteQuery", query)
                .build();

        ri.getTemplate().send(ri.getGetEfecteEntityEndpointUri(), ex);

        if (ex.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw new Exception(
                    "EfecteKeyHolderResolver.resolveEfecteKeyHolderEntityId: Fetching the Efecte person failed: "
                            + ex.getException().getMessage());
        }

        return (List<EfecteEntity>) ex.getIn().getBody(List.class);
    }

    private EfecteEntity getEfectePersonByExternalPersonId(String externalPersonId) throws Exception {
        // Outsiders have an email based identifier as the ExternalPersonId, only Efecte entity ids are numeric
        if (externalPersonId == null || !externalPersonId.matches("\\d+")) {
            return null;
        }

        String query = """
                SELECT entity
                FROM entity
                WHERE
                    template.code = 'person'
                    AND entity.id = '%s'
                """.formatted(externalPersonId)
                .replaceAll("\\s+", " ")
                .trim();

        List<EfecteEntity> efectePersons = fetchEfectePersons(query);

        if (efectePersons == null || efectePersons.isEmpty()) {
            return null;
        }

        EfecteEntity efectePerson = efectePersons.get(0);
        String status = getStatus(efectePerson);

        // An inactive person is ignored, the person is then resolved by name instead
        if (status != null && !status.equals(ACTIVE_STATUS)) {
            return null;
        }

        return efectePerson;
    }

    private List<EfecteEntity> getActiveEfectePersons(List<EfecteEntity> efectePersons) {
        return efectePersons.stream()
                .filter(efectePerson -> ACTIVE_STATUS.equals(getStatus(efectePerson)))
                .toList();
    }

    private String getStatus(EfecteEntity efectePerson) {
        try {
            return efectePerson.getAttributeValue(EnumEfecteAttribute.PERSON_STATUS);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveMappedKeys(String cc, String iLoqPersonId, EfecteEntity efecteKeyHolder) throws Exception {
        String entityId = efecteKeyHolder.getId();
        String efecteId = efecteKeyHolder.getAttributeValue(EnumEfecteAttribute.PERSON_EFECTE_ID);
        EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier(entityId, efecteId);
        String efecteEntityIdentifierJson = ri.getHelper().writeAsJson(efecteEntityIdentifier);

        String efectePrefix = ri.getMappedPersonEfectePrefix() + cc + ":" + entityId;
        String iLoqPrefix = ri.getMappedPersonILoqPrefix() + cc + ":" + iLoqPersonId;

        ri.getRedis().set(efectePrefix, iLoqPersonId);
        ri.getRedis().set(iLoqPrefix, efecteEntityIdentifierJson);
    }

    private String getNormalizedString(String str) {
        return str.trim().replaceAll("\\s+", " ");
    }
}
