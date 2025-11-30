package com.example.cogcdat_2;

// Модель данных для таблицы Trips
public class Trip {
    private int id;
    private String name;
    private int carId; // id_Машины
    private String startDateTime; // Дата и время отправления (TEXT, ISO8601)
    private String endDateTime;   // Дата и время прибытия (TEXT, ISO8601)
    private double distance; // Расстояние
    private double fuelSpent; // Количество потраченного топлива
    private double fuelConsumption; // Расход топлива (л/100км)

    public Trip() {
    }

    public Trip(int carId, String name, String startDateTime, String endDateTime, double distance, double fuelSpent, double fuelConsumption) {
        this.carId = carId;
        this.name = name;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.distance = distance;
        this.fuelSpent = fuelSpent;
        this.fuelConsumption = fuelConsumption;
        // ... остальные поля
    }

    // Геттеры и Сеттеры

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCarId() { return carId; }
    public void setCarId(int carId) { this.carId = carId; }
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

    // Метод для расчета времени в пути (пример)
    // В реальном приложении лучше использовать JodaTime или java.time, но для простоты здесь
    // предполагается, что вы будете хранить и оперировать строками ISO8601 в БД.
    // Реальный расчет времени лучше делать при отображении, используя библиотеки.
}