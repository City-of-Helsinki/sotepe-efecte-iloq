package fi.hel.models;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import fi.hel.models.enumerations.EnumEfecteAttribute;

public class EfecteAttributeImport {
    @JacksonXmlProperty(isAttribute = true)
    private String code;
    @JacksonXmlProperty(localName = "value")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<String> values;

    public EfecteAttributeImport() {
    }

    public EfecteAttributeImport(EnumEfecteAttribute efecteAttribute, List<String> values) {
        this.code = efecteAttribute.getCode();
        this.values = values;
    }

    public EfecteAttributeImport(EnumEfecteAttribute efecteAttribute, String value) {
        this.code = efecteAttribute.getCode();
        List<String> values = new ArrayList<>();
        values.add(value);
        this.values = values;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<String> getValues() {
        return this.values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
