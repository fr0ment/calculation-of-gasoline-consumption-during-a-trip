package com.example.cogcdat_2;

public enum DistanceUnit {
    KM("km"),
    MI("mi");

    private final String value;

    DistanceUnit(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DistanceUnit fromValue(String value) {
        for (DistanceUnit unit : DistanceUnit.values()) {
            if (unit.value.equals(value)) {
                return unit;
            }
        }
        return KM; // По умолчанию
    }

    public String getDisplayName() {
        switch (this) {
            case KM:
                return "км";
            case MI:
                return "мили";
            default:
                return "км";
        }
    }
}