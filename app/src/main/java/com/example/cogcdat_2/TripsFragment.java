package com.example.cogcdat_2;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.card.MaterialCardView;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TripsFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private TripAdapter adapter;
    private FloatingActionButton fabAddTrip;
    private Spinner carSelectionSpinner;
    private TextView tvEmptyTrips;
    private ImageButton btnFilter;

    private List<Car> carList = new ArrayList<>();
    private List<TripListItem> tripListItems = new ArrayList<>();

    private int selectedCarId = -1;

    // NEW: Полные datetime фильтры (с временем)
    private Calendar startDateTimeFilter = null;
    private Calendar endDateTimeFilter = null;

    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_TIME_FORMAT = new SimpleDateFormat("EEE, dd HH:mm", new Locale("ru", "RU"));  // "Пн, 01 19:22"
    private static final SimpleDateFormat HEADER_DATE_FORMAT = new SimpleDateFormat("LLLL yyyy", new Locale("ru", "RU"));
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());  // Для отображения в диалоге

    // NEW: Для поиска
    private EditText etSearchQuery;
    private String currentSearchQuery = "";

    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trips, container, false);

        dbHelper = new DatabaseHelper(getContext());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        recyclerView = view.findViewById(R.id.recycler_view_trips);
        fabAddTrip = view.findViewById(R.id.fab_add_trip);
        carSelectionSpinner = view.findViewById(R.id.spinner_car_selection);
        tvEmptyTrips = view.findViewById(R.id.tv_empty_trips);
        btnFilter = view.findViewById(R.id.btn_filter_date);
        etSearchQuery = view.findViewById(R.id.et_search_query);  // Для поиска

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fabAddTrip.setOnClickListener(v -> showAddTripOptionsDialog());

        setupCarSpinner();
        setupSearchListener();  // Listener для поиска
        setupFilterButton();  // Улучшенный фильтр

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCarsAndTrips();
    }

    // --- Кастомный диалог выбора метода добавления поездки ---
    private void showAddTripOptionsDialog() {
        if (selectedCarId == -1) {
            Toast.makeText(getContext(), "Сначала выберите автомобиль", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Добавить поездку");
        String[] options = {"Автоматическая (GPS)", "Ручная"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // GPS запись
                Intent intent = new Intent(getContext(), GpsRecordingActivity.class);
                intent.putExtra("car_id", selectedCarId);
                startActivity(intent);
            } else {
                // Ручное добавление
                Intent intent = new Intent(getContext(), AddTripManualActivity.class);
                intent.putExtra("car_id", selectedCarId);
                startActivity(intent);
            }
        });
        builder.show();
    }

    // --- Настройка спиннера автомобилей ---
    private void setupCarSpinner() {
        carList = dbHelper.getAllCars();
        CustomCarSpinnerAdapter spinnerAdapter = new CustomCarSpinnerAdapter(requireContext(), carList);
        carSelectionSpinner.setAdapter(spinnerAdapter);

        // После установки адаптера, загрузите сохранённый ID и установите позицию в спиннере
        int savedCarId = sharedPreferences.getInt("selected_car_id", -1);  // -1 как default, если ничего не сохранено
        if (savedCarId != -1) {
            for (int i = 0; i < carList.size(); i++) {
                if (carList.get(i).getId() == savedCarId) {
                    carSelectionSpinner.setSelection(i);  // Устанавливаем позицию
                    selectedCarId = savedCarId;
                    break;
                }
            }
        } else if (!carList.isEmpty()) {
            // Если ничего не сохранено, выбираем первый по умолчанию
            carSelectionSpinner.setSelection(0);
            selectedCarId = carList.get(0).getId();
        }

// Затем вызовите loadTripsForCar() для загрузки поездок с выбранным ID

        carSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCarId = carList.get(position).getId();

                // Сохраняем выбранный ID в SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("selected_car_id", selectedCarId);
                editor.apply();  // Асинхронно сохраняем

                loadTripsForCar();  // Обновляем список поездок
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });
    }

    // --- Listener для поиска по названию ---
    private void setupSearchListener() {
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase();
                // FIX: Вызываем reload для применения поиска
                if (selectedCarId != -1) {
                    loadTripsForCar();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // --- Загрузка автомобилей и поездок ---
    private void loadCarsAndTrips() {
        setupCarSpinner();  // Reload cars
        if (selectedCarId != -1) {
            loadTripsForCar();  // Load trips for default car
        }
    }

    // --- Загрузка поездок для автомобиля (с применением всех фильтров) ---
    private void loadTripsForCar() {
        if (selectedCarId == -1) return;

        List<Trip> allTrips = dbHelper.getTripsForCar(selectedCarId);
        // FIX: Применяем все фильтры и группируем
        applyFiltersAndGroup(allTrips);
        // Рассчитываем статистику на основе нефильтрованных trips (или фильтрованных? — здесь нефильтрованных для общей статистики)
        updateCarStats(allTrips);
    }

    // NEW/FIX: Применение всех фильтров (поиск + дата) и группировка
    private void applyFiltersAndGroup(List<Trip> allTrips) {
        List<Trip> filteredTrips = new ArrayList<>(allTrips);

        // Фильтр по дате (полный datetime)
        if (startDateTimeFilter != null) {
            filteredTrips.removeIf(trip -> {
                try {
                    Date tripStart = DB_DATE_FORMAT.parse(trip.getStartDateTime());
                    return tripStart.before(startDateTimeFilter.getTime());
                } catch (ParseException e) {
                    return true;  // Удаляем некорректные
                }
            });
        }
        if (endDateTimeFilter != null) {
            filteredTrips.removeIf(trip -> {
                try {
                    Date tripStart = DB_DATE_FORMAT.parse(trip.getStartDateTime());
                    return tripStart.after(endDateTimeFilter.getTime());
                } catch (ParseException e) {
                    return true;
                }
            });
        }

        // Фильтр по поиску
        if (!currentSearchQuery.isEmpty()) {
            filteredTrips.removeIf(trip -> !trip.getName().toLowerCase().contains(currentSearchQuery));
        }

        // Группировка по месяцам (сортировка по startDateTime DESC)
        tripListItems.clear();
        List<Trip> sortedTrips = filteredTrips.stream()
                .sorted((t1, t2) -> {
                    try {
                        Date d1 = DB_DATE_FORMAT.parse(t1.getStartDateTime());
                        Date d2 = DB_DATE_FORMAT.parse(t2.getStartDateTime());
                        return d2.compareTo(d1);  // Новые сверху
                    } catch (ParseException e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());

        String currentMonth = "";
        for (Trip trip : sortedTrips) {
            try {
                Date date = DB_DATE_FORMAT.parse(trip.getStartDateTime());
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                String monthHeader = HEADER_DATE_FORMAT.format(date);

                if (!monthHeader.equals(currentMonth)) {
                    tripListItems.add(new TripListItem(monthHeader));
                    currentMonth = monthHeader;
                }
                tripListItems.add(new TripListItem(trip));
            } catch (ParseException e) {
                // Пропускаем некорректные даты
            }
        }

        // FIX: Recreate adapter для обновления (или notifyDataSetChanged())
        adapter = new TripAdapter(tripListItems);
        recyclerView.setAdapter(adapter);

        // Empty state
        boolean hasItems = !tripListItems.isEmpty();
        tvEmptyTrips.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        recyclerView.setVisibility(hasItems ? View.VISIBLE : View.GONE);
    }

    // --- Рассчёт статистики для выбранного автомобиля (на основе всех trips, без фильтров) ---
    private void updateCarStats(List<Trip> trips) {
        TextView tvAvgConsumption = requireView().findViewById(R.id.tv_average_fuel_consumption);
        TextView tvMonthlyMileage = requireView().findViewById(R.id.tv_monthly_mileage);

        if (trips.isEmpty()) {
            tvAvgConsumption.setText("Средний расход: -- л/100км");
            tvMonthlyMileage.setText("Пробег за месяц: -- км");
            return;
        }

        // Средний расход: среднее по всем trips
        double avgConsumption = trips.stream()
                .mapToDouble(Trip::getFuelConsumption)
                .average()
                .orElse(0.0);

        // Пробег за месяц: сумма distance за последние 30 дней (сравнение по startDateTime)
        Calendar monthAgo = Calendar.getInstance();
        monthAgo.add(Calendar.DAY_OF_MONTH, -30);
        Date monthAgoDate = monthAgo.getTime();

        double monthlyMileage = trips.stream()
                .filter(trip -> {
                    try {
                        Date start = DB_DATE_FORMAT.parse(trip.getStartDateTime());
                        return !start.before(monthAgoDate);
                    } catch (ParseException e) {
                        return false;
                    }
                })
                .mapToDouble(Trip::getDistance)
                .sum();

        tvAvgConsumption.setText(String.format(Locale.getDefault(), "Средний расход: %.2f л/100км", avgConsumption));
        tvMonthlyMileage.setText(String.format(Locale.getDefault(), "Пробег за месяц: %.1f км", monthlyMileage));
    }

    // --- Настройка кнопки фильтра ---
    private void setupFilterButton() {
        btnFilter.setOnClickListener(v -> showDateFilterDialog());
    }

    // NEW/FIX: Полностью переработанный диалог фильтра по дате (с временем)
    private void showDateFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_date_filter, null);
        builder.setView(dialogView);

        TextView tvStartDate = dialogView.findViewById(R.id.tv_start_date);
        TextView tvEndDate = dialogView.findViewById(R.id.tv_end_date);
        Button btnApply = dialogView.findViewById(R.id.btn_apply_filter);
        Button btnClear = dialogView.findViewById(R.id.btn_clear_filter);

        // Инициализация отображения
        if (startDateTimeFilter != null) {
            tvStartDate.setText(DISPLAY_DATE_FORMAT.format(startDateTimeFilter.getTime()));
        } else {
            tvStartDate.setText("Выберите дату и время (от)");
        }
        if (endDateTimeFilter != null) {
            tvEndDate.setText(DISPLAY_DATE_FORMAT.format(endDateTimeFilter.getTime()));
        } else {
            tvEndDate.setText("Выберите дату и время (до)");
        }

        // Date + Time Picker для start
        tvStartDate.setOnClickListener(v -> showDateTimePicker(true, tvStartDate));

        // Date + Time Picker для end
        tvEndDate.setOnClickListener(v -> showDateTimePicker(false, tvEndDate));

        AlertDialog dialog = builder.create();
        dialog.show();

        btnApply.setOnClickListener(v -> {
            if (startDateTimeFilter == null && endDateTimeFilter == null) {
                Toast.makeText(getContext(), "Выберите хотя бы одну дату", Toast.LENGTH_SHORT).show();
                return;
            }
            // FIX: Применяем фильтр через reload
            loadTripsForCar();
            dialog.dismiss();
            Toast.makeText(getContext(), "Фильтр применён", Toast.LENGTH_SHORT).show();
        });

        btnClear.setOnClickListener(v -> {
            startDateTimeFilter = null;
            endDateTimeFilter = null;
            tvStartDate.setText("Выберите дату и время (от)");
            tvEndDate.setText("Выберите дату и время (до)");
            loadTripsForCar();  // Reload без фильтров
            dialog.dismiss();
            Toast.makeText(getContext(), "Фильтр сброшен", Toast.LENGTH_SHORT).show();
        });
    }

    // NEW: Показ DatePicker + TimePicker
    private void showDateTimePicker(boolean isStart, final TextView tvDate) {
        Calendar current = Calendar.getInstance();
        if ((isStart && startDateTimeFilter != null) || (!isStart && endDateTimeFilter != null)) {
            current = isStart ? startDateTimeFilter : endDateTimeFilter;
        }

        Calendar finalCurrent = current;
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth, finalCurrent.get(Calendar.HOUR_OF_DAY), finalCurrent.get(Calendar.MINUTE));
                    showTimePicker(isStart, selected, tvDate);
                },
                current.get(Calendar.YEAR),
                current.get(Calendar.MONTH),
                current.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    // NEW: TimePicker после DatePicker
    private void showTimePicker(final boolean isStart, final Calendar selectedDate, final TextView tvDate) {
        TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDate.set(Calendar.MINUTE, minute);
                    selectedDate.set(Calendar.SECOND, 0);  // Секунды = 0

                    if (isStart) {
                        startDateTimeFilter = selectedDate;
                    } else {
                        endDateTimeFilter = selectedDate;
                    }
                    tvDate.setText(DISPLAY_DATE_FORMAT.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.HOUR_OF_DAY),
                selectedDate.get(Calendar.MINUTE),
                true  // 24-часовой формат
        );
        timePicker.show();
    }

    // --- Адаптер для RecyclerView (без изменений, формат даты уже фикс) ---
    private class TripAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<TripListItem> items;

        public TripAdapter(List<TripListItem> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).getType();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TripListItem.TYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
                return new TripViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TripListItem item = items.get(position);

            if (item.getType() == TripListItem.TYPE_HEADER) {
                HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
                headerHolder.tvHeader.setText(item.getHeader());
            } else {
                TripViewHolder tripHolder = (TripViewHolder) holder;
                Trip trip = item.getTrip();

                Car car = dbHelper.getCar(trip.getCarId());
                String distanceUnit = car != null ? car.getDistanceUnit() : "км";
                String fuelUnit = car != null ? car.getFuelUnit() : "л";
                String consumptionUnit = car != null ? car.getFuelConsumptionUnit() : "л/100км";

                tripHolder.tvName.setText(trip.getName());
                // Формат даты — только начало, "Пн, 01 19:22"
                tripHolder.tvDateTime.setText(formatDateTime(trip.getStartDateTime()));
                tripHolder.tvDistance.setText(String.format(Locale.getDefault(), "%.1f %s", trip.getDistance(), distanceUnit));
                tripHolder.tvFuelSpent.setText(String.format(Locale.getDefault(), "%.2f %s", trip.getFuelSpent(), fuelUnit));
                tripHolder.tvFuelConsumption.setText(String.format(Locale.getDefault(), "%.2f %s", trip.getFuelConsumption(), consumptionUnit));
                tripHolder.tvDuration.setText(formatDuration(trip.getStartDateTime(), trip.getEndDateTime()));

                // Long Click Listener для удаления
                tripHolder.itemView.setOnLongClickListener(v -> {
                    showDeleteConfirmation(trip);
                    return true;
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private String formatDateTime(String dbDateTime) {
            try {
                Date date = DB_DATE_FORMAT.parse(dbDateTime);
                return DISPLAY_DATE_TIME_FORMAT.format(date);  // "EEE, dd HH:mm"
            } catch (ParseException e) {
                return dbDateTime;
            }
        }

        private String formatTime(String dbDateTime) {
            try {
                Date date = DB_DATE_FORMAT.parse(dbDateTime);
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
            } catch (ParseException e) {
                return "--:--";
            }
        }

        /**
         * Рассчитывает время в пути.
         */
        private String formatDuration(String startDateTimeStr, String endDateTimeStr) {
            try {
                Date startDate = DB_DATE_FORMAT.parse(startDateTimeStr);
                Date endDate = DB_DATE_FORMAT.parse(endDateTimeStr);

                long duration = endDate.getTime() - startDate.getTime();

                long hours = TimeUnit.MILLISECONDS.toHours(duration);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;

                return String.format(Locale.getDefault(), "%d ч %02d мин", hours, minutes);

            } catch (Exception e) {
                return "--";
            }
        }

        public class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView tvHeader;

            public HeaderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvHeader = itemView.findViewById(R.id.tv_header_title);
            }
        }

        public class TripViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDateTime, tvDistance, tvDuration, tvFuelSpent, tvFuelConsumption;

            public TripViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_trip_name);
                tvDateTime = itemView.findViewById(R.id.tv_trip_date_time);
                tvDistance = itemView.findViewById(R.id.tv_trip_distance);
                tvDuration = itemView.findViewById(R.id.tv_trip_duration);
                tvFuelSpent = itemView.findViewById(R.id.tv_trip_fuel_spent);
                tvFuelConsumption = itemView.findViewById(R.id.tv_trip_fuel_consumption);
            }
        }
    }

    // --- Удаление поездки (FIX: Добавлен reload) ---
    private void showDeleteConfirmation(Trip trip) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить поездку?")
                .setMessage("Эта операция необратима.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    dbHelper.deleteTrip(trip.getId());
                    // FIX: Полный reload после удаления
                    loadTripsForCar();
                    Toast.makeText(getContext(), "Поездка удалена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}