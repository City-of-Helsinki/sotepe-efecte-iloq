package fi.hel.models;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ILoqKeyImport {

    @JsonProperty("Key")
    private ILoqKey key;
    @JsonProperty("SecurityAccessIds")
    private List<String> securityAccessIds;
    @JsonProperty("ZoneIds")
    private List<String> zoneIds;

    public ILoqKeyImport() {
    }

    public ILoqKeyImport(ILoqKey iLoqKey) {
        this.key = iLoqKey;
    }

    public ILoqKey getKey() {
        return this.key;
    }

    public void setKey(ILoqKey key) {
        this.key = key;
    }

    public List<String> getSecurityAccessIds() {
        return this.securityAccessIds;
    }

    public void setSecurityAccessIds(List<String> securityAccessIds) {
        this.securityAccessIds = securityAccessIds;
    }

    public List<String> getZoneIds() {
        return this.zoneIds;
    }

    public void setZoneIds(List<String> zoneIds) {
        this.zoneIds = zoneIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ILoqKeyImport iLoqKeyImport = (ILoqKeyImport) o;

        return Objects.equals(key, iLoqKeyImport.getKey()) &&
                Objects.equals(securityAccessIds, iLoqKeyImport.getSecurityAccessIds()) &&
                Objects.equals(zoneIds, iLoqKeyImport.getZoneIds());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        return objectMapper.writeValueAsString(this);
    }
}
