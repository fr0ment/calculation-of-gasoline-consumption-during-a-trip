package com.example.cogcdat_2;

public class Car {
    private int id;
    private String name;
    private String description;
    private String distanceUnit;
    private String fuelUnit;
    private String fuelConsumptionUnit;
    private String fuelType;
    private double tankVolume;
    private String brand;
    private String model;
    private String year;
    private String licensePlate;
    private String vin;
    private String insurancePolicy;

    public Car() {}

    public Car(String name, String description, String distanceUnit, String fuelUnit,
               String fuelConsumptionUnit, String fuelType, double tankVolume,
               String brand, String model, String year, String licensePlate,
               String vin, String insurancePolicy) {
        this.name = name;
        this.description = description;
        this.distanceUnit = distanceUnit;
        this.fuelUnit = fuelUnit;
        this.fuelConsumptionUnit = fuelConsumptionUnit;
        this.fuelType = fuelType;
        this.tankVolume = tankVolume;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.licensePlate = licensePlate;
        this.vin = vin;
        this.insurancePolicy = insurancePolicy;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getInsurancePolicy() { return insurancePolicy; }
    public void setInsurancePolicy(String insurancePolicy) { this.insurancePolicy = insurancePolicy; }
}