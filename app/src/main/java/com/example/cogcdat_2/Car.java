package com.example.cogcdat_2;

public class Car {
    private String id;
    private String name;
    private String description;
    private String imagePath; // Локальный путь к файлу на устройстве
    private String serverImageUrl; // URL изображения на сервере
    private int imageVersion; // Версия изображения
    private String distanceUnit;
    private String fuelUnit;
    private String fuelConsumptionUnit;
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
    public Car(String id, String name, String description, String imagePath, String serverImageUrl, int imageVersion,
               String distanceUnit, String fuelUnit, String fuelConsumptionUnit,
               String fuelType, double tankVolume, boolean isDeleted,
               String deletedAt, String ownerId, String createdAt, String updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imagePath = imagePath;
        this.serverImageUrl = serverImageUrl;
        this.imageVersion = imageVersion;
        this.distanceUnit = distanceUnit;
        this.fuelUnit = fuelUnit;
        this.fuelConsumptionUnit = fuelConsumptionUnit;
        this.fuelType = fuelType;
        this.tankVolume = tankVolume;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Конструктор для локального создания (без полей синхронизации)
    public Car(String id, String name, String description, String imagePath,
               String distanceUnit, String fuelUnit, String fuelConsumptionUnit,
               String fuelType, double tankVolume) {
        this(id, name, description, imagePath, null, 0, distanceUnit, fuelUnit,
                fuelConsumptionUnit, fuelType, tankVolume, false, null, null, null, null);
    }

    // Конструктор с isDeleted и deletedAt (для восстановления из БД)
    public Car(String id, String name, String description, String imagePath,
               String distanceUnit, String fuelUnit, String fuelConsumptionUnit,
               String fuelType, double tankVolume, boolean isDeleted, String deletedAt) {
        this(id, name, description, imagePath, null, 0, distanceUnit, fuelUnit,
                fuelConsumptionUnit, fuelType, tankVolume, isDeleted, deletedAt, null, null, null);
    }

    // Упрощенный конструктор без ID для добавления
    public Car(String name, String description, String imagePath, String distanceUnit,
               String fuelUnit, String fuelConsumptionUnit, String fuelType, double tankVolume) {
        this(null, name, description, imagePath, null, 0, distanceUnit, fuelUnit,
                fuelConsumptionUnit, fuelType, tankVolume, false, null, null, null, null);
    }

    // Геттеры и Сеттеры

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

    public String getDistanceUnit() { return distanceUnit; }
    public void setDistanceUnit(String distanceUnit) { this.distanceUnit = distanceUnit; }

    public String getFuelUnit() { return fuelUnit; }
    public void setFuelUnit(String fuelUnit) { this.fuelUnit = fuelUnit; }

    public String getFuelConsumptionUnit() { return fuelConsumptionUnit; }
    public void setFuelConsumptionUnit(String fuelConsumptionUnit) { this.fuelConsumptionUnit = fuelConsumptionUnit; }

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

    @Override
    public String toString() {
        return name;
    }

    // Метод для проверки, нужно ли синхронизировать
    public boolean needsSync(String lastSyncTime) {
        if (lastSyncTime == null) return true;
        if (updatedAt == null) return true;
        return updatedAt.compareTo(lastSyncTime) > 0;
    }

    // Метод для увеличения версии изображения
    public void incrementImageVersion() {
        this.imageVersion++;
    }

    // Метод для создания копии
    public Car copy() {
        return new Car(
                this.id,
                this.name,
                this.description,
                this.imagePath,
                this.serverImageUrl,
                this.imageVersion,
                this.distanceUnit,
                this.fuelUnit,
                this.fuelConsumptionUnit,
                this.fuelType,
                this.tankVolume,
                this.isDeleted,
                this.deletedAt,
                this.ownerId,
                this.createdAt,
                this.updatedAt
        );
    }
}