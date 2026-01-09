package com.example.cogcdat_2;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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

public class EditTripActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private int tripId;
    private Trip trip;

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
        tripId = getIntent().getIntExtra("trip_id", -1);

        if (tripId == -1) {
            Toast.makeText(this, "Ошибка: Поездка не выбрана.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Загружаем поездку
        trip = getTripById(tripId);
        if (trip == null) {
            Toast.makeText(this, "Поездка не найдена.", Toast.LENGTH_LONG).show();
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

    private Trip getTripById(int id) {
        // Обходной путь, как в TripDetailsActivity
        for (Car car : dbHelper.getAllCars()) {
            for (Trip t : dbHelper.getTripsForCar(car.getId())) {
                if (t.getId() == id) {
                    return t;
                }
            }
        }
        return null;
    }

    private void loadTripData() {
        etTripName.setText(trip.getName());
        etDistance.setText(String.format(Locale.getDefault(), "%.1f", trip.getDistance()));
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
                .setTitleText(isStart ? "Дата начала" : "Дата окончания")
                .setSelection(selection)
                .build();

        datePicker.addOnPositiveButtonClickListener(selectionMillis -> {
            calendar.setTimeInMillis(selectionMillis);
            // Приводим к началу дня (очень рекомендуется!)
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            updateDateTimeButtons();

            // Проверка логики начала/окончания
            if (!isStart && endDateTime.before(startDateTime)) {
                Toast.makeText(this, "Дата конца не может быть раньше начала", Toast.LENGTH_SHORT).show();
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
                .setTitleText(isStart ? "Время начала" : "Время окончания")
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            calendar.set(Calendar.MINUTE, timePicker.getMinute());

            updateDateTimeButtons();

            // Проверка конца ≥ начала
            if (!isStart && endDateTime.before(startDateTime)) {
                Toast.makeText(this, "Время конца не может быть раньше начала", Toast.LENGTH_SHORT).show();
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

    // --- СОХРАНЕНИЕ И ВАЛИДАЦИЯ ---

    private void saveTrip() {
        // 1. Считывание и валидация обязательного поля (Название)
        String name = etTripName.getText().toString().trim();
        if (name.isEmpty()) {
            etTripName.setError("Название поездки является обязательным полем!");
            Toast.makeText(this, "Пожалуйста, введите название поездки.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Считывание и валидация числовых полей
        double distance = 0.0;
        double fuelSpent = 0.0;

        try {
            String distanceStr = etDistance.getText().toString();
            if (!distanceStr.isEmpty()) {
                distance = Double.parseDouble(distanceStr);
            }
        } catch (NumberFormatException e) {
            etDistance.setError("Некорректное значение расстояния.");
            return;
        }

        try {
            String fuelSpentStr = etFuelSpent.getText().toString();
            if (!fuelSpentStr.isEmpty()) {
                fuelSpent = Double.parseDouble(fuelSpentStr);
            }
        } catch (NumberFormatException e) {
            etFuelSpent.setError("Некорректное значение топлива.");
            return;
        }

        // Дополнительная валидация данных
        if (distance <= 0) {
            etDistance.setError("Расстояние должно быть больше 0.");
            return;
        }
        if (endDateTime.before(startDateTime)) {
            Toast.makeText(this, "Время окончания не может быть раньше времени начала.", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Расчет расхода топлива
        double fuelConsumption = 0.0;
        if (distance > 0 && fuelSpent > 0) {
            fuelConsumption = (fuelSpent / distance) * 100;
        }

        // 4. Форматирование дат
        String startDateTimeStr = DB_DATE_FORMAT.format(startDateTime.getTime());
        String endDateTimeStr = DB_DATE_FORMAT.format(endDateTime.getTime());

        // 5. Обновление объекта Trip
        trip.setName(name);
        trip.setStartDateTime(startDateTimeStr);
        trip.setEndDateTime(endDateTimeStr);
        trip.setDistance(distance);
        trip.setFuelSpent(fuelSpent);
        trip.setFuelConsumption(fuelConsumption);

        boolean success = dbHelper.updateTrip(trip);

        if (success) {
            Toast.makeText(this, "Поездка успешно обновлена!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Ошибка при обновлении поездки.", Toast.LENGTH_SHORT).show();
        }
    }
}
