package fi.hel.models;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "entityset")
public class EfecteEntitySetImport {
    private EfecteEntityImport entity;

    public EfecteEntitySetImport(EfecteEntityImport entity) {
        this.entity = entity;
    }

    public EfecteEntitySetImport() {
    }

    public EfecteEntityImport getEntity() {
        return this.entity;
    }

    public void setEntity(EfecteEntityImport entity) {
        this.entity = entity;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
