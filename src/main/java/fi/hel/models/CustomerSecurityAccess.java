package fi.hel.models;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomerSecurityAccess {
    private CustomerEfecteInfo efecte;
    @JsonProperty("iLoq")
    private CustomerILoqInfo iLoq;

    public CustomerEfecteInfo getEfecte() {
        return this.efecte;
    }

    public void setEfecte(CustomerEfecteInfo efecte) {
        this.efecte = efecte;
    }

    public CustomerILoqInfo getILoq() {
        return this.iLoq;
    }

    public void setILoq(CustomerILoqInfo iLoq) {
        this.iLoq = iLoq;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
