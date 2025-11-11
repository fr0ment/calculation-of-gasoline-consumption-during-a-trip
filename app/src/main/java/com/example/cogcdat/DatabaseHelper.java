package com.example.cogcdat;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static String DB_NAME = "fuel_calculator.db";
    private static String DB_PATH = "";
    private static final int DB_VERSION = 1;

    private SQLiteDatabase mDataBase;
    private final Context mContext;
    private boolean mNeedUpdate = false;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        if (android.os.Build.VERSION.SDK_INT >= 17)
            DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
        else
            DB_PATH = "/data/data/" + context.getPackageName() + "/databases/";
        this.mContext = context;

        copyDataBase();

        this.getReadableDatabase();
    }

    public void updateDataBase() throws IOException {
        if (mNeedUpdate) {
            File dbFile = new File(DB_PATH + DB_NAME);
            if (dbFile.exists())
                dbFile.delete();

            copyDataBase();

            mNeedUpdate = false;
        }
    }

    private boolean checkDataBase() {
        File dbFile = new File(DB_PATH + DB_NAME);
        return dbFile.exists();
    }

    private void copyDataBase() {
        if (!checkDataBase()) {
            this.getReadableDatabase();
            this.close();
            try {
                copyDBFile();
            } catch (IOException mIOException) {
                throw new Error("ErrorCopyingDataBase");
            }
        }
    }

    private void copyDBFile() throws IOException {
        InputStream mInput = mContext.getAssets().open(DB_NAME);
        OutputStream mOutput = new FileOutputStream(DB_PATH + DB_NAME);
        byte[] mBuffer = new byte[1024];
        int mLength;
        while ((mLength = mInput.read(mBuffer)) > 0)
            mOutput.write(mBuffer, 0, mLength);
        mOutput.flush();
        mOutput.close();
        mInput.close();
    }

    public boolean openDataBase() throws SQLException {
        mDataBase = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null, SQLiteDatabase.CREATE_IF_NECESSARY);
        return mDataBase != null;
    }

    @Override
    public synchronized void close() {
        if (mDataBase != null)
            mDataBase.close();
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Не создаем таблицу, т.к. используем готовую БД из assets
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion)
            mNeedUpdate = true;
    }

    // CRUD операции
    public void addCalculation(CalculationResult calculation) {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "INSERT INTO calculations (distance, fuel_consumption, fuel_price, fuel_needed, trip_cost) " +
                "VALUES (?, ?, ?, ?, ?)";

        db.execSQL(query, new Object[]{
                calculation.getDistance(),
                calculation.getFuelConsumption(),
                calculation.getFuelPrice(),
                calculation.getFuelNeeded(),
                calculation.getTripCost()
        });

        db.close();
    }

    public List<CalculationResult> getAllCalculations() {
        List<CalculationResult> calculations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM calculations ORDER BY id DESC", null);

        if (cursor.moveToFirst()) {
            do {
                // Создаем timestamp на основе ID (так как у вас нет поля timestamp в БД)
                String timestamp = "Запись #" + cursor.getInt(cursor.getColumnIndexOrThrow("id"));

                CalculationResult calculation = new CalculationResult(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("distance")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("fuel_consumption")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("fuel_price")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("fuel_needed")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("trip_cost")),
                        timestamp
                );
                calculations.add(calculation);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return calculations;
    }

    public void deleteCalculation(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM calculations WHERE id = ?", new Object[]{id});
        db.close();
    }

    public CalculationResult getCalculationById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        CalculationResult calculation = null;

        Cursor cursor = db.rawQuery("SELECT * FROM calculations WHERE id = ?", new String[]{String.valueOf(id)});

        if (cursor.moveToFirst()) {
            String timestamp = "Запись #" + cursor.getInt(cursor.getColumnIndexOrThrow("id"));

            calculation = new CalculationResult(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("distance")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("fuel_consumption")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("fuel_price")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("fuel_needed")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("trip_cost")),
                    timestamp
            );
        }
        cursor.close();
        db.close();
        return calculation;
    }
}