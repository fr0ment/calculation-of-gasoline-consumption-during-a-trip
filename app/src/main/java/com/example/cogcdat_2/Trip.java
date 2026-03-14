package com.example.cogcdat_2;

// Модель данных для таблицы Trips
public class Trip {
    private String id;
    private String name; // Навзание поездки
    private String carId; // id_Машины
    private String startDateTime; // Дата и время отправления (TEXT, ISO8601)
    private String endDateTime;   // Дата и время прибытия (TEXT, ISO8601)
    private double distance; // Расстояние
    private double fuelSpent; // Количество потраченного топлива
    private double fuelConsumption; // Расход топлива (л/100км)
    private String createdAt;
    private String updatedAt;

    public Trip() {
    }

    public Trip(String carId, String name, String startDateTime, String endDateTime, double distance, double fuelSpent, double fuelConsumption) {
        this.carId = carId;
        this.name = name;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.distance = distance;
        this.fuelSpent = fuelSpent;
        this.fuelConsumption = fuelConsumption;
    }

    // Геттеры и Сеттеры

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCarId() { return carId; }
    public void setCarId(String carId) { this.carId = carId; }
    public String getStartDateTime() { return startDateTime; }
    public void setStartDateTime(String startDateTime) { this.startDateTime = startDateTime; }
    public String getEndDateTime() { return endDateTime; }
    public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
    public double getFuelSpent() { return fuelSpent; }
    public void setFuelSpent(double fuelSpent) { this.fuelSpent = fuelSpent; }
    public double getFuelConsumption() { return fuelConsumption; }
    public void setFuelConsumption(double fuelConsumption) { this.fuelConsumption = fuelConsumption; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

}