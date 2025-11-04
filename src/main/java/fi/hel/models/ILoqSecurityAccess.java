package fi.hel.models;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ILoqSecurityAccess {

    @JsonProperty("Name")
    private String name;
    @JsonProperty("RealEstate_ID")
    private String realEstateId;
    @JsonProperty("SecurityAccess_ID")
    private String securityAccessId;
    @JsonProperty("Zone_ID")
    private String zoneId;

    public ILoqSecurityAccess() {
    }

    public ILoqSecurityAccess(String securityAccessId) {
        this.securityAccessId = securityAccessId;
    }

    public ILoqSecurityAccess(String name, String realEstateId, String securityAccessId) {
        this.name = name;
        this.realEstateId = realEstateId;
        this.securityAccessId = securityAccessId;
    }

    public ILoqSecurityAccess(String securityAccessId, String zoneId) {
        this.securityAccessId = securityAccessId;
        this.zoneId = zoneId;
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

    public String getSecurityAccessId() {
        return this.securityAccessId;
    }

    public void setSecurityAccessId(String securityAccessId) {
        this.securityAccessId = securityAccessId;
    }

    public String getZoneId() {
        return this.zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ILoqSecurityAccess iLoqSecurityAccess = (ILoqSecurityAccess) o;

        return Objects.equals(name, iLoqSecurityAccess.getName()) &&
                Objects.equals(realEstateId, iLoqSecurityAccess.getRealEstateId()) &&
                Objects.equals(securityAccessId, iLoqSecurityAccess.getSecurityAccessId()) &&
                Objects.equals(zoneId, iLoqSecurityAccess.getZoneId());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
