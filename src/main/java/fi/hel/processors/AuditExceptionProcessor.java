package fi.hel.processors;

import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import fi.hel.exceptions.AuditException;
import fi.hel.models.AuditExceptionRecord;
import fi.hel.models.EnrichedILoqKey;
import fi.hel.models.ILoqPerson;
import fi.hel.models.enumerations.EnumDirection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named("auditExceptionProcessor")
public class AuditExceptionProcessor {

    @Inject
    ResourceInjector ri;
    @ConfigProperty(name = "AUDIT_EXCEPTION_EXPIRATION_SECONDS")
    long auditExceptionExpirationTime;

    private AuditExceptionRecord auditExceptionRecord;

    public void setAuditRecord(Exchange ex) throws Exception {
        EnrichedILoqKey enrichedILoqKey = ex.getProperty("enrichedILoqKey", EnrichedILoqKey.class);

        if (enrichedILoqKey == null) {
            System.out.println(
                    "Warning: Setting AuditRecord failed due to missing EnrichedILoqKey. Currently AuditRecords are only supported for the iLOQ -> Efecte controller");
            return;
        }

        String realEstateId = enrichedILoqKey.getRealEstateId();
        String iLoqKeyId = enrichedILoqKey.getFnKeyId();

        enrichWithILoqPerson(enrichedILoqKey);
        this.auditExceptionRecord.setILoqKey(enrichedILoqKey);
        // The UI needs the customer code to write person mappings with the correct Redis key
        this.auditExceptionRecord.setCustomerCode(ri.getRedis().get(ri.getILoqCurrentCustomerCodePrefix()));
        String prefix = ri.getAuditRecordKeyPrefix() + realEstateId + ":" + iLoqKeyId;
        ri.getRedis().setex(prefix, this.auditExceptionRecord.toJson(), auditExceptionExpirationTime);

        this.auditExceptionRecord = null;
    }

    // The person is read from iLOQ only when the person mapping is missing, so for mapped persons (e.g. outsiders)
    // only the id is known. The UI needs the person details, so they are read here for the failed keys only.
    private void enrichWithILoqPerson(EnrichedILoqKey enrichedILoqKey) {
        ILoqPerson person = enrichedILoqKey.getPerson();

        if (person == null || person.getPersonId() == null || person.getFirstName() != null) {
            return;
        }

        try {
            Exchange personEx = new ExchangeBuilder(ri.getContext())
                    .withProperty("iLoqPersonId", person.getPersonId())
                    .build();

            ILoqPerson iLoqPerson = ri.getTemplate().send(ri.getGetILoqPersonEndpointUri(), personEx)
                    .getIn().getBody(ILoqPerson.class);

            if (personEx.getProperty(Exchange.EXCEPTION_CAUGHT) == null && personEx.getException() == null
                    && iLoqPerson != null) {
                enrichedILoqKey.setPerson(iLoqPerson);
            }
        } catch (Exception e) {
            // The audit record is saved without the person details rather than not at all
            System.out.println("Warning: Reading the iLOQ person '" + person.getPersonId()
                    + "' for the audit record failed: " + e.getMessage());
        }
    }

    public void throwAuditException(
            EnumDirection from,
            EnumDirection to,
            String entityId,
            String efecteId,
            String iLoqId,
            String message) throws Exception {
        this.auditExceptionRecord = new AuditExceptionRecord(from, to, entityId, efecteId, iLoqId, message);

        throw new AuditException(this.auditExceptionRecord.toJson());
    }

}
