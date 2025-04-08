package fi.hel.models;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class CustomerZone extends CustomerILoqBase {
    private List<CustomerSecurityAccess> securityAccesses;

    public List<CustomerSecurityAccess> getSecurityAccesses() {
        return this.securityAccesses;
    }

    public void setSecurityAccesses(List<CustomerSecurityAccess> securityAccesses) {
        this.securityAccesses = securityAccesses;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
