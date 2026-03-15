package com.example.cogcdat_2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.cogcdat_2.sync.SyncManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "cars.db";
    private static final int DATABASE_VERSION = 15;

    // Cars table
    private static final String TABLE_CARS = "cars";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_IMAGE_PATH = "image_path";
    private static final String COLUMN_SERVER_IMAGE_URL = "server_image_url";
    private static final String COLUMN_IMAGE_VERSION = "image_version";
    private static final String COLUMN_DISTANCE_UNIT = "distance_unit";
    private static final String COLUMN_FUEL_UNIT = "fuel_unit";
    private static final String COLUMN_FUEL_CONSUMPTION_UNIT = "fuel_consumption_unit";
    private static final String COLUMN_FUEL_TYPE = "fuel_type";
    private static final String COLUMN_TANK_VOLUME = "tank_volume";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String COLUMN_IS_DELETED = "is_deleted";
    private static final String COLUMN_DELETED_AT = "deleted_at";
    private static final String COLUMN_OWNER_ID = "owner_id";

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
    private static final String COLUMN_TRIP_IS_DELETED = "is_deleted";
    private static final String COLUMN_TRIP_DELETED_AT = "deleted_at";

    private final Context context;

    private static final String CREATE_TABLE_CARS = "CREATE TABLE " + TABLE_CARS + "("
            + COLUMN_ID + " TEXT PRIMARY KEY,"
            + COLUMN_NAME + " TEXT NOT NULL,"
            + COLUMN_DESCRIPTION + " TEXT,"
            + COLUMN_IMAGE_PATH + " TEXT,"
            + COLUMN_SERVER_IMAGE_URL + " TEXT,"
            + COLUMN_IMAGE_VERSION + " INTEGER DEFAULT 0,"
            + COLUMN_DISTANCE_UNIT + " TEXT,"
            + COLUMN_FUEL_UNIT + " TEXT,"
            + COLUMN_FUEL_CONSUMPTION_UNIT + " TEXT,"
            + COLUMN_FUEL_TYPE + " TEXT,"
            + COLUMN_TANK_VOLUME + " REAL,"
            + COLUMN_CREATED_AT + " TEXT NOT NULL,"
            + COLUMN_UPDATED_AT + " TEXT NOT NULL,"
            + COLUMN_IS_DELETED + " INTEGER DEFAULT 0,"
            + COLUMN_DELETED_AT + " TEXT,"
            + COLUMN_OWNER_ID + " TEXT"
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
            + COLUMN_TRIP_IS_DELETED + " INTEGER DEFAULT 0,"
            + COLUMN_TRIP_DELETED_AT + " TEXT,"
            + "FOREIGN KEY(" + COLUMN_CAR_ID + ") REFERENCES " + TABLE_CARS + "(" + COLUMN_ID + ") ON DELETE CASCADE"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CARS);
        db.execSQL(CREATE_TABLE_TRIPS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion < 13) {
            db.execSQL("ALTER TABLE " + TABLE_CARS + " ADD COLUMN " + COLUMN_IS_DELETED + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_CARS + " ADD COLUMN " + COLUMN_DELETED_AT + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_CARS + " ADD COLUMN " + COLUMN_OWNER_ID + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_TRIPS + " ADD COLUMN " + COLUMN_TRIP_IS_DELETED + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_TRIPS + " ADD COLUMN " + COLUMN_TRIP_DELETED_AT + " TEXT");
        }

        if (oldVersion < 14) {
            db.execSQL("ALTER TABLE " + TABLE_CARS + " ADD COLUMN " + COLUMN_SERVER_IMAGE_URL + " TEXT");
        }

        if (oldVersion < 15) {
            db.execSQL("ALTER TABLE " + TABLE_CARS + " ADD COLUMN " + COLUMN_IMAGE_VERSION + " INTEGER DEFAULT 0");
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // ========== МЕТОДЫ ДЛЯ АВТОМОБИЛЕЙ ==========

    public String addCar(Car car) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String id = car.getId() != null ? car.getId() : UUID.randomUUID().toString();
        String now = java.time.Instant.now().toString();

        values.put(COLUMN_ID, id);
        values.put(COLUMN_NAME, car.getName());
        values.put(COLUMN_DESCRIPTION, car.getDescription());
        values.put(COLUMN_IMAGE_PATH, car.getImagePath());
        values.put(COLUMN_SERVER_IMAGE_URL, car.getServerImageUrl());
        values.put(COLUMN_IMAGE_VERSION, car.getImageVersion());
        values.put(COLUMN_DISTANCE_UNIT, car.getDistanceUnit());
        values.put(COLUMN_FUEL_UNIT, car.getFuelUnit());
        values.put(COLUMN_FUEL_CONSUMPTION_UNIT, car.getFuelConsumptionUnit());
        values.put(COLUMN_FUEL_TYPE, car.getFuelType());
        values.put(COLUMN_TANK_VOLUME, car.getTankVolume());
        values.put(COLUMN_CREATED_AT, car.getCreatedAt() != null ? car.getCreatedAt() : now);
        values.put(COLUMN_UPDATED_AT, car.getUpdatedAt() != null ? car.getUpdatedAt() : now);
        values.put(COLUMN_IS_DELETED, car.isDeleted() ? 1 : 0);
        values.put(COLUMN_DELETED_AT, car.getDeletedAt());
        values.put(COLUMN_OWNER_ID, car.getOwnerId());

        long result = db.insert(TABLE_CARS, null, values);
        db.close();

        if (result != -1) {
            car.setId(id);
            triggerSync();
            return id;
        }
        return null;
    }

    public Car getCar(String id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CARS, new String[]{
                        COLUMN_ID, COLUMN_NAME, COLUMN_DESCRIPTION, COLUMN_IMAGE_PATH,
                        COLUMN_SERVER_IMAGE_URL, COLUMN_IMAGE_VERSION,
                        COLUMN_DISTANCE_UNIT, COLUMN_FUEL_UNIT, COLUMN_FUEL_CONSUMPTION_UNIT,
                        COLUMN_FUEL_TYPE, COLUMN_TANK_VOLUME, COLUMN_CREATED_AT, COLUMN_UPDATED_AT,
                        COLUMN_IS_DELETED, COLUMN_DELETED_AT, COLUMN_OWNER_ID},
                COLUMN_ID + "=?",
                new String[]{id}, null, null, null, null);

        Car car = null;
        if (cursor != null && cursor.moveToFirst()) {
            car = extractCarFromCursor(cursor);
            cursor.close();
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        db.close();
        return car;
    }

    public List<Car> getAllCars() {
        return getAllCars(false);
    }

    public List<Car> getAllCars(boolean includeDeleted) {
        List<Car> carList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CARS;
        if (!includeDeleted) {
            selectQuery += " WHERE " + COLUMN_IS_DELETED + " = 0";
        }
        selectQuery += " ORDER BY " + COLUMN_NAME + " ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Car car = extractCarFromCursor(cursor);
                carList.add(car);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return carList;
    }

    public boolean updateCar(Car car) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String now = java.time.Instant.now().toString();

        values.put(COLUMN_NAME, car.getName());
        values.put(COLUMN_DESCRIPTION, car.getDescription());
        values.put(COLUMN_IMAGE_PATH, car.getImagePath());
        values.put(COLUMN_SERVER_IMAGE_URL, car.getServerImageUrl());
        values.put(COLUMN_IMAGE_VERSION, car.getImageVersion());
        values.put(COLUMN_FUEL_TYPE, car.getFuelType());
        values.put(COLUMN_TANK_VOLUME, car.getTankVolume());
        values.put(COLUMN_DISTANCE_UNIT, car.getDistanceUnit());
        values.put(COLUMN_FUEL_UNIT, car.getFuelUnit());
        values.put(COLUMN_FUEL_CONSUMPTION_UNIT, car.getFuelConsumptionUnit());
        values.put(COLUMN_UPDATED_AT, now);
        values.put(COLUMN_IS_DELETED, car.isDeleted() ? 1 : 0);
        values.put(COLUMN_DELETED_AT, car.getDeletedAt());
        values.put(COLUMN_OWNER_ID, car.getOwnerId());

        int rowsAffected = db.update(TABLE_CARS, values,
                COLUMN_ID + " = ?", new String[]{car.getId()});
        db.close();

        if (rowsAffected > 0) {
            triggerSync();
        }
        return rowsAffected > 0;
    }

    public boolean deleteCar(String carId) {
        // Мягкое удаление
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String now = java.time.Instant.now().toString();

        values.put(COLUMN_IS_DELETED, 1);
        values.put(COLUMN_DELETED_AT, now);
        values.put(COLUMN_UPDATED_AT, now);

        int rowsAffected = db.update(TABLE_CARS, values,
                COLUMN_ID + " = ?", new String[]{carId});
        db.close();

        if (rowsAffected > 0) {
            triggerSync();
        }
        return rowsAffected > 0;
    }

    public boolean permanentDeleteCar(String carId) {
        // Физическое удаление (используется только при выходе из аккаунта)
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_CARS, COLUMN_ID + " = ?",
                new String[]{carId});
        db.close();

        if (rowsAffected > 0) {
            triggerSync();
        }
        return rowsAffected > 0;
    }

    private Car extractCarFromCursor(Cursor cursor) {
        Car car = new Car(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SERVER_IMAGE_URL)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_VERSION)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DISTANCE_UNIT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FUEL_UNIT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FUEL_CONSUMPTION_UNIT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FUEL_TYPE)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TANK_VOLUME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DELETED)) == 1,
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DELETED_AT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OWNER_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
        );
        return car;
    }

    // ========== МЕТОДЫ ДЛЯ ПОЕЗДОК ==========

    public String addTrip(Trip trip) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        String id = trip.getId() != null ? trip.getId() : UUID.randomUUID().toString();
        String now = java.time.Instant.now().toString();

        values.put(COLUMN_TRIP_ID, id);
        values.put(COLUMN_CAR_ID, trip.getCarId());
        values.put(COLUMN_TRIP_NAME, trip.getName());
        values.put(COLUMN_TRIP_START_DATETIME, trip.getStartDateTime());
        values.put(COLUMN_TRIP_END_DATETIME, trip.getEndDateTime());
        values.put(COLUMN_TRIP_DISTANCE, trip.getDistance());
        values.put(COLUMN_TRIP_FUEL_SPENT, trip.getFuelSpent());
        values.put(COLUMN_TRIP_FUEL_CONSUMPTION, trip.getFuelConsumption());
        values.put(COLUMN_TRIP_CREATED_AT, trip.getCreatedAt() != null ? trip.getCreatedAt() : now);
        values.put(COLUMN_TRIP_UPDATED_AT, trip.getUpdatedAt() != null ? trip.getUpdatedAt() : now);
        values.put(COLUMN_TRIP_IS_DELETED, trip.isDeleted() ? 1 : 0);
        values.put(COLUMN_TRIP_DELETED_AT, trip.getDeletedAt());

        long result = db.insert(TABLE_TRIPS, null, values);
        db.close();

        if (result != -1) {
            trip.setId(id);
            triggerSync();
            return id;
        }
        return null;
    }

    public Trip getTrip(String id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TRIPS, new String[]{
                        COLUMN_TRIP_ID, COLUMN_CAR_ID, COLUMN_TRIP_NAME,
                        COLUMN_TRIP_START_DATETIME, COLUMN_TRIP_END_DATETIME,
                        COLUMN_TRIP_DISTANCE, COLUMN_TRIP_FUEL_SPENT, COLUMN_TRIP_FUEL_CONSUMPTION,
                        COLUMN_TRIP_CREATED_AT, COLUMN_TRIP_UPDATED_AT,
                        COLUMN_TRIP_IS_DELETED, COLUMN_TRIP_DELETED_AT},
                COLUMN_TRIP_ID + "=?",
                new String[]{id}, null, null, null, null);

        Trip trip = null;
        if (cursor != null && cursor.moveToFirst()) {
            trip = extractTripFromCursor(cursor);
            cursor.close();
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        db.close();
        return trip;
    }

    public List<Trip> getTripsForCar(String carId) {
        return getTripsForCar(carId, false);
    }

    public List<Trip> getTripsForCar(String carId, boolean includeDeleted) {
        List<Trip> tripList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TRIPS +
                " WHERE " + COLUMN_CAR_ID + " = ?";
        if (!includeDeleted) {
            selectQuery += " AND " + COLUMN_TRIP_IS_DELETED + " = 0";
        }
        selectQuery += " ORDER BY " + COLUMN_TRIP_START_DATETIME + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{carId});

        if (cursor.moveToFirst()) {
            do {
                Trip trip = extractTripFromCursor(cursor);
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
        values.put(COLUMN_TRIP_IS_DELETED, trip.isDeleted() ? 1 : 0);
        values.put(COLUMN_TRIP_DELETED_AT, trip.getDeletedAt());

        int rowsAffected = db.update(TABLE_TRIPS, values,
                COLUMN_TRIP_ID + " = ?", new String[]{trip.getId()});
        db.close();

        if (rowsAffected > 0) {
            triggerSync();
        }
        return rowsAffected > 0;
    }

    public void deleteTrip(String tripId) {
        // Мягкое удаление
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String now = java.time.Instant.now().toString();

        values.put(COLUMN_TRIP_IS_DELETED, 1);
        values.put(COLUMN_TRIP_DELETED_AT, now);
        values.put(COLUMN_TRIP_UPDATED_AT, now);

        db.update(TABLE_TRIPS, values, COLUMN_TRIP_ID + " = ?", new String[]{tripId});
        db.close();

        triggerSync();
    }

    public void permanentDeleteTrip(String tripId) {
        // Физическое удаление (используется только при выходе из аккаунта)
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRIPS, COLUMN_TRIP_ID + " = ?", new String[]{tripId});
        db.close();

        triggerSync();
    }

    private Trip extractTripFromCursor(Cursor cursor) {
        Trip trip = new Trip(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAR_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_START_DATETIME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_END_DATETIME)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TRIP_DISTANCE)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TRIP_FUEL_SPENT)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TRIP_FUEL_CONSUMPTION)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TRIP_IS_DELETED)) == 1,
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_DELETED_AT))
        );
        trip.setId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_ID)));
        trip.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_CREATED_AT)));
        trip.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRIP_UPDATED_AT)));
        return trip;
    }

    // ========== СИНХРОНИЗАЦИЯ ==========

    private void triggerSync() {
        try {
            SyncManager syncManager = SyncManager.getInstance(context);
            if (syncManager.getSavedToken() != null) {
                syncManager.syncAll();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при запуске синхронизации: " + e.getMessage());
        }
    }

    public Context getContext() {
        return context;
    }
}