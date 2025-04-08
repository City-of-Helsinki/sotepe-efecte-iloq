package fi.hel.processors;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import fi.hel.exceptions.AuditException;
import fi.hel.models.AuditExceptionRecord;
import fi.hel.models.enumerations.EnumDirection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuditExceptionProcessor {

    @Inject
    ResourceInjector ri;
    @ConfigProperty(name = "AUDIT_EXCEPTION_EXPIRATION_SECONDS")
    long auditExceptionExpirationTime;

    public String setAuditException(
            EnumDirection from,
            EnumDirection to,
            String entityId,
            String efecteId,
            String iLoqId,
            String message) throws Exception {
        AuditExceptionRecord auditExceptionRecord = new AuditExceptionRecord(
                from, to, entityId, efecteId, iLoqId, message);
        String auditExceptionJson = auditExceptionRecord.toJson();
        String suffix = efecteId != null ? efecteId : iLoqId;
        String prefix = ri.getAuditExceptionPrefix() + auditExceptionRecord.getTimestamp() + ":" + suffix;

        if (!ri.getRedis().exists(ri.getAuditExceptionInProgressPrefix())) {
            ri.getRedis().setex(prefix, auditExceptionJson, auditExceptionExpirationTime);
            ri.getRedis().set(ri.getAuditExceptionInProgressPrefix(), "true");
        }

        return auditExceptionJson;
    }

    public void throwAuditException(
            EnumDirection from,
            EnumDirection to,
            String entityId,
            String efecteId,
            String iLoqId,
            String message) throws Exception {
        String auditExceptionJson = setAuditException(from, to, entityId, efecteId, iLoqId, message);

        throw new AuditException(auditExceptionJson);
    }

}
