package com.example.cogcdat_2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "cars.db";
    private static final int DATABASE_VERSION = 12;

    // Cars table
    private static final String TABLE_CARS = "cars";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_IMAGE_PATH = "image_path";
    private static final String COLUMN_DISTANCE_UNIT = "distance_unit";
    private static final String COLUMN_FUEL_UNIT = "fuel_unit";
    private static final String COLUMN_FUEL_CONSUMPTION_UNIT = "fuel_consumption_unit";
    private static final String COLUMN_FUEL_TYPE = "fuel_type";
    private static final String COLUMN_TANK_VOLUME = "tank_volume";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";

    // Trips table
    private static final String TABLE_TRIPS = "trips";
    private static final String COLUMN_TRIP_ID = "id";
    private static final String COLUMN_CAR_ID = "car_id";
    private static final String COLUMN_TRIP_NAME = "name";
    private static final String COLUMN_TRIP_START_DATETIME = "start_datetime";
    private static final String COLUMN_TRIP_END_DATETIME = "end_datetime";
    private static final String COLUMN_TRIP_DISTANCE = "distance";
    private static final String COLUMN_TRIP_FUEL_SPENT = "fuel_spent";
    private static final String COLUMN_TRIP_FUEL_CONSUMPTION = "fuel_consumption";
    private static final String COLUMN_TRIP_CREATED_AT = "created_at";
    private static final String COLUMN_TRIP_UPDATED_AT = "updated_at";

    private static final String CREATE_TABLE_CARS = "CREATE TABLE " + TABLE_CARS + "("
            + COLUMN_ID + " TEXT PRIMARY KEY,"
            + COLUMN_NAME + " TEXT NOT NULL,"
            + COLUMN_DESCRIPTION + " TEXT,"
            + COLUMN_IMAGE_PATH + " TEXT,"
            + COLUMN_DISTANCE_UNIT + " TEXT,"
            + COLUMN_FUEL_UNIT + " TEXT,"
            + COLUMN_FUEL_CONSUMPTION_UNIT + " TEXT,"
            + COLUMN_FUEL_TYPE + " TEXT,"
            + COLUMN_TANK_VOLUME + " REAL,"
            + COLUMN_CREATED_AT + " TEXT NOT NULL,"
            + COLUMN_UPDATED_AT + " TEXT NOT NULL"
            + ")"; 

    private static final String CREATE_TABLE_TRIPS = "CREATE TABLE " + TABLE_TRIPS + "("
            + COLUMN_TRIP_ID + " TEXT PRIMARY KEY,"
            + COLUMN_CAR_ID + " TEXT,"
            + COLUMN_TRIP_NAME + " TEXT,"
            + COLUMN_TRIP_START_DATETIME + " TEXT,"
            + COLUMN_TRIP_END_DATETIME + " TEXT,"
            + COLUMN_TRIP_DISTANCE + " REAL,"
            + COLUMN_TRIP_FUEL_SPENT + " REAL,"
            + COLUMN_TRIP_FUEL_CONSUMPTION + " REAL,"
            + COLUMN_TRIP_CREATED_AT + " TEXT NOT NULL,"
            + COLUMN_TRIP_UPDATED_AT + " TEXT NOT NULL,"
            + "FOREIGN KEY(" + COLUMN_CAR_ID + ") REFERENCES " + TABLE_CARS + "(" + COLUMN_ID + ") ON DELETE CASCADE"
            + ")"; 

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CARS);
        db.execSQL(CREATE_TABLE_TRIPS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CARS);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // Cars methods
    public String addCar(Car car) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String id = UUID.randomUUID().toString();
        String now = java.time.Instant.now().toString();
        values.put(COLUMN_ID, id);
        values.put(COLUMN_NAME, car.getName());
        values.put(COLUMN_DESCRIPTION, car.getDescription());
        values.put(COLUMN_IMAGE_PATH, car.getImagePath());
        values.put(COLUMN_DISTANCE_UNIT, car.getDistanceUnit());
        values.put(COLUMN_FUEL_UNIT, car.getFuelUnit());
        values.put(COLUMN_FUEL_CONSUMPTION_UNIT, car.getFuelConsumptionUnit());
        values.put(COLUMN_FUEL_TYPE, car.getFuelType());
        values.put(COLUMN_TANK_VOLUME, car.getTankVolume());
        values.put(COLUMN_CREATED_AT, now);
        values.put(COLUMN_UPDATED_AT, now);

        db.insert(TABLE_CARS, null, values);
        db.close();
        car.setId(id);
        return id;
    }

    public Car getCar(String id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CARS, new String[]{
                        COLUMN_ID, COLUMN_NAME, COLUMN_DESCRIPTION, COLUMN_IMAGE_PATH,
                        COLUMN_DISTANCE_UNIT, COLUMN_FUEL_UNIT, COLUMN_FUEL_CONSUMPTION_UNIT,
                        COLUMN_FUEL_TYPE, COLUMN_TANK_VOLUME},
                COLUMN_ID + "=?",
                new String[]{id}, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Car car = new Car(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DISTANCE_UNIT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FUEL_UNIT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FUEL_CONSUMPTION_UNIT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FUEL_TYPE)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TANK_VOLUME))
            );
            cursor.close();
            db.close();
            return car;
        }
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return null;
    }

    public List<Car> getAllCars() {
        List<Car> carList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CARS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Car car = new Car(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DISTANCE_UNIT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FUEL_UNIT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FUEL_CONSUMPTION_UNIT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FUEL_TYPE)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TANK_VOLUME))
                );
                carList.add(car);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return carList;
    }

    // Trips methods
    public String addTrip(Trip trip) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String id = UUID.randomUUID().toString();
        String now = java.time.Instant.now().toString();
        values.put(COLUMN_TRIP_ID, id);
        values.put(COLUMN_CAR_ID, trip.getCarId());
        values.put(COLUMN_TRIP_NAME, trip.getName());
        values.put(COLUMN_TRIP_START_DATETIME, trip.getStartDateTime());
        values.put(COLUMN_TRIP_END_DATETIME, trip.getEndDateTime());
        values.put(COLUMN_TRIP_DISTANCE, trip.getDistance());
        values.put(COLUMN_TRIP_FUEL_SPENT, trip.getFuelSpent());
        values.put(COLUMN_TRIP_FUEL_CONSUMPTION, trip.getFuelConsumption());
        values.put(COLUMN_TRIP_CREATED_AT, now);
        values.put(COLUMN_TRIP_UPDATED_AT, now);

        db.insert(TABLE_TRIPS, null, values);
        db.close();
        trip.setId(id);
        return id;
    }

    public Trip getTrip(String id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TRIPS, new String[]{
                        COLUMN_TRIP_ID, COLUMN_CAR_ID, COLUMN_TRIP_NAME,
                        COLUMN_TRIP_START_DATETIME, COLUMN_TRIP_END_DATETIME,
                        COLUMN_TRIP_DISTANCE, COLUMN_TRIP_FUEL_SPENT, COLUMN_TRIP_FUEL_CONSUMPTION},
                COLUMN_TRIP_ID + "=?",
                new String[]{id}, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Trip trip = new Trip();
            trip.setId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_ID)));
            trip.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_NAME)));
            trip.setCarId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAR_ID)));
            trip.setStartDateTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_START_DATETIME)));
            trip.setEndDateTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_END_DATETIME)));
            trip.setDistance(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TRIP_DISTANCE)));
            trip.setFuelSpent(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TRIP_FUEL_SPENT)));
            trip.setFuelConsumption(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TRIP_FUEL_CONSUMPTION)));
            cursor.close();
            db.close();
            return trip;
        }
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return null;
    }

    public List<Trip> getTripsForCar(String carId) {
        List<Trip> tripList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TRIPS + " WHERE " + COLUMN_CAR_ID + " = ? ORDER BY " + COLUMN_TRIP_START_DATETIME + " DESC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{carId});

        if (cursor.moveToFirst()) {
            do {
                Trip trip = new Trip();
                trip.setId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_ID)));
                trip.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_NAME)));
                trip.setCarId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAR_ID)));
                trip.setStartDateTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_START_DATETIME)));
                trip.setEndDateTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_END_DATETIME)));
                trip.setDistance(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TRIP_DISTANCE)));
                trip.setFuelSpent(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TRIP_FUEL_SPENT)));
                trip.setFuelConsumption(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TRIP_FUEL_CONSUMPTION)));
                tripList.add(trip);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tripList;
    }

    public boolean updateTrip(Trip trip) {
        SQLiteDatabase db = this.getWritableDatabase();
        String now = java.time.Instant.now().toString();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRIP_NAME, trip.getName());
        values.put(COLUMN_TRIP_START_DATETIME, trip.getStartDateTime());
        values.put(COLUMN_TRIP_END_DATETIME, trip.getEndDateTime());
        values.put(COLUMN_TRIP_DISTANCE, trip.getDistance());
        values.put(COLUMN_TRIP_FUEL_SPENT, trip.getFuelSpent());
        values.put(COLUMN_TRIP_FUEL_CONSUMPTION, trip.getFuelConsumption());
        values.put(COLUMN_TRIP_UPDATED_AT, now);

        int rowsAffected = db.update(TABLE_TRIPS, values,
                COLUMN_TRIP_ID + " = ?", new String[]{trip.getId()});
        db.close();

        return rowsAffected > 0;
    }

    public void deleteTrip(String tripId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRIPS, COLUMN_TRIP_ID + " = ?",
                new String[]{tripId});
        db.close();
    }

    public boolean updateCar(Car car) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String now = java.time.Instant.now().toString();
        values.put(COLUMN_NAME, car.getName());
        values.put(COLUMN_DESCRIPTION, car.getDescription());
        values.put(COLUMN_FUEL_TYPE, car.getFuelType());
        values.put(COLUMN_TANK_VOLUME, car.getTankVolume());
        values.put(COLUMN_DISTANCE_UNIT, car.getDistanceUnit());
        values.put(COLUMN_FUEL_UNIT, car.getFuelUnit());
        values.put(COLUMN_FUEL_CONSUMPTION_UNIT, car.getFuelConsumptionUnit());
        values.put(COLUMN_IMAGE_PATH, car.getImagePath());
        values.put(COLUMN_UPDATED_AT, now);

        int rowsAffected = db.update(TABLE_CARS, values,
                COLUMN_ID + " = ?", new String[]{car.getId()});
        db.close();

        return rowsAffected > 0;
    }

    public boolean deleteCar(String carId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_CARS, COLUMN_ID + " = ?",
                new String[]{carId});
        db.close();

        return rowsAffected > 0;
    }
}

