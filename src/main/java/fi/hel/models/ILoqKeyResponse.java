package fi.hel.models;

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
public class ILoqKeyResponse {

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
    @JsonProperty("IsProgrammed")
    private boolean isProgrammed;
    @JsonProperty("KeyTypeMask")
    private Integer keyTypeMask;
    @JsonProperty("ProgrammingState")
    private Integer programmingState;
    @JsonProperty("ROM_ID")
    private String romId;
    @JsonProperty("Stamp")
    private String stamp;
    @JsonProperty("StampSource")
    private Integer stampSource;
    @JsonProperty("State")
    private Integer state;
    @JsonProperty("TagKey")
    private String tagKey;
    @JsonProperty("TagKeyHex")
    private String tagKeyHex;
    @JsonProperty("TagKeySource")
    private Integer tagKeySource;
    @JsonProperty("VersionCode")
    private String versionCode;

    public ILoqKeyResponse() {
    }

    public ILoqKeyResponse(String fnKeyId) {
        this.fnKeyId = fnKeyId;
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

    public boolean isIsProgrammed() {
        return this.isProgrammed;
    }

    public boolean getIsProgrammed() {
        return this.isProgrammed;
    }

    public void setIsProgrammed(boolean isProgrammed) {
        this.isProgrammed = isProgrammed;
    }

    public Integer getKeyTypeMask() {
        return this.keyTypeMask;
    }

    public void setKeyTypeMask(Integer keyTypeMask) {
        this.keyTypeMask = keyTypeMask;
    }

    public Integer getProgrammingState() {
        return this.programmingState;
    }

    public void setProgrammingState(Integer programmingState) {
        this.programmingState = programmingState;
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

    public Integer getStampSource() {
        return this.stampSource;
    }

    public void setStampSource(Integer stampSource) {
        this.stampSource = stampSource;
    }

    public Integer getState() {
        return this.state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getTagKey() {
        return this.tagKey;
    }

    public void setTagKey(String tagKey) {
        this.tagKey = tagKey;
    }

    public String getTagKeyHex() {
        return this.tagKeyHex;
    }

    public void setTagKeyHex(String tagKeyHex) {
        this.tagKeyHex = tagKeyHex;
    }

    public Integer getTagKeySource() {
        return this.tagKeySource;
    }

    public void setTagKeySource(Integer tagKeySource) {
        this.tagKeySource = tagKeySource;
    }

    public String getVersionCode() {
        return this.versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        return objectMapper.writeValueAsString(this);
    }
}
