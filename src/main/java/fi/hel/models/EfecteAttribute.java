package fi.hel.models;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import jakarta.xml.bind.annotation.XmlElement;

@JacksonXmlRootElement(namespace = "attribute")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EfecteAttribute {
    @JacksonXmlProperty(isAttribute = true)
    private String id;
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private String code;
    @JacksonXmlProperty(localName = "reference")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<EfecteReference> references;
    @XmlElement
    private String value;

    public String getId() {
        return this.id;
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

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<EfecteReference> getReferences() {
        return this.references;
    }

    public void setReferences(List<EfecteReference> reference) {
        this.references = reference;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EfecteAttribute efecteAttribute = (EfecteAttribute) o;

        return Objects.equals(id, efecteAttribute.getId()) &&
                Objects.equals(name, efecteAttribute.getName()) &&
                Objects.equals(code, efecteAttribute.getCode()) &&
                Objects.equals(references, efecteAttribute.getReferences()) &&
                Objects.equals(value, efecteAttribute.getValue());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
