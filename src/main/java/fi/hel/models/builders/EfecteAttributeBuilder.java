package fi.hel.models.builders;

import java.util.ArrayList;
import java.util.List;

import fi.hel.models.EfecteAttribute;
import fi.hel.models.EfecteReference;

public class EfecteAttributeBuilder {
    private String id;
    private String name;
    private String code;
    private List<EfecteReference> references;
    private String value;

    public EfecteAttributeBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public EfecteAttributeBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public EfecteAttributeBuilder withCode(String code) {
        this.code = code;
        return this;
    }

    public EfecteAttributeBuilder withValue(String value) {
        this.value = value;
        return this;
    }

    public EfecteAttributeBuilder withReference(EfecteReference reference) {
        List<EfecteReference> references = new ArrayList<>();
        references.add(reference);
        this.references = references;

        return this;
    }

    public EfecteAttributeBuilder withReference(String id) {
        List<EfecteReference> references = new ArrayList<>();
        references.add(new EfecteReference(id));
        this.references = references;

        return this;
    }

    public EfecteAttributeBuilder withReference(String id, String name) {
        List<EfecteReference> references = new ArrayList<>();
        references.add(new EfecteReference(id, name));
        this.references = references;

        return this;
    }

    public EfecteAttributeBuilder withReferences(List<EfecteReference> references) {
        this.references = references;

        return this;
    }

    public EfecteAttributeBuilder withDefaults() {
        if (id == null) {
            withId("2937");
        }

        if (name == null) {
            withName("EfecteID");
        }

        if (code == null) {
            withCode("efecteid");
        }

        if (value == null) {
            withValue("KEY-001120");
        }

        return this;
    }

    public EfecteAttribute build() {
        EfecteAttribute efecteAttribute = new EfecteAttribute();

        efecteAttribute.setId(id);
        efecteAttribute.setName(name);
        efecteAttribute.setCode(code);

        if (value != null) {
            efecteAttribute.setValue(value);
        }

        if (references != null) {
            efecteAttribute.setReferences(references);
        }

        return efecteAttribute;
    }
}
