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
    private static final int DATABASE_VERSION = 2;

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

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CARS);
        onCreate(db);
    }

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
}