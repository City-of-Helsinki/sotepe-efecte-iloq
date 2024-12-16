package fi.hel.processors;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;

import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteEntityIdentifier;
import fi.hel.models.ILoqPersonImport;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ILoqPersonProcessor {

    @Inject
    ResourceInjector ri;

    ////////////////////
    // Efecte -> iLOQ //
    ////////////////////

    public String createILoqPerson(EfecteEntity efecteKey, List<String> zoneIds) throws Exception {
        ILoqPersonImport iLoqPersonImport;
        String efecteKeyHolderEntityId = null;
        String efecteKeyHolderEfecteId = null;

        if (hasKeyHolder(efecteKey)) {
            String keyHolderEntityId = efecteKey.getAttributeReferences(EnumEfecteAttribute.KEY_HOLDER).get(0)
                    .getId();
            EfecteEntity efectePerson = getEfectePerson(keyHolderEntityId);
            efecteKeyHolderEntityId = efectePerson.getId();
            efecteKeyHolderEfecteId = efectePerson.getAttributeValue(EnumEfecteAttribute.PERSON_EFECTE_ID);
            iLoqPersonImport = ri.getILoqPersonMapper().mapToNewILoqPerson(efectePerson);
        } else {
            iLoqPersonImport = ri.getILoqPersonMapper().mapToNewILoqPersonForOutsider(efecteKey);
        }

        iLoqPersonImport.setZoneIds(zoneIds);
        String iLoqPersonId = forwardNewPersonToILoq(iLoqPersonImport);

        if (hasKeyHolder(efecteKey)) {
            EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier(
                    efecteKeyHolderEntityId, efecteKeyHolderEfecteId);
            ri.getRedis().set(ri.getMappedPersonEfectePrefix() + efecteKeyHolderEntityId, iLoqPersonId);
            ri.getRedis().set(ri.getMappedPersonILoqPrefix() + iLoqPersonId,
                    ri.getHelper().writeAsJson(efecteEntityIdentifier));
        } else {
            String outsiderName = efecteKey.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_NAME);
            String outsiderEmail = efecteKey.getAttributeValue(EnumEfecteAttribute.KEY_OUTSIDER_EMAIL);
            EfecteEntityIdentifier efecteEntityIdentifier = new EfecteEntityIdentifier();
            efecteEntityIdentifier.setOutsiderName(outsiderName);
            efecteEntityIdentifier.setOutsiderEmail(outsiderEmail);

            ri.getRedis().set(
                    ri.getMappedPersonEfectePrefix() + ri.getHelper().createIdentifier(outsiderEmail, outsiderName),
                    iLoqPersonId);
            ri.getRedis().set(ri.getMappedPersonILoqPrefix() + iLoqPersonId,
                    ri.getHelper().writeAsJson(efecteEntityIdentifier));
        }

        return iLoqPersonId;
    }

    private EfecteEntity getEfectePerson(String personEntityId) throws Exception {
        String efectePersonJson = ri.getRedis().get(ri.getTempEfectePersonPrefix() + personEntityId);

        return ri.getHelper().writeAsPojo(efectePersonJson, EfecteEntity.class);
    }

    private String forwardNewPersonToILoq(ILoqPersonImport iLoqNewPerson) throws Exception {
        Exchange ex = new ExchangeBuilder(ri.getContext())
                .withProperty("newILoqPerson", iLoqNewPerson)
                .build();

        String newPersonId = ri.getTemplate().send(ri.getCreateILoqPersonEndpointUri(), ex).getIn()
                .getBody(String.class);

        if (ex.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            String exceptionMessage = "ILoqPersonProcessor: Creating an iLOQ person failed: ";

            if (ex.getProperty(Exchange.EXCEPTION_CAUGHT).getClass().isInstance(HttpOperationFailedException.class)) {
                exceptionMessage = exceptionMessage
                        + ex.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class)
                                .getResponseBody();
            } else {
                exceptionMessage = exceptionMessage + ex.getException().getMessage();
            }

            throw new Exception(exceptionMessage);
        }

        return newPersonId;

    }

    private boolean hasKeyHolder(EfecteEntity key) throws Exception {
        try {
            key.getAttributeReferences(EnumEfecteAttribute.KEY_HOLDER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
