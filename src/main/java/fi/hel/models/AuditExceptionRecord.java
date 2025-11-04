package fi.hel.models;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.hel.models.enumerations.EnumDirection;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditExceptionRecord {
    private String id;
    private String timestamp;
    private EnumDirection from;
    private EnumDirection to;
    private String entityId;
    private String efecteId;
    @JsonProperty("iLoqId")
    private String iLoqId;
    private String message;
    private EnrichedILoqKey iLoqKey;

    public AuditExceptionRecord() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = createDatetimeNow();
    }

    public AuditExceptionRecord(
            EnumDirection from,
            EnumDirection to,
            String entityId,
            String efecteId,
            String iLoqId,
            String message) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = createDatetimeNow();
        this.from = from;
        this.to = to;
        this.entityId = entityId;
        this.efecteId = efecteId;
        this.iLoqId = iLoqId;
        this.message = message;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public EnumDirection getFrom() {
        return this.from;
    }

    public void setFrom(EnumDirection from) {
        this.from = from;
    }

    public EnumDirection getTo() {
        return this.to;
    }

    public void setTo(EnumDirection to) {
        this.to = to;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getEfecteId() {
        return this.efecteId;
    }

    public void setEfecteId(String efecteId) {
        this.efecteId = efecteId;
    }

    @JsonProperty("iLoqId")
    public String getILoqId() {
        return this.iLoqId;
    }

    @JsonProperty("iLoqId")
    public void setILoqId(String iLoqId) {
        this.iLoqId = iLoqId;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    private String createDatetimeNow() {
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"));
        String pattern = "yyyy-MM-dd'T'HH:mm:ss";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    public EnrichedILoqKey getILoqKey() {
        return this.iLoqKey;
    }

    public void setILoqKey(EnrichedILoqKey iLoqKey) {
        this.iLoqKey = iLoqKey;
    }

}
