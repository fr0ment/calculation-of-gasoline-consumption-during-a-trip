package com.example.cogcdat_2;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class SelectedCarManager {
    private static final String KEY_SELECTED_CAR_ID = "selected_car_id";

    public static void setSelectedCarId(Context context, String carId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(KEY_SELECTED_CAR_ID, carId).apply();
    }

    public static String getSelectedCarId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(KEY_SELECTED_CAR_ID, "");
    }

    public static void clear(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(KEY_SELECTED_CAR_ID).apply();
    }
}