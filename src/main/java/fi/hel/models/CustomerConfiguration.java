package fi.hel.models;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class CustomerConfiguration {
    private String customerCode;
    private String customerCodePassword;
    private List<CustomerRealEstate> realEstates;
    private List<CustomerZone> zones;

    public String getCustomerCode() {
        return this.customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public String getCustomerCodePassword() {
        return this.customerCodePassword;
    }

    public void setCustomerCodePassword(String customerCodePassword) {
        this.customerCodePassword = customerCodePassword;
    }

    public List<CustomerRealEstate> getRealEstates() {
        return this.realEstates;
    }

    public void setRealEstates(List<CustomerRealEstate> realEstates) {
        this.realEstates = realEstates;
    }

    public List<CustomerZone> getZones() {
        return this.zones;
    }

    public void setZones(List<CustomerZone> zones) {
        this.zones = zones;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
