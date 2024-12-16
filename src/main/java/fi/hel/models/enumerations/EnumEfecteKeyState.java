package fi.hel.models.enumerations;

public enum EnumEfecteKeyState {
    ODOTTAA_AKTIVOINTIA("Odottaa aktivointia"),
    AKTIIVINEN("Aktiivinen"),
    HYLATTY("Hylätty"),
    PASSIIVINEN("Passiivinen"),
    POISTETTU("Poistettu");

    private final String name;

    EnumEfecteKeyState(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}