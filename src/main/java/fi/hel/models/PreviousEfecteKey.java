package fi.hel.models;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class PreviousEfecteKey {
    private String state;
    private Set<String> securityAccesses;
    private String validityDate;

    public PreviousEfecteKey() {
    }

    public PreviousEfecteKey(String state) {
        this.state = state;
    }

    public PreviousEfecteKey(Set<String> securityAccesses) {
        this.securityAccesses = securityAccesses;
    }

    public PreviousEfecteKey(String state, Set<String> securityAccesses) {
        this.state = state;
        this.securityAccesses = securityAccesses;
    }

    public PreviousEfecteKey(String state, Set<String> securityAccesses, String validityDate) {
        this.state = state;
        this.securityAccesses = securityAccesses;
        this.validityDate = validityDate;
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Set<String> getSecurityAccesses() {
        return this.securityAccesses;
    }

    public void setSecurityAccesses(Set<String> securityAccesses) {
        this.securityAccesses = securityAccesses;
    }

    public String getValidityDate() {
        return this.validityDate;
    }

    public void setValidityDate(String validityDate) {
        this.validityDate = validityDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PreviousEfecteKey previousEfecteKey = (PreviousEfecteKey) o;

        return Objects.equals(state, previousEfecteKey.getState()) &&
                Objects.equals(securityAccesses, previousEfecteKey.getSecurityAccesses()) &&
                Objects.equals(validityDate, previousEfecteKey.getValidityDate());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
