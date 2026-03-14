package com.example.cogcdat_2;

public class Car {
    private String id;
    private String name;
    private String description;
    private String imagePath;
    private String distanceUnit;
    private String fuelUnit;
    private String fuelConsumptionUnit;
    private String fuelType;
    private double tankVolume;
    private String createdAt;
    private String updatedAt;
    // Удалены: brand, model, year, licensePlate, vin, insurancePolicy

    public Car() {}

    // Упрощенный конструктор с ID
    public Car(String id, String name, String description, String imagePath, String distanceUnit, String fuelUnit,
               String fuelConsumptionUnit, String fuelType, double tankVolume) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imagePath = imagePath;
        this.distanceUnit = distanceUnit;
        this.fuelUnit = fuelUnit;
        this.fuelConsumptionUnit = fuelConsumptionUnit;
        this.fuelType = fuelType;
        this.tankVolume = tankVolume;
    }

    // Упрощенный конструктор без ID для добавления
    public Car(String name, String description, String imagePath, String distanceUnit, String fuelUnit,
               String fuelConsumptionUnit, String fuelType, double tankVolume) {
        this(null, name, description, imagePath, distanceUnit, fuelUnit, fuelConsumptionUnit, fuelType, tankVolume);
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

    @Override
    public String toString() {
        return name;
    }
}