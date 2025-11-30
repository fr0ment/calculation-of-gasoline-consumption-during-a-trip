package com.example.cogcdat_2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "cars.db";
    // УВЕЛИЧИВАЕМ ВЕРСИЮ БД, чтобы сработал onUpgrade или чтобы создалась таблица Trips
    private static final int DATABASE_VERSION = 6;

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
    private static final String COLUMN_BRAND = "brand";
    private static final String COLUMN_MODEL = "model";
    private static final String COLUMN_YEAR = "year";
    private static final String COLUMN_LICENSE_PLATE = "license_plate";
    private static final String COLUMN_VIN = "vin";
    private static final String COLUMN_INSURANCE_POLICY = "insurance_policy";


    // --- НОВЫЕ КОНСТАНТЫ ДЛЯ ТАБЛИЦЫ TRIPS ---
    private static final String TABLE_TRIPS = "trips";
    private static final String COLUMN_TRIP_CAR_ID = "car_id";
    private static final String COLUMN_TRIP_START_DATETIME = "start_datetime";
    private static final String COLUMN_TRIP_END_DATETIME = "end_datetime";
    private static final String COLUMN_TRIP_DISTANCE = "distance";
    private static final String COLUMN_TRIP_FUEL_SPENT = "fuel_spent";
    private static final String COLUMN_TRIP_FUEL_CONSUMPTION = "fuel_consumption";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Создание таблицы CARS (ваш существующий код)
        String CREATE_CARS_TABLE = "CREATE TABLE " + TABLE_CARS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_IMAGE_PATH + " TEXT,"
                + COLUMN_DISTANCE_UNIT + " TEXT,"
                + COLUMN_FUEL_UNIT + " TEXT,"
                + COLUMN_FUEL_CONSUMPTION_UNIT + " TEXT,"
                + COLUMN_FUEL_TYPE + " TEXT,"
                + COLUMN_TANK_VOLUME + " REAL,"
                + COLUMN_BRAND + " TEXT,"
                + COLUMN_MODEL + " TEXT,"
                + COLUMN_YEAR + " TEXT,"
                + COLUMN_LICENSE_PLATE + " TEXT,"
                + COLUMN_VIN + " TEXT,"
                + COLUMN_INSURANCE_POLICY + " TEXT"
                + ")";
        db.execSQL(CREATE_CARS_TABLE);

        // --- СОЗДАНИЕ ТАБЛИЦЫ TRIPS ---
        String CREATE_TRIPS_TABLE = "CREATE TABLE " + TABLE_TRIPS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT," // Название поездки
                + COLUMN_TRIP_CAR_ID + " INTEGER," // ID_Машины
                + COLUMN_TRIP_START_DATETIME + " TEXT," // Дата и время отправления (ISO8601)
                + COLUMN_TRIP_END_DATETIME + " TEXT," // Дата и время прибытия (ISO8601)
                + COLUMN_TRIP_DISTANCE + " REAL," // Расстояние
                + COLUMN_TRIP_FUEL_SPENT + " REAL," // Количество потраченного топлива
                + COLUMN_TRIP_FUEL_CONSUMPTION + " REAL," // Расход топлива (Рассчитываемый)
                // Связь с таблицей CARS (внешний ключ)
                + "FOREIGN KEY(" + COLUMN_TRIP_CAR_ID + ") REFERENCES " + TABLE_CARS + "(" + COLUMN_ID + ")"
                + ")";
        db.execSQL(CREATE_TRIPS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // При обновлении БД удаляем и пересоздаем обе таблицы
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CARS);
        onCreate(db);
    }

    // --- МЕТОДЫ ДЛЯ CARS ---

    // Изменяем метод addCar чтобы он возвращал ID созданной записи
    public long addCar(Car car) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, car.getName());
        values.put(COLUMN_DESCRIPTION, car.getDescription());
        values.put(COLUMN_IMAGE_PATH, car.getImagePath());
        values.put(COLUMN_DISTANCE_UNIT, car.getDistanceUnit());
        values.put(COLUMN_FUEL_UNIT, car.getFuelUnit());
        values.put(COLUMN_FUEL_CONSUMPTION_UNIT, car.getFuelConsumptionUnit());
        values.put(COLUMN_FUEL_TYPE, car.getFuelType());
        values.put(COLUMN_TANK_VOLUME, car.getTankVolume());
        values.put(COLUMN_BRAND, car.getBrand());
        values.put(COLUMN_MODEL, car.getModel());
        values.put(COLUMN_YEAR, car.getYear());
        values.put(COLUMN_LICENSE_PLATE, car.getLicensePlate());
        values.put(COLUMN_VIN, car.getVin());
        values.put(COLUMN_INSURANCE_POLICY, car.getInsurancePolicy());

        long id = db.insert(TABLE_CARS, null, values);
        db.close();
        return id; // Возвращаем ID новой записи
    }

    public List<Car> getAllCars() {
        List<Car> carList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CARS;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Car car = new Car();
                car.setId(cursor.getInt(0));
                car.setName(cursor.getString(1));
                car.setDescription(cursor.getString(2));
                car.setImagePath(cursor.getString(3));
                car.setDistanceUnit(cursor.getString(4));
                car.setFuelUnit(cursor.getString(5));
                car.setFuelConsumptionUnit(cursor.getString(6));
                car.setFuelType(cursor.getString(7));
                car.setTankVolume(cursor.getDouble(8));
                car.setBrand(cursor.getString(9));
                car.setModel(cursor.getString(10));
                car.setYear(cursor.getString(11));
                car.setLicensePlate(cursor.getString(12));
                car.setVin(cursor.getString(13));
                car.setInsurancePolicy(cursor.getString(14));
                carList.add(car);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return carList;
    }

    public Car getCarById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CARS, null, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Car car = new Car();
            car.setId(cursor.getInt(0));
            car.setName(cursor.getString(1));
            car.setDescription(cursor.getString(2));
            car.setImagePath(cursor.getString(3));
            car.setDistanceUnit(cursor.getString(4));
            car.setFuelUnit(cursor.getString(5));
            car.setFuelConsumptionUnit(cursor.getString(6));
            car.setFuelType(cursor.getString(7));
            car.setTankVolume(cursor.getDouble(8));
            car.setBrand(cursor.getString(9));
            car.setModel(cursor.getString(10));
            car.setYear(cursor.getString(11));
            car.setLicensePlate(cursor.getString(12));
            car.setVin(cursor.getString(13));
            car.setInsurancePolicy(cursor.getString(14));
            cursor.close();
            return car;
        }
        return null;
    }

    // --- НОВЫЙ МЕТОД: Добавить поездку (Trip) ---
    public long addTrip(Trip trip) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, trip.getName());
        values.put(COLUMN_TRIP_CAR_ID, trip.getCarId());
        values.put(COLUMN_TRIP_START_DATETIME, trip.getStartDateTime());
        values.put(COLUMN_TRIP_END_DATETIME, trip.getEndDateTime());
        values.put(COLUMN_TRIP_DISTANCE, trip.getDistance());
        values.put(COLUMN_TRIP_FUEL_SPENT, trip.getFuelSpent());
        values.put(COLUMN_TRIP_FUEL_CONSUMPTION, trip.getFuelConsumption());

        long id = db.insert(TABLE_TRIPS, null, values);
        db.close();
        return id;
    }

    // --- НОВЫЙ МЕТОД: Получить все поездки ---
    public List<Trip> getAllTrips() {
        List<Trip> tripList = new ArrayList<>();
        // Сортировка по дате начала (от самой новой к самой старой)
        String selectQuery = "SELECT * FROM " + TABLE_TRIPS + " ORDER BY " + COLUMN_TRIP_START_DATETIME + " DESC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Trip trip = new Trip();
                trip.setId(cursor.getInt(0));
                trip.setName(cursor.getString(1));
                trip.setCarId(cursor.getInt(2));
                trip.setStartDateTime(cursor.getString(3));
                trip.setEndDateTime(cursor.getString(4));
                trip.setDistance(cursor.getDouble(5));
                trip.setFuelSpent(cursor.getDouble(6));
                trip.setFuelConsumption(cursor.getDouble(7));
                tripList.add(trip);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return tripList;
    }

    // Вам могут понадобиться другие методы (получение поездок по car_id, по дате и т.д.)
}