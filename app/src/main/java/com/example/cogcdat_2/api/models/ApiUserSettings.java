package com.example.cogcdat_2.api.models;

import com.example.cogcdat_2.UserSettings;
import com.google.gson.annotations.SerializedName;

public class ApiUserSettings {
    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("distance_unit")
    private String distanceUnit;

    @SerializedName("theme")
    private String theme;

    @SerializedName("language")
    private String language;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;


    public ApiUserSettings(UserSettings settings) {
        this.id = settings.getId();
        this.userId = settings.getUserId();
        this.distanceUnit = settings.getDistanceUnit().getValue();
        this.theme = settings.getTheme().getValue();
        this.language = settings.getLanguage().getValue();
        this.createdAt = settings.getCreatedAt();
        this.updatedAt = settings.getUpdatedAt();
    }

    public UserSettings toLocalSettings() {
        UserSettings settings = new UserSettings();
        settings.setId(this.id);
        settings.setUserId(this.userId);
        settings.setDistanceUnit(this.distanceUnit);
        settings.setTheme(this.theme);
        settings.setLanguage(this.language);
        settings.setCreatedAt(this.createdAt);
        settings.setUpdatedAt(this.updatedAt);
        return settings;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDistanceUnit() { return distanceUnit; }
    public void setDistanceUnit(String distanceUnit) { this.distanceUnit = distanceUnit; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}