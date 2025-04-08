package fi.hel.models;

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
public class ILoqKey {

    @JsonProperty("Description")
    private String description;
    @JsonProperty("Person_ID")
    private String personId;
    @JsonProperty("RealEstate_ID")
    private String realEstateId;
    @JsonProperty("FNKey_ID")
    private String fnKeyId;
    @JsonProperty("InfoText")
    private String infoText;
    @JsonProperty("ExpireDate")
    private String expireDate;

    // Default fields (during creation):
    @JsonProperty("KeyTypeMask")
    private Integer keyTypeMask;
    @JsonProperty("ManufacturingInfo")
    private String manufacturingInfo;
    @JsonProperty("OnlineAccessCode")
    private String onlineAccessCode;
    @JsonProperty("ROM_ID")
    private String romId;
    @JsonProperty("Stamp")
    private String stamp;
    @JsonProperty("TagKey")
    private String tagKey;

    public ILoqKey() {
    }

    public ILoqKey(String fnKeyId) {
        this.fnKeyId = fnKeyId;
    }

    public ILoqKey(String personId, String realEstateId) {
        this.personId = personId;
        this.realEstateId = realEstateId;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getKeyTypeMask() {
        return this.keyTypeMask;
    }

    public void setKeyTypeMask(Integer keyTypeMask) {
        this.keyTypeMask = keyTypeMask;
    }

    public String getInfoText() {
        return this.infoText;
    }

    public void setInfoText(String infoText) {
        this.infoText = infoText;
    }

    public String getExpireDate() {
        return this.expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    public String getManufacturingInfo() {
        return this.manufacturingInfo;
    }

    public void setManufacturingInfo(String manufacturingInfo) {
        this.manufacturingInfo = manufacturingInfo;
    }

    public String getOnlineAccessCode() {
        return this.onlineAccessCode;
    }

    public void setOnlineAccessCode(String onlineAccessCode) {
        this.onlineAccessCode = onlineAccessCode;
    }

    public String getRomId() {
        return this.romId;
    }

    public void setRomId(String romId) {
        this.romId = romId;
    }

    public String getStamp() {
        return this.stamp;
    }

    public void setStamp(String stamp) {
        this.stamp = stamp;
    }

    public String getTagKey() {
        return this.tagKey;
    }

    public void setTagKey(String tagKey) {
        this.tagKey = tagKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ILoqKey ILoqKey = (ILoqKey) o;

        return Objects.equals(description, ILoqKey.getDescription()) &&
                Objects.equals(personId, ILoqKey.getPersonId()) &&
                Objects.equals(realEstateId, ILoqKey.getRealEstateId()) &&
                Objects.equals(fnKeyId, ILoqKey.getFnKeyId()) &&
                Objects.equals(infoText, ILoqKey.getInfoText()) &&
                Objects.equals(expireDate, ILoqKey.getExpireDate());
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
