package com.example.cogcdat_2;

import android.content.Context;

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

    public String getDisplayName(Context context) {
        switch (this) {
            case RU:
                return context.getString(R.string.lang_russian);
            case EN:
                return context.getString(R.string.lang_english);
            default:
                return context.getString(R.string.lang_russian);
        }
    }
}