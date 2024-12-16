package fi.hel.models.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.hel.models.EfecteAttribute;
import fi.hel.models.EfecteEntity;
import fi.hel.models.EfecteReference;
import fi.hel.models.EfecteTemplate;
import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.models.enumerations.EnumEfecteKeyState;
import fi.hel.models.enumerations.EnumEfecteTemplate;

public class EfecteEntityBuilder {
    private String id;
    private String name;
    private EfecteTemplate template;
    private List<EfecteAttribute> attributes = new ArrayList<>();
    private EfecteAttribute efecteId;
    // KEY
    private EfecteAttribute externalId;
    private EfecteAttribute state;
    private EfecteAttribute keyHolder;
    private EfecteAttribute streetAddress;
    private EfecteAttribute securityAccess;
    private EfecteAttribute isOutsider;
    private EfecteAttribute outsiderName;
    private EfecteAttribute outsiderEmail;
    private EfecteAttribute validityDate;
    private EfecteAttribute keyType;
    // PERSON
    private EfecteAttribute firstName;
    private EfecteAttribute lastName;

    public EfecteEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public EfecteEntityBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public EfecteEntityBuilder withTemplate(String code) {
        this.template = new EfecteTemplate(code);
        return this;
    }

    public EfecteEntityBuilder withKeyEfecteId(String efecteId) {
        this.efecteId = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_EFECTE_ID.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_EFECTE_ID.getCode())
                .withValue(efecteId)
                .build();

        return this;
    }

    public EfecteEntityBuilder withPersonEfecteId(String efecteId) {
        this.efecteId = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.PERSON_EFECTE_ID.getAttributeId())
                .withCode(EnumEfecteAttribute.PERSON_EFECTE_ID.getCode())
                .withValue(efecteId)
                .build();

        return this;
    }

    public EfecteEntityBuilder withExternalId(String externalId) {
        this.externalId = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_EXTERNAL_ID.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_EXTERNAL_ID.getCode())
                .withValue(externalId)
                .build();
        return this;
    }

    public EfecteEntityBuilder withState(EnumEfecteKeyState enumEfecteKeyState) {
        this.state = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_STATE.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_STATE.getCode())
                .withValue(enumEfecteKeyState.getName())
                .build();
        return this;
    }

    public EfecteEntityBuilder withKeyHolderReference(String referenceId) {
        this.keyHolder = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_HOLDER.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_HOLDER.getCode())
                .withReference(referenceId)
                .build();
        return this;
    }

    public EfecteEntityBuilder withKeyHolderReference(String referenceId, String referenceName) {
        this.keyHolder = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_HOLDER.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_HOLDER.getCode())
                .withReference(referenceId, referenceName)
                .build();
        return this;
    }

    public EfecteEntityBuilder withKeyHolderReference(EfecteReference keyHolderReference) {
        this.keyHolder = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_HOLDER.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_HOLDER.getCode())
                .withReference(keyHolderReference)
                .build();
        return this;
    }

    public EfecteEntityBuilder withStreetAddress(String referenceId, String referenceName) {
        this.streetAddress = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_STREET_ADDRESS.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_STREET_ADDRESS.getCode())
                .withReference(referenceId, referenceName)
                .build();
        return this;
    }

    public EfecteEntityBuilder withSecurityAccesses(EfecteReference... securityAccessReferences) {
        this.securityAccess = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_SECURITY_ACCESS.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_SECURITY_ACCESS.getCode())
                .withReferences(Arrays.asList(securityAccessReferences))
                .build();
        return this;
    }

    public EfecteEntityBuilder withIsOutsider(boolean isOutsider) {
        this.isOutsider = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_IS_OUTSIDER.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_IS_OUTSIDER.getCode())
                .withValue(isOutsider ? "Kyllä" : "Ei")
                .build();

        return this;
    }

    public EfecteEntityBuilder withOutsiderName(String outsiderName) {
        this.outsiderName = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_OUTSIDER_NAME.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_OUTSIDER_NAME.getCode())
                .withValue(outsiderName)
                .build();

        return this;
    }

    public EfecteEntityBuilder withOutsiderEmail(String outsiderEmail) {
        this.outsiderEmail = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_OUTSIDER_EMAIL.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_OUTSIDER_EMAIL.getCode())
                .withValue(outsiderEmail)
                .build();

        return this;
    }

    public EfecteEntityBuilder withValidityDate(String validityDate) {
        this.validityDate = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_VALIDITY_DATE.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_VALIDITY_DATE.getCode())
                .withValue(validityDate)
                .build();

        return this;
    }

    public EfecteEntityBuilder withKeyType(String keyType) {
        this.keyType = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.KEY_TYPE.getAttributeId())
                .withCode(EnumEfecteAttribute.KEY_TYPE.getCode())
                .withValue(keyType)
                .build();

        return this;
    }

    public EfecteEntityBuilder withFirstName(String firstName) {
        this.firstName = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.PERSON_FIRSTNAME.getAttributeId())
                .withCode(EnumEfecteAttribute.PERSON_FIRSTNAME.getCode())
                .withValue(firstName)
                .build();
        return this;
    }

    public EfecteEntityBuilder withLastName(String lastName) {
        this.lastName = new EfecteAttributeBuilder()
                .withId(EnumEfecteAttribute.PERSON_LASTNAME.getAttributeId())
                .withCode(EnumEfecteAttribute.PERSON_LASTNAME.getCode())
                .withValue(lastName)
                .build();
        return this;
    }

    public EfecteEntityBuilder withAttributes(EfecteAttribute... newAttributes) {
        EfecteAttribute attributeToRemove = null;

        for (EfecteAttribute newAttribute : newAttributes) {
            for (EfecteAttribute existingAttribute : this.attributes) {
                if (existingAttribute.getId().equals(newAttribute.getId())) {
                    attributeToRemove = existingAttribute;
                    break;
                }
            }

            if (attributeToRemove != null) {
                this.attributes.remove(attributeToRemove);
            }

            this.attributes.add(newAttribute);
        }

        return this;
    }

    public EfecteEntityBuilder withDefaults(EnumEfecteTemplate templateType) throws Exception {
        if (id == null)
            withId("1234");

        switch (templateType) {
            case KEY:
                if (name == null)
                    withName("1 - Kulkualue - Smith John");
                if (template == null)
                    withTemplate(EnumEfecteTemplate.KEY.getCode());
                if (efecteId == null)
                    withKeyEfecteId("KEY-000123");
                if (externalId == null)
                    withExternalId("abc-123");
                if (keyHolder == null && isOutsider == null)
                    withKeyHolderReference("135", "Doe John");
                if (state == null)
                    withState(EnumEfecteKeyState.ODOTTAA_AKTIVOINTIA);
                if (streetAddress == null)
                    withStreetAddress("123", "Testikatu 1, 00510, Helsinki");
                if (securityAccess == null)
                    withSecurityAccesses(new EfecteReference("345", "Testikulkualue"));
                if (outsiderName == null && isOutsider != null)
                    withOutsiderName("John Smith");
                if (outsiderEmail == null && isOutsider != null)
                    withOutsiderEmail("john.smith@outsider.com");
                if (isOutsider == null)
                    withIsOutsider(false);
                if (validityDate == null)
                    withValidityDate("20.05.2025 00:00");
                if (keyType == null)
                    withKeyType("iLOQ");
                break;
            case PERSON:
                if (name == null)
                    withName("Smith John");
                if (template == null)
                    withTemplate(EnumEfecteTemplate.PERSON.getCode());
                if (efecteId == null)
                    withPersonEfecteId("PER-00123");
                if (firstName == null)
                    withFirstName("John");
                if (lastName == null)
                    withLastName("Smith");
                break;
            default:
                throw new Exception("Missing template type");
        }

        return this;

    }

    public EfecteEntity build() {
        EfecteEntity efecteEntity = new EfecteEntity();

        efecteEntity.setId(id);
        efecteEntity.setName(name);
        efecteEntity.setTemplate(template);
        efecteEntity.setAttributes(attributes);

        // KEY
        addAttributeIfNotNull(efecteId);
        addAttributeIfNotNull(externalId);
        addAttributeIfNotNull(state);
        addAttributeIfNotNull(keyHolder);
        addAttributeIfNotNull(streetAddress);
        addAttributeIfNotNull(securityAccess);
        addAttributeIfNotNull(isOutsider);
        addAttributeIfNotNull(outsiderName);
        addAttributeIfNotNull(outsiderEmail);
        addAttributeIfNotNull(validityDate);
        addAttributeIfNotNull(keyType);
        // PERSON
        addAttributeIfNotNull(firstName);
        addAttributeIfNotNull(lastName);

        return efecteEntity;
    }

    private void addAttributeIfNotNull(EfecteAttribute item) {
        if (item != null) {
            this.attributes.add(item);
        }
    }
}
