package com.example.cogcdat_2.api.models;

import android.util.Log;

import com.example.cogcdat_2.Car;
import com.google.gson.annotations.SerializedName;

public class ApiCar {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("image_path")
    private String imagePath; // URL на сервере! (в терминах приложения - serverImageUrl)

    @SerializedName("distance_unit")
    private String distanceUnit;

    @SerializedName("fuel_unit")
    private String fuelUnit;

    @SerializedName("fuel_consumption_unit")
    private String fuelConsumptionUnit;

    @SerializedName("fuel_type")
    private String fuelType;

    @SerializedName("tank_volume")
    private double tankVolume;

    @SerializedName("owner_id")
    private String ownerId;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("is_deleted")
    private boolean isDeleted;

    @SerializedName("deleted_at")
    private String deletedAt;

    @SerializedName("image_version")
    private int imageVersion;

    // Конструктор для преобразования из локальной Car в API модель
    public ApiCar(Car car) {
        this.id = car.getId();
        this.name = car.getName();
        this.description = car.getDescription();
        // ВАЖНО: отправляем SERVER_IMAGE_URL (то, что хранится на сервере)
        this.imagePath = car.getServerImageUrl();
        this.distanceUnit = car.getDistanceUnit();
        this.fuelUnit = car.getFuelUnit();
        this.fuelConsumptionUnit = car.getFuelConsumptionUnit();
        this.fuelType = car.getFuelType();
        this.tankVolume = car.getTankVolume();
        this.createdAt = car.getCreatedAt();
        this.updatedAt = car.getUpdatedAt();
        this.isDeleted = car.isDeleted();
        this.deletedAt = car.getDeletedAt();
        this.imageVersion = car.getImageVersion();
        // owner_id будет установлен сервером
    }

    // Пустой конструктор для Gson
    public ApiCar() {}

    // Метод для преобразования из API модели в локальную Car
    public Car toLocalCar() {
        Car car = new Car(
                this.id,
                this.name,
                this.description,
                null, // imagePath - локальный путь, пока неизвестен
                this.imagePath, // serverImageUrl - это URL с сервера!
                this.imageVersion, // imageVersion - версия с сервера
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

        // Дополнительная проверка для отладки
        Log.d("ApiCar", "toLocalCar: id=" + this.id +
                ", serverImageUrl=" + this.imagePath +
                ", imageVersion=" + this.imageVersion);

        return car;
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

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public String getDeletedAt() { return deletedAt; }
    public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }

    public int getImageVersion() { return imageVersion; }
    public void setImageVersion(int imageVersion) { this.imageVersion = imageVersion; }
}