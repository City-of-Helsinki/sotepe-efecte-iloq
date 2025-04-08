package fi.hel.models.enumerations;

public enum EnumEfecteTemplate {
    KEY("avain"),
    PERSON("person"),
    SECURITY_ACCESS("kulkualue");

    private String code;

    EnumEfecteTemplate(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
