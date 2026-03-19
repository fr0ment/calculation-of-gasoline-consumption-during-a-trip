package com.example.cogcdat_2.api.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SyncResponse {
    @SerializedName("cars")
    private List<ApiCar> cars;

    @SerializedName("trips")
    private List<ApiTrip> trips;

    @SerializedName("settings") // Добавляем поле для настроек
    private ApiUserSettings settings;

    @SerializedName("deleted_car_ids")
    private List<String> deletedCarIds;

    @SerializedName("deleted_trip_ids")
    private List<String> deletedTripIds;

    @SerializedName("server_time")
    private String serverTime;

    public List<ApiCar> getCars() { return cars; }
    public List<ApiTrip> getTrips() { return trips; }
    public ApiUserSettings getSettings() { return settings; }
    public List<String> getDeletedCarIds() { return deletedCarIds; }
    public List<String> getDeletedTripIds() { return deletedTripIds; }
    public String getServerTime() { return serverTime; }
}