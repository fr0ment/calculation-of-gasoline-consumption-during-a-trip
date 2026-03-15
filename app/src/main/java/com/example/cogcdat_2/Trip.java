package com.example.cogcdat_2;

// Модель данных для таблицы Trips
public class Trip {
    private String id;
    private String name; // Название поездки
    private String carId; // id_Машины
    private String startDateTime; // Дата и время отправления (TEXT, ISO8601)
    private String endDateTime;   // Дата и время прибытия (TEXT, ISO8601)
    private double distance; // Расстояние
    private double fuelSpent; // Количество потраченного топлива
    private double fuelConsumption; // Расход топлива (л/100км)
    private String createdAt;
    private String updatedAt;
    private boolean isDeleted;
    private String deletedAt;

    // Пустой конструктор
    public Trip() {}

    // Полный конструктор со всеми полями
    public Trip(String id, String carId, String name, String startDateTime,
                String endDateTime, double distance, double fuelSpent,
                double fuelConsumption, String createdAt, String updatedAt,
                boolean isDeleted, String deletedAt) {
        this.id = id;
        this.carId = carId;
        this.name = name;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.distance = distance;
        this.fuelSpent = fuelSpent;
        this.fuelConsumption = fuelConsumption;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
    }

    // Конструктор для создания новой поездки
    public Trip(String carId, String name, String startDateTime,
                String endDateTime, double distance, double fuelSpent,
                double fuelConsumption) {
        this(null, carId, name, startDateTime, endDateTime, distance,
                fuelSpent, fuelConsumption, null, null, false, null);
    }

    // Конструктор с isDeleted (для восстановления из БД)
    public Trip(String carId, String name, String startDateTime,
                String endDateTime, double distance, double fuelSpent,
                double fuelConsumption, boolean isDeleted, String deletedAt) {
        this(null, carId, name, startDateTime, endDateTime, distance,
                fuelSpent, fuelConsumption, null, null, isDeleted, deletedAt);
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

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public String getDeletedAt() { return deletedAt; }
    public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }

    // Метод для проверки, нужно ли синхронизировать
    public boolean needsSync(String lastSyncTime) {
        if (lastSyncTime == null) return true;
        if (updatedAt == null) return true;
        return updatedAt.compareTo(lastSyncTime) > 0;
    }

    // Метод для создания копии
    public Trip copy() {
        return new Trip(
                this.id,
                this.carId,
                this.name,
                this.startDateTime,
                this.endDateTime,
                this.distance,
                this.fuelSpent,
                this.fuelConsumption,
                this.createdAt,
                this.updatedAt,
                this.isDeleted,
                this.deletedAt
        );
    }

    // Метод для расчета длительности в миллисекундах
    public long getDurationMs() {
        try {
            java.text.SimpleDateFormat format =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date start = format.parse(this.startDateTime);
            java.util.Date end = format.parse(this.endDateTime);
            return end.getTime() - start.getTime();
        } catch (java.text.ParseException e) {
            return 0;
        }
    }

    // Метод для форматированной длительности
    public String getFormattedDuration() {
        long ms = getDurationMs();
        long hours = ms / (1000 * 60 * 60);
        long minutes = (ms % (1000 * 60 * 60)) / (1000 * 60);
        return String.format(java.util.Locale.getDefault(), "%d ч %02d мин", hours, minutes);
    }
}