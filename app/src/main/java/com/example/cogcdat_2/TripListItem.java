package com.example.cogcdat_2;

// Используется для адаптера, который должен отображать разные типы элементов
public class TripListItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_TRIP = 1;

    private int type;
    private Trip trip;
    private String header; // Например, "Ноябрь 2025"

    // Конструктор для Заголовка
    public TripListItem(String header) {
        this.type = TYPE_HEADER;
        this.header = header;
    }

    // Конструктор для Поездки
    public TripListItem(Trip trip) {
        this.type = TYPE_TRIP;
        this.trip = trip;
    }

    public int getType() {
        return type;
    }

    public Trip getTrip() {
        return trip;
    }

    public String getHeader() {
        return header;
    }
}