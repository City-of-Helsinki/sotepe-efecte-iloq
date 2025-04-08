package fi.hel.models;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ILoqRealEstate {

    @JsonProperty("Name")
    private String name;
    @JsonProperty("RealEstate_ID")
    private String realEstateId;

    public ILoqRealEstate() {
    }

    public ILoqRealEstate(String realEstateId, String name) {
        this.realEstateId = realEstateId;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRealEstateId() {
        return this.realEstateId;
    }

    public void setRealEstateId(String realEstateId) {
        this.realEstateId = realEstateId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
