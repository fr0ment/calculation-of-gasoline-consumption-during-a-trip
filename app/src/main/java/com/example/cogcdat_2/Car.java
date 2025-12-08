package com.example.cogcdat_2;

public class Car {
    private int id;
    private String name;
    private String description;
    private String imagePath;
    private String distanceUnit;
    private String fuelUnit;
    private String fuelConsumptionUnit;
    private String fuelType;
    private double tankVolume;
    // Удалены: brand, model, year, licensePlate, vin, insurancePolicy

    public Car() {}

    // Упрощенный конструктор с ID
    public Car(int id, String name, String description, String imagePath, String distanceUnit, String fuelUnit,
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
        this(-1, name, description, imagePath, distanceUnit, fuelUnit, fuelConsumptionUnit, fuelType, tankVolume);
    }

    // Геттеры и Сеттеры

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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

    @Override
    public String toString() {
        return name;
    }
}