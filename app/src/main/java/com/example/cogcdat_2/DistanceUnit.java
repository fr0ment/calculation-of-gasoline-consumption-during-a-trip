package com.example.cogcdat_2;

import android.content.Context;

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
            if (unit.value.equalsIgnoreCase(value)) {
                return unit;
            }
        }
        return KM; // По умолчанию
    }

    public String getDisplayName(Context context) {
        switch (this) {
            case KM: return context.getString(R.string.unit_km);
            case MI: return context.getString(R.string.unit_mile);
            default: return context.getString(R.string.unit_km);
        }
    }
}