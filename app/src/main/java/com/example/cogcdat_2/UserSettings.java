package com.example.cogcdat_2;

public class UserSettings {
    private String id;
    private String userId;
    private DistanceUnit distanceUnit;
    private Theme theme;
    private Language language;
    private String createdAt;
    private String updatedAt;

    public UserSettings() {
        this.distanceUnit = DistanceUnit.KM;
        this.theme = Theme.SYSTEM;
        this.language = Language.RU;
    }

    public UserSettings(String id, String userId, DistanceUnit distanceUnit,
                        Theme theme, Language language, String createdAt, String updatedAt) {
        this.id = id;
        this.userId = userId;
        this.distanceUnit = distanceUnit;
        this.theme = theme;
        this.language = language;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public DistanceUnit getDistanceUnit() { return distanceUnit; }
    public void setDistanceUnit(DistanceUnit distanceUnit) { this.distanceUnit = distanceUnit; }
    public void setDistanceUnit(String value) {
        this.distanceUnit = DistanceUnit.fromValue(value);
    }

    public Theme getTheme() { return theme; }
    public void setTheme(Theme theme) { this.theme = theme; }
    public void setTheme(String value) {
        this.theme = Theme.fromValue(value);
    }

    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }
    public void setLanguage(String value) {
        this.language = Language.fromValue(value);
    }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}