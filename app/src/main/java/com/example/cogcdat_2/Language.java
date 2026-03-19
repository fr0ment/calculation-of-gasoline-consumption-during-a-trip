package com.example.cogcdat_2;

public enum Language {
    RU("ru"),
    EN("en");

    private final String value;

    Language(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Language fromValue(String value) {
        for (Language lang : Language.values()) {
            if (lang.value.equals(value)) {
                return lang;
            }
        }
        return RU;
    }

    public String getDisplayName() {
        switch (this) {
            case RU:
                return "Русский";
            case EN:
                return "English";
            default:
                return "Русский";
        }
    }
}