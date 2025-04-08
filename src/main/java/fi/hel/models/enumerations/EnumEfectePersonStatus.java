package fi.hel.models.enumerations;

public enum EnumEfectePersonStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive");

    private String status;

    EnumEfectePersonStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
