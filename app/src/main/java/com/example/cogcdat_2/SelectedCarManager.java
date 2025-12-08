package com.example.cogcdat_2;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class SelectedCarManager {
    private static final String KEY_SELECTED_CAR_ID = "selected_car_id";

    public static void setSelectedCarId(Context context, int carId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(KEY_SELECTED_CAR_ID, carId).apply();
    }

    public static int getSelectedCarId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(KEY_SELECTED_CAR_ID, -1);
    }

    public static void clear(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(KEY_SELECTED_CAR_ID).apply();
    }
}