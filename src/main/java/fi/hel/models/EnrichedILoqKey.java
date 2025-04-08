package fi.hel.models;

import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrichedILoqKey {

    private String personId;
    private String realEstateId;
    private String fnKeyId;
    private String infoText;
    private Integer state;
    private Set<ILoqSecurityAccess> securityAccesses;

    public EnrichedILoqKey() {
    }

    public EnrichedILoqKey(String fnKeyId) {
        this.fnKeyId = fnKeyId;
    }

    public String getPersonId() {
        return this.personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getRealEstateId() {
        return this.realEstateId;
    }

    public void setRealEstateId(String realEstateId) {
        this.realEstateId = realEstateId;
    }

    public String getFnKeyId() {
        return this.fnKeyId;
    }

    public void setFnKeyId(String fnKeyId) {
        this.fnKeyId = fnKeyId;
    }

    public String getInfoText() {
        return this.infoText;
    }

    public void setInfoText(String infoText) {
        this.infoText = infoText;
    }

    public Integer getState() {
        return this.state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public Set<ILoqSecurityAccess> getSecurityAccesses() {
        return this.securityAccesses;
    }

    public void setSecurityAccesses(Set<ILoqSecurityAccess> securityAccesses) {
        this.securityAccesses = securityAccesses;
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
