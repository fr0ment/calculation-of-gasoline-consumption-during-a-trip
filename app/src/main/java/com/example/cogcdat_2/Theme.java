package com.example.cogcdat_2;

import android.content.Context;

public enum Theme {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    private final String value;

    Theme(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Theme fromValue(String value) {
        for (Theme theme : Theme.values()) {
            if (theme.value.equals(value)) {
                return theme;
            }
        }
        return SYSTEM;
    }

    public String getDisplayName(Context context) {
        switch (this) {
            case SYSTEM:
                return context.getString(R.string.theme_system);
            case LIGHT:
                return context.getString(R.string.theme_light);
            case DARK:
                return context.getString(R.string.theme_dark);
            default:
                return context.getString(R.string.theme_system);
        }
    }
}