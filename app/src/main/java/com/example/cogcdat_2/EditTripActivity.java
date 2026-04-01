package com.example.cogcdat_2;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditTripActivity extends BaseActivity {

    private DatabaseHelper dbHelper;
    private String tripId;
    private Trip trip;
    private String currentUserId;
    private UserSettings userSettings;

    // Поля ввода
    private EditText etTripName;
    private EditText etDistance;
    private EditText etFuelSpent;
    private Button btnSelectStartDate;
    private Button btnSelectStartTime;
    private Button btnSelectEndDate;
    private Button btnSelectEndTime;
    private Button btnSaveTrip;
    private Button btnBack;

    // Календари для хранения выбранной даты и времени
    private Calendar startDateTime = Calendar.getInstance();
    private Calendar endDateTime = Calendar.getInstance();

    // Формат даты/времени для БД
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    // Формат для отображения в кнопках
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_trip);

        dbHelper = new DatabaseHelper(this);
        tripId = getIntent().getStringExtra("trip_id");

        // Получаем ID текущего пользователя и его настройки
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", null);

        if (currentUserId != null) {
            userSettings = dbHelper.getUserSettings(currentUserId);
        } else {
            userSettings = new UserSettings(); // Настройки по умолчанию
        }

        if (tripId == null || tripId.isEmpty()) {
            Toast.makeText(this, R.string.error_trip_not_selected, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Загружаем поездку
        trip = getTripById(tripId);
        if (trip == null) {
            Toast.makeText(this, R.string.trip_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Инициализация полей
        etTripName = findViewById(R.id.et_trip_name);
        etDistance = findViewById(R.id.et_distance);
        etFuelSpent = findViewById(R.id.et_fuel_spent);
        btnSelectStartDate = findViewById(R.id.btn_select_start_date);
        btnSelectStartTime = findViewById(R.id.btn_select_start_time);
        btnSelectEndDate = findViewById(R.id.btn_select_end_date);
        btnSelectEndTime = findViewById(R.id.btn_select_end_time);
        btnSaveTrip = findViewById(R.id.btn_save_trip);
        btnBack = findViewById(R.id.btnBack);

        // Обновляем подсказку для поля расстояния
        updateDistanceHint();

        // Загружаем данные в поля
        loadTripData();

        // Установка слушателей
        btnSelectStartDate.setOnClickListener(v -> showDatePicker(startDateTime, true));
        btnSelectStartTime.setOnClickListener(v -> showTimePicker(startDateTime, true));
        btnSelectEndDate.setOnClickListener(v -> showDatePicker(endDateTime, false));
        btnSelectEndTime.setOnClickListener(v -> showTimePicker(endDateTime, false));

        btnSaveTrip.setOnClickListener(v -> saveTrip());
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Обновляет подсказку в поле расстояния в зависимости от единиц измерения
     */
    private void updateDistanceHint() {
        if (userSettings != null && userSettings.getDistanceUnit() == DistanceUnit.MI) {
            etDistance.setHint(getString(R.string.distance_hint_miles));
        } else {
            etDistance.setHint(getString(R.string.distance_hint_km));
        }
    }

    private Trip getTripById(String id) {
        // Обходной путь, как в TripDetailsActivity
        for (Car car : dbHelper.getAllCars()) {
            for (Trip t : dbHelper.getTripsForCar(car.getId())) {
                if (t.getId().equals(id)) {
                    return t;
                }
            }
        }
        return null;
    }

    private void loadTripData() {
        etTripName.setText(trip.getName());

        // Отображаем расстояние в единицах пользователя
        double displayDistance = trip.getDistanceInUnit(userSettings.getDistanceUnit());
        etDistance.setText(String.format(Locale.getDefault(), "%.1f", displayDistance));

        etFuelSpent.setText(String.format(Locale.getDefault(), "%.2f", trip.getFuelSpent()));

        // Парсим даты
        try {
            startDateTime.setTime(DB_DATE_FORMAT.parse(trip.getStartDateTime()));
            endDateTime.setTime(DB_DATE_FORMAT.parse(trip.getEndDateTime()));
        } catch (ParseException e) {
            // Используем текущую дату если ошибка
            startDateTime = Calendar.getInstance();
            endDateTime = Calendar.getInstance();
        }

        updateDateTimeButtons();
    }

    // --- ВЫБОР ДАТЫ/ВРЕМЕНИ ---

    private void showDatePicker(Calendar calendar, boolean isStart) {
        long selection = calendar.getTimeInMillis();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder
                .datePicker()
                .setTitleText(isStart ? getString(R.string.start_date) : getString(R.string.end_date))
                .setSelection(selection)
                .build();

        datePicker.addOnPositiveButtonClickListener(selectionMillis -> {
            calendar.setTimeInMillis(selectionMillis);
            // Приводим к началу дня
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            updateDateTimeButtons();

            // Проверка логики начала/окончания
            if (!isStart && endDateTime.before(startDateTime)) {
                Toast.makeText(this, getString(R.string.error_end_before_start_date), Toast.LENGTH_SHORT).show();
                endDateTime.setTime(startDateTime.getTime());
                updateDateTimeButtons();
            }
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void showTimePicker(Calendar calendar, boolean isStart) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText(isStart ? getString(R.string.start_time) : getString(R.string.end_time))
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            calendar.set(Calendar.MINUTE, timePicker.getMinute());

            updateDateTimeButtons();

            // Проверка конца ≥ начала
            if (!isStart && endDateTime.before(startDateTime)) {
                Toast.makeText(this, getString(R.string.error_end_before_start_time), Toast.LENGTH_SHORT).show();
                endDateTime.setTime(startDateTime.getTime());
                updateDateTimeButtons();
            }
        });

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    private void updateDateTimeButtons() {
        btnSelectStartDate.setText(DISPLAY_DATE_FORMAT.format(startDateTime.getTime()));
        btnSelectStartTime.setText(DISPLAY_TIME_FORMAT.format(startDateTime.getTime()));
        btnSelectEndDate.setText(DISPLAY_DATE_FORMAT.format(endDateTime.getTime()));
        btnSelectEndTime.setText(DISPLAY_TIME_FORMAT.format(endDateTime.getTime()));
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---

    private double parseDoubleWithComma(String input) throws NumberFormatException {
        if (input == null || input.trim().isEmpty()) {
            return 0.0;
        }
        String normalized = input.trim().replace(',', '.');
        return Double.parseDouble(normalized);
    }

    // --- СОХРАНЕНИЕ И ВАЛИДАЦИЯ ---

    private void saveTrip() {
        // 1. Считывание и валидация обязательного поля (Название)
        String name = etTripName.getText().toString().trim();
        if (name.isEmpty()) {
            etTripName.setError(getString(R.string.error_trip_name_required));
            Toast.makeText(this, R.string.error_please_enter_trip_name, Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Считывание и валидация числовых полей
        double userDistance = 0.0;
        double fuelSpent = 0.0;

        try {
            String distanceStr = etDistance.getText().toString();
            if (!distanceStr.isEmpty()) {
                userDistance = parseDoubleWithComma(distanceStr);
            }
        } catch (NumberFormatException e) {
            etDistance.setError(getString(R.string.error_invalid_distance));
            return;
        }

        try {
            String fuelSpentStr = etFuelSpent.getText().toString();
            if (!fuelSpentStr.isEmpty()) {
                fuelSpent = parseDoubleWithComma(fuelSpentStr);
            }
        } catch (NumberFormatException e) {
            etFuelSpent.setError(getString(R.string.error_invalid_fuel));
            return;
        }

        // Дополнительная валидация данных
        if (userDistance <= 0) {
            etDistance.setError(getString(R.string.error_distance_must_be_positive));
            return;
        }
        if (fuelSpent <= 0) {
            etFuelSpent.setError(getString(R.string.error_fuel_must_be_positive));
            return;
        }
        if (endDateTime.before(startDateTime)) {
            Toast.makeText(this, R.string.error_end_before_start, Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Конвертируем расстояние в километры для хранения
        double distanceInKm;
        if (userSettings.getDistanceUnit() == DistanceUnit.MI) {
            distanceInKm = userDistance * 1.60934; // мили -> км
        } else {
            distanceInKm = userDistance; // уже км
        }

        // 4. Форматирование дат
        String startDateTimeStr = DB_DATE_FORMAT.format(startDateTime.getTime());
        String endDateTimeStr = DB_DATE_FORMAT.format(endDateTime.getTime());

        // 5. Обновление объекта Trip (расход топлива не сохраняем)
        trip.setName(name);
        trip.setStartDateTime(startDateTimeStr);
        trip.setEndDateTime(endDateTimeStr);
        trip.setDistance(distanceInKm); // Всегда в километрах!
        trip.setFuelSpent(fuelSpent);
        // fuelConsumption не устанавливаем - вычисляется динамически

        boolean success = dbHelper.updateTrip(trip);

        if (success) {
            Toast.makeText(this, R.string.trip_updated_successfully, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, R.string.error_updating_trip, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем настройки при возврате на экран
        if (currentUserId != null) {
            userSettings = dbHelper.getUserSettings(currentUserId);
            updateDistanceHint();

            // Обновляем отображаемое расстояние с новыми единицами
            if (trip != null) {
                double displayDistance = trip.getDistanceInUnit(userSettings.getDistanceUnit());
                etDistance.setText(String.format(Locale.getDefault(), "%.1f", displayDistance));
            }
        }
    }
}