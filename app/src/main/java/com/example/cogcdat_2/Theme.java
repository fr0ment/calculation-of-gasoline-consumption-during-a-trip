package com.example.cogcdat_2;

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

    public String getDisplayName() {
        switch (this) {
            case SYSTEM:
                return "Системная";
            case LIGHT:
                return "Светлая";
            case DARK:
                return "Темная";
            default:
                return "Системная";
        }
    }
}