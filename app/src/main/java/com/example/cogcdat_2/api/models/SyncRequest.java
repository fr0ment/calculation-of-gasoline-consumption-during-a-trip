package com.example.cogcdat_2.api.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SyncRequest {
    @SerializedName("last_sync")
    private String lastSync;

    @SerializedName("cars")
    private List<ApiCar> cars;

    @SerializedName("trips")
    private List<ApiTrip> trips;

    public SyncRequest(String lastSync, List<ApiCar> cars, List<ApiTrip> trips) {
        this.lastSync = lastSync;
        this.cars = cars;
        this.trips = trips;
    }

    public String getLastSync() { return lastSync; }
    public List<ApiCar> getCars() { return cars; }
    public List<ApiTrip> getTrips() { return trips; }
}