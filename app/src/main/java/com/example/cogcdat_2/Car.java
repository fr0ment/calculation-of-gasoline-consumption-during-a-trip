package com.example.cogcdat_2;

public class Car {
    private String id;
    private String name;
    private String description;
    private String imagePath;
    private String serverImageUrl;
    private int imageVersion;
    private String fuelUnit; // л, гал, кВтч, м³
    private String fuelType;
    private double tankVolume;
    private String createdAt;
    private String updatedAt;
    private boolean isDeleted;
    private String deletedAt;
    private String ownerId;

    // Пустой конструктор
    public Car() {}

    // Полный конструктор со всеми полями
    public Car(String id, String name, String description, String imagePath,
               String serverImageUrl, int imageVersion, String fuelUnit,
               String fuelType, double tankVolume, boolean isDeleted,
               String deletedAt, String ownerId, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imagePath = imagePath;
        this.serverImageUrl = serverImageUrl;
        this.imageVersion = imageVersion;
        this.fuelUnit = fuelUnit;
        this.fuelType = fuelType;
        this.tankVolume = tankVolume;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Конструктор для локального создания
    public Car(String id, String name, String description, String imagePath,
               String fuelUnit, String fuelType, double tankVolume) {
        this(id, name, description, imagePath, null, 0, fuelUnit,
                fuelType, tankVolume, false, null, null, null, null);
    }

    // Упрощенный конструктор без ID для добавления
    public Car(String name, String description, String imagePath,
               String fuelUnit, String fuelType, double tankVolume) {
        this(null, name, description, imagePath, null, 0, fuelUnit,
                fuelType, tankVolume, false, null, null, null, null);
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getServerImageUrl() { return serverImageUrl; }
    public void setServerImageUrl(String serverImageUrl) { this.serverImageUrl = serverImageUrl; }

    public int getImageVersion() { return imageVersion; }
    public void setImageVersion(int imageVersion) { this.imageVersion = imageVersion; }

    public String getFuelUnit() { return fuelUnit; }
    public void setFuelUnit(String fuelUnit) { this.fuelUnit = fuelUnit; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public double getTankVolume() { return tankVolume; }
    public void setTankVolume(double tankVolume) { this.tankVolume = tankVolume; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public String getDeletedAt() { return deletedAt; }
    public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public void incrementImageVersion() {
        this.imageVersion++;
    }

    @Override
    public String toString() {
        return name;
    }

    public static class UserSettings {
        private String id;
        private String userId;
        private String distanceUnit; // "km" или "mi"
        private String theme;         // "system", "light", "dark"
        private String language;      // "ru", "en"
        private String createdAt;
        private String updatedAt;

        public UserSettings() {
            this.distanceUnit = "km";
            this.theme = "system";
            this.language = "ru";
        }

        // Геттеры и сеттеры
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getDistanceUnit() { return distanceUnit; }
        public void setDistanceUnit(String distanceUnit) { this.distanceUnit = distanceUnit; }

        public boolean isUsingMiles() {
            return "mi".equalsIgnoreCase(distanceUnit);
        }

        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }
}