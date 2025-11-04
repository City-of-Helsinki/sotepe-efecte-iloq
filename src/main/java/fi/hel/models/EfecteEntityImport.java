package fi.hel.models;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import fi.hel.models.enumerations.EnumEfecteAttribute;
import fi.hel.models.enumerations.EnumEfecteTemplate;

public class EfecteEntityImport {
    @JacksonXmlProperty(localName = "template")
    private EfecteTemplate template;
    @JacksonXmlProperty(localName = "attribute")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<EfecteAttributeImport> attributes;

    public EfecteEntityImport() {
    }

    public EfecteEntityImport(EnumEfecteTemplate template, List<EfecteAttributeImport> attributes) {
        EfecteTemplate efecteTemplate = new EfecteTemplate(template.getCode());
        this.template = efecteTemplate;
        this.attributes = attributes;
    }

    public List<EfecteAttributeImport> getAttributes() {
        return this.attributes;
    }

    public void setAttributes(List<EfecteAttributeImport> attributes) {
        this.attributes = attributes;
    }

    public EfecteTemplate getTemplate() {
        return this.template;
    }

    public void setTemplate(EfecteTemplate template) {
        this.template = template;
    }

    public EfecteAttributeImport getAttributeByType(EnumEfecteAttribute efecteEntityAttribute) {
        return attributes.stream()
                .filter(attribute -> attribute.getCode().equals(efecteEntityAttribute.getCode()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        return objectMapper.writeValueAsString(this);
    }
}
