package com.example.cogcdat_2.api.models;

import com.example.cogcdat_2.Trip;
import com.google.gson.annotations.SerializedName;

public class ApiTrip {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("car_id")
    private String carId;

    @SerializedName("start_datetime")
    private String startDateTime;

    @SerializedName("end_datetime")
    private String endDateTime;

    @SerializedName("distance")
    private double distance; // Всегда в километрах!

    @SerializedName("fuel_spent")
    private double fuelSpent;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("is_deleted")
    private boolean isDeleted;

    @SerializedName("deleted_at")
    private String deletedAt;

    // Конструктор для преобразования из локальной Trip в API модель
    public ApiTrip(Trip trip) {
        this.id = trip.getId();
        this.name = trip.getName();
        this.carId = trip.getCarId();
        this.startDateTime = trip.getStartDateTime();
        this.endDateTime = trip.getEndDateTime();
        this.distance = trip.getDistance(); // Всегда в километрах!
        this.fuelSpent = trip.getFuelSpent();
        this.createdAt = trip.getCreatedAt();
        this.updatedAt = trip.getUpdatedAt();
        this.isDeleted = trip.isDeleted();
        this.deletedAt = trip.getDeletedAt();
    }

    // Пустой конструктор для Gson
    public ApiTrip() {}

    // Метод для преобразования из API модели в локальную Trip
    public Trip toLocalTrip() {
        Trip trip = new Trip(
                this.carId,
                this.name,
                this.startDateTime,
                this.endDateTime,
                this.distance, // Всегда в километрах!
                this.fuelSpent,
                this.isDeleted,
                this.deletedAt
        );
        trip.setId(this.id);
        trip.setCreatedAt(this.createdAt);
        trip.setUpdatedAt(this.updatedAt);
        return trip;
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
}