package com.example.cogcdat_2;

// Класс для передачи данных о поездке через LiveData
public class TripRecordingData {
    private final double distance; // Общий пробег в км
    private final long durationMs; // Общая длительность в мс
    private final double fuelRecharged; // Заправленное топливо
    private final boolean isPaused; // Состояние паузы

    public TripRecordingData(double distance, long durationMs, double fuelRecharged, boolean isPaused) {
        this.distance = distance;
        this.durationMs = durationMs;
        this.fuelRecharged = fuelRecharged;
        this.isPaused = isPaused;
    }

    public double getDistance() { return distance; }
    public long getDurationMs() { return durationMs; }
    public double getFuelRecharged() { return fuelRecharged; }
    public boolean isPaused() { return isPaused; }
}