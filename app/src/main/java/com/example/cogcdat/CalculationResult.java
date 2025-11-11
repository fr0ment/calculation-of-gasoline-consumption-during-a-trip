package com.example.cogcdat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CalculationResult {
    private int id;
    private double distance;
    private double fuelConsumption;
    private double fuelPrice;
    private double fuelNeeded;
    private double tripCost;
    private String timestamp;

    // Конструктор для новых расчетов
    public CalculationResult(double distance, double fuelConsumption, double fuelPrice,
                             double fuelNeeded, double tripCost) {
        this.distance = distance;
        this.fuelConsumption = fuelConsumption;
        this.fuelPrice = fuelPrice;
        this.fuelNeeded = fuelNeeded;
        this.tripCost = tripCost;
        this.timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());
    }

    // Конструктор для данных из БД (без timestamp в БД)
    public CalculationResult(int id, double distance, double fuelConsumption, double fuelPrice,
                             double fuelNeeded, double tripCost, String timestamp) {
        this.id = id;
        this.distance = distance;
        this.fuelConsumption = fuelConsumption;
        this.fuelPrice = fuelPrice;
        this.fuelNeeded = fuelNeeded;
        this.tripCost = tripCost;
        this.timestamp = timestamp; // Генерируем в коде
    }

    // Геттеры
    public int getId() { return id; }
    public double getDistance() { return distance; }
    public double getFuelConsumption() { return fuelConsumption; }
    public double getFuelPrice() { return fuelPrice; }
    public double getFuelNeeded() { return fuelNeeded; }
    public double getTripCost() { return tripCost; }
    public String getTimestamp() { return timestamp; }

    // Сеттеры
    public void setId(int id) { this.id = id; }
    public void setDistance(double distance) { this.distance = distance; }
    public void setFuelConsumption(double fuelConsumption) { this.fuelConsumption = fuelConsumption; }
    public void setFuelPrice(double fuelPrice) { this.fuelPrice = fuelPrice; }
    public void setFuelNeeded(double fuelNeeded) { this.fuelNeeded = fuelNeeded; }
    public void setTripCost(double tripCost) { this.tripCost = tripCost; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return String.format("Расстояние: %.1f км, Топливо: %.1f л, Стоимость: %.2f руб",
                distance, fuelNeeded, tripCost);
    }
}