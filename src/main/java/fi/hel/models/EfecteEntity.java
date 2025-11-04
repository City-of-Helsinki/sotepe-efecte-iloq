package fi.hel.models;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import fi.hel.models.enumerations.EnumEfecteAttribute;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EfecteEntity {
    @JacksonXmlProperty(isAttribute = true)
    private String id;
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(localName = "template")
    private EfecteTemplate template;
    @JacksonXmlProperty(localName = "attribute")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<EfecteAttribute> attributes;

    public EfecteEntity() {
    }

    public EfecteEntity(String id) {
        this.id = id;
    }

    public List<EfecteAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<EfecteAttribute> attributes) {
        this.attributes = attributes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EfecteTemplate getTemplate() {
        return this.template;
    }

    public void setTemplate(EfecteTemplate template) {
        this.template = template;
    }

    public String getAttributeValue(EnumEfecteAttribute efecteEntityAttribute) throws Exception {
        EfecteAttribute efecteAttribute = getAttributeById(efecteEntityAttribute);

        return efecteAttribute.getValue();
    }

    public List<EfecteReference> getAttributeReferences(EnumEfecteAttribute efecteEntityAttribute) throws Exception {
        EfecteAttribute efecteAttribute = getAttributeById(efecteEntityAttribute);

        if (efecteAttribute == null) {
            return null;
        }

        return efecteAttribute.getReferences();
    }

    private EfecteAttribute getAttributeById(EnumEfecteAttribute enumEfecteAttribute) throws Exception {
        EfecteAttribute efecteAttribute = attributes.stream()
                .filter(attribute -> attribute.getId().equals(enumEfecteAttribute.getId()))
                .findFirst()
                .orElse(null);

        if (efecteAttribute == null) {
            if (enumEfecteAttribute.getId()
                    .equals(EnumEfecteAttribute.KEY_SECURITY_ACCESS.getId())) {
                return null;
            } else {
                throw new Exception("EfecteEntity.getAttributeById: No attribute found for '"
                        + enumEfecteAttribute.getId() + "' (" + enumEfecteAttribute.getCode() + "). Key id: " + id);
            }
        }

        return efecteAttribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EfecteEntity efecteEntity = (EfecteEntity) o;

        return Objects.equals(id, efecteEntity.getId()) &&
                Objects.equals(name, efecteEntity.getName()) &&
                Objects.equals(template, efecteEntity.getTemplate()) &&
                Objects.equals(attributes, efecteEntity.getAttributes());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.writeValueAsString(this);
    }

    public String toJsonPretty() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        return objectMapper.writeValueAsString(this);
    }
}
