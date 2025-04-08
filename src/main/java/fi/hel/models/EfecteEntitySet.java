package fi.hel.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName(value = "entityset")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EfecteEntitySet {
    @JacksonXmlProperty(localName = "entity")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<EfecteEntity> entities;

    public EfecteEntitySet() {
    }

    public EfecteEntitySet(EfecteEntity efecteEntity) {
        this.entities = new ArrayList<>();
        this.entities.add(efecteEntity);
    }

    public EfecteEntitySet(List<EfecteEntity> entities) {
        this.entities = entities;
    }

    public List<EfecteEntity> getEntities() {
        return this.entities;
    }

    public void setEntities(List<EfecteEntity> entities) {
        this.entities = entities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EfecteEntitySet efecteEntitySet = (EfecteEntitySet) o;

        return Objects.equals(entities, efecteEntitySet.getEntities());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
