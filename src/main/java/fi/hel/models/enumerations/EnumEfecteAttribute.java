package fi.hel.models.enumerations;

public enum EnumEfecteAttribute {
    // AVAIN
    KEY_EFECTE_ID("2937", "efecteid"),
    KEY_TYPE("3952", "avain_tyyppi"),
    KEY_HOLDER("2930", "avain_haltija"),
    KEY_STREET_ADDRESS("2928", "avain_katuosoite"),
    KEY_SECURITY_ACCESS("2929", "avain_kulkualue"),
    KEY_STATE("2933", "avain_tila"),
    KEY_UPDATED("2940", "updated"),
    KEY_IS_OUTSIDER("3827", "avain_ulkopuolinen"),
    KEY_OUTSIDER_NAME("3850", "avain_ulkopuolinen_nimi"),
    KEY_OUTSIDER_EMAIL("3851", "avain_ulkopuolinen_sposti"),
    KEY_VALIDITY_DATE("2935", "avain_voimassa"),
    // TUOTANTO:
    // KEY_EXTERNAL_ID("4557", "avain_external_id"),
    // TESTI:
    KEY_EXTERNAL_ID("4534", "avain_external_id"),

    // KULKUALUE
    SECURITY_ACCESS_NAME("3014", "kulkualue_nimi"),
    SECURITY_ACCESS_EFECTE_ID("3020", "efecte_id"),
    SECURITY_ACCESS_KEY_TYPE("4076", "kulkualue_avain_tyyppi"),
    SECURITY_ACCESS_STREET_ADDRESS("3016", "kulkualue_katuosoite"),

    // AVAIMEN HALTIJA
    PERSON_EFECTE_ID("1350", "efecte_id"),
    PERSON_FIRSTNAME("1336", "first_name"),
    PERSON_LASTNAME("1337", "last_name"),
    PERSON_STATUS("1782", "status");

    private String attributeId;
    private String code;

    EnumEfecteAttribute(String attributeId, String code) {
        this.attributeId = attributeId;
        this.code = code;
    }

    public String getAttributeId() {
        return this.attributeId;
    }

    public String getCode() {
        return this.code;
    }
}
