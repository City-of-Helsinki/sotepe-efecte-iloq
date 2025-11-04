package fi.hel.models.enumerations;

import org.eclipse.microprofile.config.ConfigProvider;

public enum EnumEfecteAttribute {
    // AVAIN
    KEY_EFECTE_ID("2937", "2937", "efecteid"),
    KEY_TYPE("3952", "3952", "avain_tyyppi"),
    KEY_HOLDER("2930", "2930", "avain_haltija"),
    KEY_STREET_ADDRESS("2928", "2928", "avain_katuosoite"),
    KEY_SECURITY_ACCESS("2929", "2929", "avain_kulkualue"),
    KEY_STATE("2933", "2933", "avain_tila"),
    KEY_UPDATED("2940", "2940", "updated"),
    KEY_IS_OUTSIDER("3827", "3827", "avain_ulkopuolinen"),
    KEY_OUTSIDER_NAME("3850", "3850", "avain_ulkopuolinen_nimi"),
    KEY_OUTSIDER_EMAIL("3851", "3851", "avain_ulkopuolinen_sposti"),
    KEY_VALIDITY_DATE("2935", "2935", "avain_voimassa"),
    KEY_EXTERNAL_ID("4557", "4534", "avain_external_id"),

    // KULKUALUE
    SECURITY_ACCESS_NAME("3014", "3014", "kulkualue_nimi"),
    SECURITY_ACCESS_EFECTE_ID("3020", "3020", "efecte_id"),
    SECURITY_ACCESS_KEY_TYPE("4076", "4076", "kulkualue_avain_tyyppi"),
    SECURITY_ACCESS_STREET_ADDRESS("3016", "3016", "kulkualue_katuosoite"),

    // AVAIMEN HALTIJA
    PERSON_ENTITY_ID("3537", "3537", "person_entityid"),
    PERSON_EFECTE_ID("1350", "1350", "efecte_id"),
    PERSON_FIRSTNAME("1336", "1336", "first_name"),
    PERSON_LASTNAME("1337", "1337", "last_name"),
    PERSON_FULL_NAME("1338", "1338", "full_name"),
    PERSON_MOBILE("1340", "1340", "mobile"),
    PERSON_PHONE("1343", "1343", "phone"),
    PERSON_EMAIL("1341", "1341", "email"),
    PERSON_OFFICE("1770", "1770", "office"),
    PERSON_DEPARTMENT("1769", "1769", "department"),
    PERSON_STATUS("1782", "1782", "status"),
    PERSON_TITLE("1339", "1339", "title");

    private String idProd;
    private String idDev;
    private String code;

    EnumEfecteAttribute(String idProd, String idDev, String code) {
        this.idProd = idProd;
        this.idDev = idDev;
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    public String getId() {
        if (ConfigProvider.getConfig().getConfigValue("QUARKUS_PROFILE").getValue().equals("production")) {
            return idProd;
        } else {
            return idDev;
        }
    }
}
