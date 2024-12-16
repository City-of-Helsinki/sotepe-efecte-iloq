package fi.hel.models;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ILoqPerson {

    @JsonProperty("Address")
    private String address;
    @JsonProperty("FirstName")
    private String firstName;
    @JsonProperty("LastName")
    private String lastName;
    @JsonProperty("Person_ID")
    private String personId;
    @JsonProperty("ExternalPersonId")
    private String externalPersonId;

    // Default fields:
    @JsonProperty("PersonCode")
    private String personCode;
    @JsonProperty("CompanyName")
    private String companyName;
    @JsonProperty("ContactInfo")
    private String contactInfo;
    @JsonProperty("Country")
    private String country;
    @JsonProperty("Description")
    private String description;
    @JsonProperty("eMail")
    private String email;
    @JsonProperty("EmploymentEndDate")
    private String employmentEndDate;
    @JsonProperty("ExternalCanEdit")
    private boolean externalCanEdit;
    @JsonProperty("LanguageCode")
    private String languageCode;
    @JsonProperty("Phone1")
    private String phone1;
    @JsonProperty("Phone2")
    private String phone2;
    @JsonProperty("Phone3")
    private String phone3;
    @JsonProperty("PostOffice")
    private String postOffice;
    @JsonProperty("State")
    private Integer state;
    @JsonProperty("WorkTitle")
    private String workTitle;
    @JsonProperty("ZipCode")
    private String zipCode;

    public ILoqPerson() {
    }

    public ILoqPerson(String personId) {
        this.personId = personId;
    }

    public ILoqPerson(String firstName, String lastName, String personId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.personId = personId;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPersonId() {
        return this.personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getExternalPersonId() {
        return this.externalPersonId;
    }

    public void setExternalPersonId(String externalPersonId) {
        this.externalPersonId = externalPersonId;
    }

    public String getPersonCode() {
        return this.personCode;
    }

    public void setPersonCode(String personCode) {
        this.personCode = personCode;
    }

    public String getCompanyName() {
        return this.companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactInfo() {
        return this.contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmploymentEndDate() {
        return this.employmentEndDate;
    }

    public void setEmploymentEndDate(String employmentEndDate) {
        this.employmentEndDate = employmentEndDate;
    }

    public boolean isExternalCanEdit() {
        return this.externalCanEdit;
    }

    public boolean getExternalCanEdit() {
        return this.externalCanEdit;
    }

    public void setExternalCanEdit(boolean externalCanEdit) {
        this.externalCanEdit = externalCanEdit;
    }

    public String getLanguageCode() {
        return this.languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getPhone1() {
        return this.phone1;
    }

    public void setPhone1(String phone1) {
        this.phone1 = phone1;
    }

    public String getPhone2() {
        return this.phone2;
    }

    public void setPhone2(String phone2) {
        this.phone2 = phone2;
    }

    public String getPhone3() {
        return this.phone3;
    }

    public void setPhone3(String phone3) {
        this.phone3 = phone3;
    }

    public String getPostOffice() {
        return this.postOffice;
    }

    public void setPostOffice(String postOffice) {
        this.postOffice = postOffice;
    }

    public Integer getState() {
        return this.state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getWorkTitle() {
        return this.workTitle;
    }

    public void setWorkTitle(String workTitle) {
        this.workTitle = workTitle;
    }

    public String getZipCode() {
        return this.zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
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
