package com.example.cogcdat_2;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Trip {
    private String id;
    private String name;
    private String carId;
    private String startDateTime;
    private String endDateTime;
    private double distance; // Всегда в километрах!
    private double fuelSpent;
    private String createdAt;
    private String updatedAt;
    private boolean isDeleted;
    private String deletedAt;

    private static final SimpleDateFormat DB_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // Пустой конструктор
    public Trip() {}

    // Полный конструктор для БД (со всеми полями)
    public Trip(String id, String carId, String name, String startDateTime,
                String endDateTime, double distance, double fuelSpent,
                String createdAt, String updatedAt, boolean isDeleted, String deletedAt) {
        this.id = id;
        this.carId = carId;
        this.name = name;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.distance = distance;
        this.fuelSpent = fuelSpent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
    }

    // Конструктор для создания новой поездки (без ID и служебных полей)
    public Trip(String carId, String name, String startDateTime,
                String endDateTime, double distance, double fuelSpent) {
        this(null, carId, name, startDateTime, endDateTime, distance,
                fuelSpent, null, null, false, null);
    }

    // Конструктор для восстановления из БД (с isDeleted)
    public Trip(String carId, String name, String startDateTime,
                String endDateTime, double distance, double fuelSpent,
                boolean isDeleted, String deletedAt) {
        this(null, carId, name, startDateTime, endDateTime, distance,
                fuelSpent, null, null, isDeleted, deletedAt);
    }

    // Геттеры и сеттеры
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

    // Для установки расстояния из пользовательского ввода (с конвертацией)
    public void setDistanceFromUserInput(double value, DistanceUnit inputUnit) {
        if (inputUnit == DistanceUnit.MI) {
            this.distance = value * 1.60934; // мили -> км
        } else {
            this.distance = value; // уже км
        }
    }

    // Для получения расстояния в нужных единицах (для отображения)
    public double getDistanceInUnit(DistanceUnit targetUnit) {
        if (targetUnit == DistanceUnit.MI) {
            return distance / 1.60934; // км -> мили
        }
        return distance; // км
    }

    public double getFuelSpent() { return fuelSpent; }
    public void setFuelSpent(double fuelSpent) { this.fuelSpent = fuelSpent; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public String getDeletedAt() { return deletedAt; }
    public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }

    // Вычисляемый расход топлива (всегда л/100км)
    public double getFuelConsumption() {
        if (distance > 0 && fuelSpent > 0) {
            return (fuelSpent / distance) * 100;
        }
        return 0.0;
    }

    // Расход в выбранных единицах расстояния
    public double getFuelConsumptionInUnit(DistanceUnit targetUnit) {
        double consumption = getFuelConsumption();
        if (targetUnit == DistanceUnit.MI) {
            // Конвертация л/100км в л/100миль
            return consumption * 1.60934;
        }
        return consumption;
    }

    public long getDurationMs() {
        try {
            Date start = DB_DATE_FORMAT.parse(this.startDateTime);
            Date end = DB_DATE_FORMAT.parse(this.endDateTime);
            return end.getTime() - start.getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    public String getFormattedDuration() {
        long ms = getDurationMs();
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
        return String.format(Locale.getDefault(), "%d ч %02d мин", hours, minutes);
    }
}