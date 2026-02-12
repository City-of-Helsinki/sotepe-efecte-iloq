package fi.hel.processors;

import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import fi.hel.exceptions.AuditException;
import fi.hel.models.AuditExceptionRecord;
import fi.hel.models.EnrichedILoqKey;
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

        this.auditExceptionRecord.setILoqKey(enrichedILoqKey);
        String prefix = ri.getAuditRecordKeyPrefix() + realEstateId + ":" + iLoqKeyId;
        ri.getRedis().set(prefix, this.auditExceptionRecord.toJson());

        this.auditExceptionRecord = null;
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
