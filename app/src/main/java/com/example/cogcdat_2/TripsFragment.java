package com.example.cogcdat_2;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private Spinner carSelectionSpinner;
    private RecyclerView tripsRecyclerView;
    private FloatingActionButton fabAddTrip;
    private TextView tvAverageFuelConsumption;
    private TextView tvMonthlyMileage;
    private TextView tvEmptyTrips;

    private EditText etSearchQuery;
    private ImageButton btnFilterDate;

    private List<Car> carList;
    private Car selectedCar;
    private TripAdapter tripAdapter;

    private List<Trip> allTripsForSelectedCar = new ArrayList<>();
    private String currentSearchQuery = "";
    private Date startDateFilter = null;
    private Date endDateFilter = null;

    private static final int REQUEST_CODE_MANUAL_INPUT = 1;

    // Форматы даты/времени
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_ONLY_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    // ФОРМАТЫ ДЛЯ ГРУППИРОВКИ (LLLL для именительного падежа: "Ноябрь 2025")
    private static final SimpleDateFormat HEADER_MONTH_FORMAT = new SimpleDateFormat("LLLL yyyy", new Locale("ru", "RU"));
    private static final SimpleDateFormat TRIP_DAY_FORMAT = new SimpleDateFormat("dd, E", new Locale("ru", "RU"));


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trips, container, false);

        dbHelper = new DatabaseHelper(getContext());
        carSelectionSpinner = view.findViewById(R.id.spinner_car_selection);
        tripsRecyclerView = view.findViewById(R.id.recycler_view_trips);
        fabAddTrip = view.findViewById(R.id.fab_add_trip);
        tvAverageFuelConsumption = view.findViewById(R.id.tv_average_fuel_consumption);
        tvMonthlyMileage = view.findViewById(R.id.tv_monthly_mileage);
        tvEmptyTrips = view.findViewById(R.id.tv_empty_trips);
        etSearchQuery = view.findViewById(R.id.et_search_query);
        btnFilterDate = view.findViewById(R.id.btn_filter_date);


        tripsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tripAdapter = new TripAdapter(new ArrayList<>());
        tripsRecyclerView.setAdapter(tripAdapter);

        loadCarsIntoSpinner();

        carSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (carList != null && !carList.isEmpty()) {
                    selectedCar = carList.get(position);

                    // Проверяем, что выбран не фиктивный элемент "Нет авто"
                    if (selectedCar.getId() != -1) {
                        currentSearchQuery = "";
                        etSearchQuery.setText("");
                        startDateFilter = null;
                        endDateFilter = null;
                        updateTripsAndStats(selectedCar.getId());
                    } else {
                        // Если выбран фиктивный элемент, очищаем список
                        updateRecyclerView(new ArrayList<>());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        fabAddTrip.setOnClickListener(v -> showAddTripMenu());

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        btnFilterDate.setOnClickListener(v -> showDateFilterDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCarsIntoSpinner();
        if (selectedCar != null && selectedCar.getId() != -1) {
            updateTripsAndStats(selectedCar.getId());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_MANUAL_INPUT && resultCode == AppCompatActivity.RESULT_OK) {
            if (selectedCar != null && selectedCar.getId() != -1) {
                updateTripsAndStats(selectedCar.getId());
            }
        }
    }

    private void loadCarsIntoSpinner() {
        carList = dbHelper.getAllCars();

        if (carList.isEmpty()) {
            // Добавляем фиктивный объект Car для отображения сообщения в Spinner
            // ЭТА СТРОКА ТЕПЕРЬ РАБОТАЕТ
            carList.add(new Car(-1, "", "Нет доступных авто. Добавьте в разделе 'Машины'", null, null, null, null, null, 0, null, null, null, null, null, null));
            carSelectionSpinner.setEnabled(false);
            fabAddTrip.setEnabled(false);
            tvEmptyTrips.setVisibility(View.VISIBLE);
            tvEmptyTrips.setText("Нет доступных авто. Добавьте в разделе 'Машины'.");
        } else {
            carSelectionSpinner.setEnabled(true);
            fabAddTrip.setEnabled(true);
            tvEmptyTrips.setVisibility(View.GONE);
        }

        // --- ИСПОЛЬЗУЕМ КАСТОМНЫЙ АДАПТЕР ---
        CustomCarSpinnerAdapter adapter = new CustomCarSpinnerAdapter(
                getContext(),
                carList);
        carSelectionSpinner.setAdapter(adapter);

        // Восстановление/установка выбранного авто
        if (selectedCar == null && !carList.isEmpty() && carList.get(0).getId() != -1) {
            selectedCar = carList.get(0);
            updateTripsAndStats(selectedCar.getId());
        } else if (selectedCar != null && !carList.isEmpty()) {
            int position = -1;
            for (int i = 0; i < carList.size(); i++) {
                if (carList.get(i).getId() == selectedCar.getId()) {
                    position = i;
                    break;
                }
            }
            if (position != -1) {
                carSelectionSpinner.setSelection(position);
            }
        }
    }

    private void updateTripsAndStats(int carId) {
        List<Trip> allDBTrips = dbHelper.getAllTrips();
        allTripsForSelectedCar.clear();

        double totalDistance = 0;
        double totalFuelSpent = 0;
        double monthlyDistance = 0;
        long currentTime = System.currentTimeMillis();
        long thirtyDaysAgo = currentTime - (30L * 24 * 60 * 60 * 1000);

        for (Trip trip : allDBTrips) {
            if (trip.getCarId() == carId) {
                allTripsForSelectedCar.add(trip);
                totalDistance += trip.getDistance();
                totalFuelSpent += trip.getFuelSpent();

                try {
                    Date startDate = DB_DATE_FORMAT.parse(trip.getStartDateTime());
                    if (startDate.getTime() >= thirtyDaysAgo) {
                        monthlyDistance += trip.getDistance();
                    }
                } catch (ParseException e) {
                    // Обработка невалидной даты
                }
            }
        }

        String distanceUnit = selectedCar != null ? selectedCar.getDistanceUnit() : "км";
        String consumptionUnit = selectedCar != null ? selectedCar.getFuelConsumptionUnit() : "л/100км";

        if (totalDistance > 0) {
            double averageConsumption = (totalFuelSpent / totalDistance) * 100;
            tvAverageFuelConsumption.setText(String.format(Locale.getDefault(), "Средний расход: %.2f %s", averageConsumption, consumptionUnit));
        } else {
            tvAverageFuelConsumption.setText("Средний расход: --");
        }
        tvMonthlyMileage.setText(String.format(Locale.getDefault(), "Пробег за месяц: %.1f %s", monthlyDistance, distanceUnit));

        applyFilters();
    }

    private void applyFilters() {
        if (allTripsForSelectedCar.isEmpty()) {
            updateRecyclerView(new ArrayList<>());
            return;
        }

        List<Trip> rawFilteredList = allTripsForSelectedCar.stream()
                .filter(trip -> filterByName(trip, currentSearchQuery))
                .filter(trip -> filterByDate(trip, startDateFilter, endDateFilter))
                .collect(Collectors.toList());

        // --- ГРУППИРОВКА И ДОБАВЛЕНИЕ ЗАГОЛОВКОВ ---
        List<TripListItem> finalGroupedList = new ArrayList<>();
        String currentHeader = "";

        // Сортируем список по дате перед группировкой (если БД не гарантирует сортировку)
        rawFilteredList.sort((t1, t2) -> {
            try {
                Date d1 = DB_DATE_FORMAT.parse(t1.getStartDateTime());
                Date d2 = DB_DATE_FORMAT.parse(t2.getStartDateTime());
                return d2.compareTo(d1); // Сортируем от новой к старой
            } catch (ParseException e) {
                return 0;
            }
        });

        for (Trip trip : rawFilteredList) {
            try {
                Date startDate = DB_DATE_FORMAT.parse(trip.getStartDateTime());
                String monthHeader = HEADER_MONTH_FORMAT.format(startDate);

                if (!monthHeader.equals(currentHeader)) {
                    String capitalizedHeader = capitalizeFirstLetter(monthHeader);
                    finalGroupedList.add(new TripListItem(capitalizedHeader));
                    currentHeader = monthHeader;
                }

                finalGroupedList.add(new TripListItem(trip));

            } catch (ParseException e) {
                finalGroupedList.add(new TripListItem(trip));
            }
        }

        updateRecyclerView(finalGroupedList);
    }

    private String capitalizeFirstLetter(String original) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        return original.substring(0, 1).toUpperCase(new Locale("ru", "RU")) + original.substring(1).toLowerCase(new Locale("ru", "RU"));
    }

    private void updateRecyclerView(List<TripListItem> list) {
        if (list.isEmpty() && allTripsForSelectedCar.isEmpty()) {
            tripsRecyclerView.setVisibility(View.GONE);
            tvEmptyTrips.setVisibility(View.VISIBLE);
            tvEmptyTrips.setText("Для выбранного автомобиля нет поездок.");
        } else if (list.isEmpty() && !allTripsForSelectedCar.isEmpty()) {
            tripsRecyclerView.setVisibility(View.GONE);
            tvEmptyTrips.setVisibility(View.VISIBLE);
            tvEmptyTrips.setText("Поездок, соответствующих фильтру, не найдено.");
        }
        else {
            tripsRecyclerView.setVisibility(View.VISIBLE);
            tvEmptyTrips.setVisibility(View.GONE);
            tripAdapter.updateList(list);
            tripAdapter.notifyDataSetChanged();
        }
    }

    private boolean filterByName(Trip trip, String query) {
        if (query.isEmpty()) return true;
        return trip.getName() != null && trip.getName().toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()));
    }

    private boolean filterByDate(Trip trip, Date start, Date end) {
        if (start == null && end == null) return true;

        try {
            Date tripDate = DB_DATE_FORMAT.parse(trip.getStartDateTime());

            boolean afterStart = (start == null) || (tripDate.after(start) || isSameDay(tripDate, start));

            Calendar endCal = Calendar.getInstance();
            if (end != null) {
                endCal.setTime(end);
                endCal.set(Calendar.HOUR_OF_DAY, 23);
                endCal.set(Calendar.MINUTE, 59);
                endCal.set(Calendar.SECOND, 59);
            }

            boolean beforeEnd = (end == null) || tripDate.before(endCal.getTime());

            return afterStart && beforeEnd;
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void showDateFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_date_filter, null);
        builder.setView(dialogView);

        TextView tvStartDate = dialogView.findViewById(R.id.tv_start_date);
        TextView tvEndDate = dialogView.findViewById(R.id.tv_end_date);
        Button btnApply = dialogView.findViewById(R.id.btn_apply_filter);
        Button btnClear = dialogView.findViewById(R.id.btn_clear_filter);

        if (startDateFilter != null) {
            tvStartDate.setText(DISPLAY_DATE_ONLY_FORMAT.format(startDateFilter));
        } else {
            tvStartDate.setText("Выберите дату");
        }
        if (endDateFilter != null) {
            tvEndDate.setText(DISPLAY_DATE_ONLY_FORMAT.format(endDateFilter));
        } else {
            tvEndDate.setText("Выберите дату");
        }

        tvStartDate.setOnClickListener(v -> showDatePicker(tvStartDate, true));
        tvEndDate.setOnClickListener(v -> showDatePicker(tvEndDate, false));

        final AlertDialog dialog = builder.create();

        btnApply.setOnClickListener(v -> {
            if (!validateDates()) {
                Toast.makeText(getContext(), "Начальная дата не может быть позже конечной.", Toast.LENGTH_SHORT).show();
                return;
            }
            applyFilters();
            dialog.dismiss();
        });

        btnClear.setOnClickListener(v -> {
            startDateFilter = null;
            endDateFilter = null;
            tvStartDate.setText("Выберите дату");
            tvEndDate.setText("Выберите дату");
            applyFilters();
            dialog.dismiss();
            Toast.makeText(getContext(), "Фильтр по дате сброшен.", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showDatePicker(final TextView textView, final boolean isStartDate) {
        final Calendar calendar = Calendar.getInstance();
        if (isStartDate && startDateFilter != null) {
            calendar.setTime(startDateFilter);
        } else if (!isStartDate && endDateFilter != null) {
            calendar.setTime(endDateFilter);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth, 0, 0, 0);
                    Date selectedDate = calendar.getTime();

                    if (isStartDate) {
                        startDateFilter = selectedDate;
                    } else {
                        endDateFilter = selectedDate;
                    }
                    textView.setText(DISPLAY_DATE_ONLY_FORMAT.format(selectedDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private boolean validateDates() {
        if (startDateFilter != null && endDateFilter != null) {
            return !startDateFilter.after(endDateFilter);
        }
        return true;
    }


    private void showAddTripMenu() {
        if (selectedCar == null || selectedCar.getId() == -1) {
            Toast.makeText(getContext(), "Сначала добавьте и выберите автомобиль!", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] options = {"Ручной ввод", "GPS запись"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Выберите способ добавления поездки");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent intent = new Intent(getActivity(), AddTripManualActivity.class);
                intent.putExtra("CAR_ID", selectedCar.getId());
                startActivityForResult(intent, REQUEST_CODE_MANUAL_INPUT);
            } else if (which == 1) {
                // ИЗМЕНЕНО: Запускаем активность для GPS записи
                Intent intent = new Intent(getActivity(), GpsRecordingActivity.class);
                intent.putExtra("CAR_ID", selectedCar.getId());
                startActivity(intent); // Используем startActivity, так как данные вернутся через БД
            }
        });
        builder.show();
    }

    // --- АДАПТЕР для работы с заголовками и поездками ---
    private class TripAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<TripListItem> items;

        public TripAdapter(List<TripListItem> items) {
            this.items = items;
        }

        public void updateList(List<TripListItem> newItems) {
            this.items = newItems;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).getType();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TripListItem.TYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_trip_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_trip, parent, false);
                return new TripViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TripListItem item = items.get(position);

            if (item.getType() == TripListItem.TYPE_HEADER) {
                HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
                headerHolder.tvHeader.setText(item.getHeader());

            } else if (item.getType() == TripListItem.TYPE_TRIP) {
                TripViewHolder tripHolder = (TripViewHolder) holder;
                Trip trip = item.getTrip();

                tripHolder.tvName.setText(trip.getName());

                String tripDayText = formatTripDay(trip.getStartDateTime());
                tripHolder.tvDateTime.setText(tripDayText);

                String distanceUnit = selectedCar != null ? selectedCar.getDistanceUnit() : "км";
                String fuelUnit = selectedCar != null ? selectedCar.getFuelUnit() : "л"; // Получаем единицу топлива
                String consumptionUnit = selectedCar != null ? selectedCar.getFuelConsumptionUnit() : "л/100км";

                tripHolder.tvDistance.setText(String.format(Locale.getDefault(), "%.1f %s", trip.getDistance(), distanceUnit));

                // ДОБАВЛЕНО: Отображение потраченного топлива
                tripHolder.tvFuelSpent.setText(String.format(Locale.getDefault(), "%.1f %s", trip.getFuelSpent(), fuelUnit));

                tripHolder.tvFuelConsumption.setText(String.format(Locale.getDefault(), "%.2f %s", trip.getFuelConsumption(), consumptionUnit));
                tripHolder.tvDuration.setText(calculateDuration(trip.getStartDateTime(), trip.getEndDateTime()));

                tripHolder.itemView.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "Детали поездки: " + trip.getName() + " (TODO)", Toast.LENGTH_SHORT).show();
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private String formatTripDay(String dbDateTime) {
            try {
                Date date = DB_DATE_FORMAT.parse(dbDateTime);
                return TRIP_DAY_FORMAT.format(date);
            } catch (ParseException | NullPointerException e) {
                return "--";
            }
        }

        private String calculateDuration(String start, String end) {
            try {
                Date startDate = DB_DATE_FORMAT.parse(start);
                Date endDate = DB_DATE_FORMAT.parse(end);

                long duration = endDate.getTime() - startDate.getTime();

                long hours = TimeUnit.MILLISECONDS.toHours(duration);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;

                return String.format(Locale.getDefault(), "%d ч %02d мин", hours, minutes);

            } catch (ParseException | NullPointerException e) {
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

                // ДОБАВЛЕНО: Инициализация нового TextView
                tvFuelSpent = itemView.findViewById(R.id.tv_trip_fuel_spent);

                tvFuelConsumption = itemView.findViewById(R.id.tv_trip_fuel_consumption);
            }
        }
    }
}