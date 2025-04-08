package fi.hel.models;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EfecteEntityIdentifier {
    private String entityId;
    private String efecteId;
    private String outsiderName;
    private String outsiderEmail;

    public EfecteEntityIdentifier(String entityId, String efecteId) {
        this.entityId = entityId;
        this.efecteId = efecteId;
    }

    public EfecteEntityIdentifier() {
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

    public String getOutsiderName() {
        return this.outsiderName;
    }

    public void setOutsiderName(String outsiderName) {
        this.outsiderName = outsiderName;
    }

    public String getOutsiderEmail() {
        return this.outsiderEmail;
    }

    public void setOutsiderEmail(String outsiderEmail) {
        this.outsiderEmail = outsiderEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EfecteEntityIdentifier efecteEntityIdentifier = (EfecteEntityIdentifier) o;

        return Objects.equals(entityId, efecteEntityIdentifier.getEntityId()) &&
                Objects.equals(efecteId, efecteEntityIdentifier.getEfecteId()) &&
                Objects.equals(outsiderName, efecteEntityIdentifier.getOutsiderName()) &&
                Objects.equals(outsiderEmail, efecteEntityIdentifier.getOutsiderEmail());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
