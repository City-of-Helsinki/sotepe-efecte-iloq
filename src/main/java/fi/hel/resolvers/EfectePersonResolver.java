package fi.hel.resolvers;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;

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

    ////////////////////
    // iLOQ -> Efecte //
    ////////////////////

    public String resolveEfectePersonIdentifier(ILoqPerson iLoqPerson) throws Exception {
        String iLoqPersonId = iLoqPerson.getPersonId();

        String efectePersonIdentifierJson = ri.getRedis().get(ri.getMappedPersonILoqPrefix() + iLoqPersonId);
        String efectePersonIdentifierValue = null;

        if (efectePersonIdentifierJson == null) {
            String firstName = getNormalizedString(iLoqPerson.getFirstName());
            String lastName = getNormalizedString(iLoqPerson.getLastName());

            List<EfecteEntity> efectePersons = getEfectePerson(firstName, lastName);

            if (efectePersons.isEmpty()) {
                return firstName + " " + lastName;
            } else if (efectePersons.size() > 1) {
                // When multiple Efecte persons match, store them in auditRecord to allow the user to select the correct match in the UI
                String prefix = ri.getAuditRecordPersonPrefix() + iLoqPersonId;
                ri.getRedis().set(prefix, efectePersons.toString());

                String auditMessage = "Multiple matching key holders found for iLOQ person '" + firstName + " "
                        + lastName
                        + "' at Efecte";

                ri.getAuditExceptionProcessor().throwAuditException(
                        EnumDirection.ILOQ, EnumDirection.EFECTE, null, null, iLoqPersonId, auditMessage);
            }

            EfecteEntity efecteKeyHolder = efectePersons.get(0);
            efectePersonIdentifierValue = efecteKeyHolder.getId();
            saveMappedKeys(iLoqPersonId, efecteKeyHolder);
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

    private void saveMappedKeys(String iLoqPersonId, EfecteEntity efecteKeyHolder) throws Exception {
        String entityId = efecteKeyHolder.getId();
        String efecteId = efecteKeyHolder.getAttributeValue(EnumEfecteAttribute.PERSON_EFECTE_ID);
        EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier(entityId, efecteId);
        String efecteEntityIdentifierJson = ri.getHelper().writeAsJson(efecteEntityIdentifier);

        String efectePrefix = ri.getMappedPersonEfectePrefix() + entityId;
        String iLoqPrefix = ri.getMappedPersonILoqPrefix() + iLoqPersonId;

        ri.getRedis().set(efectePrefix, iLoqPersonId);
        ri.getRedis().set(iLoqPrefix, efecteEntityIdentifierJson);
    }

    private String getNormalizedString(String str) {
        return str.trim().replaceAll("\\s+", " ");
    }
}
