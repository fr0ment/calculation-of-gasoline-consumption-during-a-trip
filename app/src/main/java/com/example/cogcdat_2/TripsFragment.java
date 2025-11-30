package com.example.cogcdat_2;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.material.card.MaterialCardView;

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

    private Calendar startDateFilter = null;
    private Calendar endDateFilter = null;

    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_TIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat HEADER_DATE_FORMAT = new SimpleDateFormat("LLLL yyyy", new Locale("ru", "RU"));


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trips, container, false);

        dbHelper = new DatabaseHelper(getContext());

        recyclerView = view.findViewById(R.id.recycler_view_trips);
        fabAddTrip = view.findViewById(R.id.fab_add_trip);
        carSelectionSpinner = view.findViewById(R.id.spinner_car_selection);
        tvEmptyTrips = view.findViewById(R.id.tv_empty_trips);
        btnFilter = view.findViewById(R.id.btn_filter_date);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fabAddTrip.setOnClickListener(v -> showAddTripOptionsDialog());

        setupCarSpinner();
        setupFilterButton();

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
            Toast.makeText(getContext(), "Пожалуйста, выберите автомобиль.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_trip_options, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            // Установка прозрачного фона для отображения закругленных углов в XML
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialCardView cardRecordGps = dialogView.findViewById(R.id.card_record_gps);
        MaterialCardView cardAddManual = dialogView.findViewById(R.id.card_add_manual);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close_dialog);


        cardRecordGps.setOnClickListener(v -> {
            dialog.dismiss();
            startGpsRecordingActivity();
        });

        cardAddManual.setOnClickListener(v -> {
            dialog.dismiss();
            startManualTripActivity();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Предотвращение закрытия по тапу вне диалога (хотя с кнопкой закрытия это менее критично)
        dialog.setCanceledOnTouchOutside(true);

        dialog.show();
    }

    private void startGpsRecordingActivity() {
        Intent intent = new Intent(getActivity(), GpsRecordingActivity.class);
        intent.putExtra("car_id", selectedCarId);

        // Передача единиц измерения для UI GPS Activity
        Car car = carList.stream().filter(c -> c.getId() == selectedCarId).findFirst().orElse(null);
        if (car != null) {
            intent.putExtra("car_fuel_unit", car.getFuelUnit());
            intent.putExtra("car_distance_unit", car.getDistanceUnit());
        }
        startActivity(intent);
    }

    private void startManualTripActivity() {
        Intent intent = new Intent(getActivity(), AddTripManualActivity.class);
        intent.putExtra("car_id", selectedCarId);
        startActivityForResult(intent, 1); // Используем startActivityForResult, чтобы обновить список
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == AppCompatActivity.RESULT_OK) {
            loadCarsAndTrips(); // Обновляем список, если ручное добавление прошло успешно
        }
    }

    // --- Методы setupCarSpinner, loadCarsAndTrips, filterTrips, showDateFilterDialog, showDeleteConfirmation, TripAdapter ---
    // (Их содержимое должно быть в файле TripsFragment.java, код ниже для полноты)

    private void setupCarSpinner() {
        carList = dbHelper.getAllCars();

        // Добавляем заглушку "Выберите автомобиль"
        Car placeholderCar = new Car(-1, "Выберите автомобиль", "", null, "км", "л", "л/100км", "", 0);
        List<Car> spinnerList = new ArrayList<>(carList);
        if (carList.isEmpty()) {
            spinnerList.add(placeholderCar);
        } else {
            spinnerList.add(0, placeholderCar);
        }

        CustomCarSpinnerAdapter adapter = new CustomCarSpinnerAdapter(requireContext(), spinnerList);
        carSelectionSpinner.setAdapter(adapter);

        if (!carList.isEmpty()) {
            carSelectionSpinner.setSelection(1);
            selectedCarId = carList.get(0).getId();
        } else {
            carSelectionSpinner.setSelection(0);
            selectedCarId = -1;
        }

        carSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0 && spinnerList.get(position).getId() == -1) {
                    selectedCarId = -1;
                    tripListItems.clear();
                    if (adapter != null) adapter.notifyDataSetChanged();
                    tvEmptyTrips.setVisibility(View.VISIBLE);
                    fabAddTrip.setVisibility(View.GONE);
                } else {
                    Car selectedCar = spinnerList.get(position);
                    selectedCarId = selectedCar.getId();
                    loadTripsForSelectedCar();
                    fabAddTrip.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCarId = -1;
            }
        });
    }

    private void setupFilterButton() {
        btnFilter.setOnClickListener(v -> showDateFilterDialog());
    }

    private void loadCarsAndTrips() {
        carList = dbHelper.getAllCars();
        // Обновляем спиннер, сохраняя текущий выбор
        int oldCarId = selectedCarId;
        setupCarSpinner();

        // Попытка восстановить предыдущий выбор
        int positionToSelect = -1;
        for (int i = 0; i < carList.size(); i++) {
            if (carList.get(i).getId() == oldCarId) {
                positionToSelect = i + 1; // +1 из-за placeholder
                break;
            }
        }

        if (positionToSelect != -1) {
            carSelectionSpinner.setSelection(positionToSelect);
            selectedCarId = oldCarId;
            loadTripsForSelectedCar();
        } else if (!carList.isEmpty()) {
            carSelectionSpinner.setSelection(1);
            selectedCarId = carList.get(0).getId();
            loadTripsForSelectedCar();
        } else {
            selectedCarId = -1;
            loadTripsForSelectedCar();
        }
    }


    private void loadTripsForSelectedCar() {
        if (selectedCarId == -1) {
            tripListItems.clear();
            recyclerView.setVisibility(View.GONE);
            tvEmptyTrips.setVisibility(View.VISIBLE);
            return;
        }

        List<Trip> trips = dbHelper.getTripsForCar(selectedCarId);
        List<Trip> filteredTrips = filterTrips(trips);

        if (filteredTrips.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyTrips.setText("Для выбранного автомобиля нет поездок.");
            tvEmptyTrips.setVisibility(View.VISIBLE);
            tripListItems.clear();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            return;
        }

        tvEmptyTrips.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        // Формирование списка с заголовками
        tripListItems.clear();
        String currentHeader = "";
        for (Trip trip : filteredTrips) {
            try {
                // Используем StartDateTime для группировки
                Date tripDate = DB_DATE_FORMAT.parse(trip.getStartDateTime());
                String header = HEADER_DATE_FORMAT.format(tripDate);

                if (!header.equals(currentHeader)) {
                    tripListItems.add(new TripListItem(header));
                    currentHeader = header;
                }
                tripListItems.add(new TripListItem(trip));
            } catch (ParseException e) {
                tripListItems.add(new TripListItem(trip));
            }
        }

        if (adapter == null) {
            adapter = new TripAdapter(tripListItems);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateTrips(tripListItems);
        }
    }

    private List<Trip> filterTrips(List<Trip> trips) {
        if (startDateFilter == null && endDateFilter == null) {
            return trips;
        }

        return trips.stream().filter(trip -> {
            try {
                Date tripDate = DB_DATE_FORMAT.parse(trip.getStartDateTime());
                Calendar tripCal = Calendar.getInstance();
                tripCal.setTime(tripDate);

                boolean isAfterStart = true;
                if (startDateFilter != null) {
                    Calendar start = (Calendar) startDateFilter.clone();
                    start.set(Calendar.HOUR_OF_DAY, 0);
                    start.set(Calendar.MINUTE, 0);
                    start.set(Calendar.SECOND, 0);
                    start.set(Calendar.MILLISECOND, 0);
                    isAfterStart = !tripCal.before(start);
                }

                boolean isBeforeEnd = true;
                if (endDateFilter != null) {
                    Calendar end = (Calendar) endDateFilter.clone();
                    end.set(Calendar.HOUR_OF_DAY, 23);
                    end.set(Calendar.MINUTE, 59);
                    end.set(Calendar.SECOND, 59);
                    end.set(Calendar.MILLISECOND, 999);
                    isBeforeEnd = !tripCal.after(end);
                }

                return isAfterStart && isBeforeEnd;

            } catch (ParseException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }

    private void showDateFilterDialog() {
        // Логика отображения диалога фильтрации по дате (без изменений)
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_date_filter, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        final TextView tvStartDate = dialogView.findViewById(R.id.tv_start_date);
        final TextView tvEndDate = dialogView.findViewById(R.id.tv_end_date);
        Button btnApply = dialogView.findViewById(R.id.btn_apply_filter);
        Button btnClear = dialogView.findViewById(R.id.btn_clear_filter);

        final SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        // Установка начальных значений
        if (startDateFilter != null) {
            tvStartDate.setText(displayFormat.format(startDateFilter.getTime()));
        }
        if (endDateFilter != null) {
            tvEndDate.setText(displayFormat.format(endDateFilter.getTime()));
        }

        // Слушатель для выбора начальной даты
        tvStartDate.setOnClickListener(v -> {
            Calendar initial = startDateFilter != null ? startDateFilter : Calendar.getInstance();
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, month, dayOfMonth);
                startDateFilter = newDate;
                tvStartDate.setText(displayFormat.format(startDateFilter.getTime()));
            }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Слушатель для выбора конечной даты
        tvEndDate.setOnClickListener(v -> {
            Calendar initial = endDateFilter != null ? endDateFilter : Calendar.getInstance();
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, month, dayOfMonth);
                endDateFilter = newDate;
                tvEndDate.setText(displayFormat.format(endDateFilter.getTime()));
            }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Применить фильтр
        btnApply.setOnClickListener(v -> {
            loadTripsForSelectedCar();
            dialog.dismiss();
        });

        // Сбросить фильтр
        btnClear.setOnClickListener(v -> {
            startDateFilter = null;
            endDateFilter = null;
            loadTripsForSelectedCar();
            dialog.dismiss();
        });

        dialog.show();
    }


    private void showDeleteConfirmation(Trip trip) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить поездку")
                .setMessage("Вы уверены, что хотите удалить поездку \"" + trip.getName() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    dbHelper.deleteTrip(trip.getId());
                    Toast.makeText(getContext(), "Поездка удалена.", Toast.LENGTH_SHORT).show();
                    loadTripsForSelectedCar();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    /**
     * Адаптер для списка поездок с поддержкой заголовков.
     */
    private class TripAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<TripListItem> items;

        public TripAdapter(List<TripListItem> items) {
            this.items = items;
        }

        public void updateTrips(List<TripListItem> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
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
                tripHolder.tvDateTime.setText(String.format(Locale.getDefault(), "%s - %s",
                        formatDateTime(trip.getStartDateTime()), formatTime(trip.getEndDateTime())));
                tripHolder.tvDistance.setText(String.format(Locale.getDefault(), "%.1f %s", trip.getDistance(), distanceUnit));
                tripHolder.tvDuration.setText(formatDuration(trip.getStartDateTime(), trip.getEndDateTime()));
                tripHolder.tvFuelSpent.setText(String.format(Locale.getDefault(), "%.2f %s", trip.getFuelSpent(), fuelUnit));
                tripHolder.tvFuelConsumption.setText(String.format(Locale.getDefault(), "%.2f %s", trip.getFuelConsumption(), consumptionUnit));

                // Добавление Long Click Listener для удаления
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
                return DISPLAY_DATE_TIME_FORMAT.format(date);
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
                // Предполагается, что R.id.tv_header_title существует в item_trip_header.xml
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
}