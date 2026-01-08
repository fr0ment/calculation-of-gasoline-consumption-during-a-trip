package com.example.cogcdat_2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TripsFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private TripAdapter adapter;
    private FloatingActionButton fabAddTrip;
    private TextView tvEmptyTrips;
    private ImageButton btnFilterDate;

    // Новый UI для выбора автомобиля
    private View carSelectionView;
    private ImageView ivSelectedCarIcon;
    private TextView tvSelectedCarName;

    private List<Car> carList = new ArrayList<>();
    private List<TripListItem> tripListItems = new ArrayList<>();

    private int selectedCarId = -1;

    // Поиск
    private EditText etSearchQuery;
    private String currentSearchQuery = "";

    // Фильтры по дате (если используешь)
    private Calendar startDateTimeFilter = null;
    private Calendar endDateTimeFilter = null;

    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_TIME_FORMAT = new SimpleDateFormat("EEE, dd HH:mm", new Locale("ru", "RU"));
    private static final SimpleDateFormat HEADER_DATE_FORMAT = new SimpleDateFormat("LLLL yyyy", new Locale("ru", "RU"));

    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_trips, container, false);

        dbHelper = new DatabaseHelper(getContext());

        recyclerView = rootView.findViewById(R.id.recycler_view_trips);
        fabAddTrip = rootView.findViewById(R.id.fab_add_trip);
        tvEmptyTrips = rootView.findViewById(R.id.tv_empty_trips);
        btnFilterDate = rootView.findViewById(R.id.btn_filter_date);
        etSearchQuery = rootView.findViewById(R.id.et_search_query);

        // Новые элементы выбора автомобиля
        carSelectionView = rootView.findViewById(R.id.car_selection_view);
        ivSelectedCarIcon = rootView.findViewById(R.id.iv_selected_car_icon);
        tvSelectedCarName = rootView.findViewById(R.id.tv_selected_car_name);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fabAddTrip.setOnClickListener(v -> showAddTripOptionsDialog());

        setupCarSelection();
        setupSearchListener();
        setupFilterButton();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCarsAndTrips();
    }

    private void setupCarSelection() {
        carSelectionView.setOnClickListener(v -> showCarSelectionDialog());
    }

    private void loadCarsAndTrips() {
        carList = dbHelper.getAllCars();

        if (carList.isEmpty()) {
            carSelectionView.setVisibility(View.GONE);
            tvEmptyTrips.setText("Нет добавленных автомобилей");
            tvEmptyTrips.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        } else {
            carSelectionView.setVisibility(View.VISIBLE);
        }

        // Восстанавливаем сохранённый выбор автомобиля
        int savedCarId = SelectedCarManager.getSelectedCarId(requireContext());
        Car selectedCar = null;
        for (Car car : carList) {
            if (car.getId() == savedCarId) {
                selectedCar = car;
                break;
            }
        }

        // Если сохранённого нет — берём первый
        if (selectedCar == null && !carList.isEmpty()) {
            selectedCar = carList.get(0);
            SelectedCarManager.setSelectedCarId(requireContext(), selectedCar.getId());
        }

        selectedCarId = selectedCar != null ? selectedCar.getId() : -1;
        updateSelectedCarDisplay(selectedCar);

        // Загружаем поездки для выбранного автомобиля
        loadTripsForCar();
    }

    private void updateSelectedCarDisplay(Car car) {
        if (car == null) {
            tvSelectedCarName.setText("Выберите автомобиль");
            ivSelectedCarIcon.setImageResource(R.drawable.ic_car_outline);
        } else {
            tvSelectedCarName.setText(car.getName());

            String imagePath = car.getImagePath();
            if (imagePath != null && !imagePath.isEmpty() && new File(imagePath).exists()) {
                Glide.with(requireContext())
                        .load(new File(imagePath))
                        .circleCrop()
                        .placeholder(R.drawable.ic_car_outline)
                        .error(R.drawable.ic_car_outline)
                        .into(ivSelectedCarIcon);
            } else {
                ivSelectedCarIcon.setImageResource(R.drawable.ic_car_outline);
            }
        }
    }

    private void showCarSelectionDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_car_selection, null);
        RecyclerView recyclerViewDialog = dialogView.findViewById(R.id.recycler_view_cars);
        recyclerViewDialog.setLayoutManager(new LinearLayoutManager(requireContext()));

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        CarSelectionAdapter carAdapter = new CarSelectionAdapter(carList, car -> {
            selectedCarId = car.getId();
            SelectedCarManager.setSelectedCarId(requireContext(), selectedCarId);
            updateSelectedCarDisplay(car);
            loadTripsForCar(); // Перезагружаем поездки
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Подсвечиваем текущий выбранный автомобиль
        int selectedPos = -1;
        for (int i = 0; i < carList.size(); i++) {
            if (carList.get(i).getId() == selectedCarId) {
                selectedPos = i;
                break;
            }
        }
        carAdapter.setSelectedPosition(selectedPos);

        recyclerViewDialog.setAdapter(carAdapter);

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void loadTripsForCar() {
        tripListItems.clear();

        if (selectedCarId == -1) {
            updateEmptyState("Выберите автомобиль");
            return;
        }

        List<Trip> allTrips = dbHelper.getTripsForCar(selectedCarId);

        // Применяем поиск
        if (!currentSearchQuery.isEmpty()) {
            String query = currentSearchQuery.toLowerCase();
            allTrips = new ArrayList<>();
            for (Trip trip : dbHelper.getTripsForCar(selectedCarId)) {
                if (trip.getName() != null && trip.getName().toLowerCase().contains(query)) {
                    allTrips.add(trip);
                }
            }
        }

        // Здесь ты уже имел свою логику фильтрации и группировки
        applyFiltersAndGroup(allTrips);

        if (adapter == null) {
            adapter = new TripAdapter(tripListItems);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateItems(tripListItems);
        }

        if (tripListItems.isEmpty()) {
            updateEmptyState("Для выбранного автомобиля нет поездок.");
        } else {
            tvEmptyTrips.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void applyFiltersAndGroup(List<Trip> trips) {
        // Здесь твоя оригинальная логика группировки по месяцам и создания TripListItem
        // Я оставляю её как есть — ты её не прислал полностью, но предполагаю, что она у тебя есть
        // Пример (если нужно восстановить типичную реализацию):

        tripListItems.clear();

        if (trips.isEmpty()) {
            return;
        }

        // Сортируем по дате (от новых к старым)
        trips.sort((t1, t2) -> {
            try {
                Date d1 = DB_DATE_FORMAT.parse(t1.getStartDateTime());
                Date d2 = DB_DATE_FORMAT.parse(t2.getStartDateTime());
                return d2.compareTo(d1);
            } catch (ParseException e) {
                return 0;
            }
        });

        String currentHeader = null;
        for (Trip trip : trips) {
            try {
                Date date = DB_DATE_FORMAT.parse(trip.getStartDateTime());
                String header = HEADER_DATE_FORMAT.format(date); // "Ноябрь 2025"

                if (!header.equals(currentHeader)) {
                    tripListItems.add(new TripListItem(header));
                    currentHeader = header;
                }

                tripListItems.add(new TripListItem(trip));
            } catch (ParseException e) {
                // Если дата битая — просто добавляем без заголовка
                tripListItems.add(new TripListItem(trip));
            }
        }
    }

    private void updateEmptyState(String message) {
        tvEmptyTrips.setText(message);
        tvEmptyTrips.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void setupSearchListener() {
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().trim();
                loadTripsForCar();
            }
        });
    }

    private void setupFilterButton() {
        btnFilterDate.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Фильтр по дате (можно реализовать)", Toast.LENGTH_SHORT).show();
            // Здесь можно открыть DatePicker и применить фильтр
        });
    }

    private void showAddTripOptionsDialog() {
        if (selectedCarId == -1) {
            Toast.makeText(getContext(), "Сначала выберите автомобиль", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Добавить поездку")
                .setItems(new String[]{"Автоматическая (GPS)", "Ручная"}, (dialog, which) -> {
                    Intent intent;
                    if (which == 0) {
                        intent = new Intent(getContext(), GpsRecordingActivity.class);
                    } else {
                        intent = new Intent(getContext(), AddTripManualActivity.class);
                    }
                    intent.putExtra("car_id", selectedCarId);
                    startActivity(intent);
                })
                .show();
    }

    // Адаптер остаётся без изменений — использует TripListItem
    private class TripAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<TripListItem> items;

        TripAdapter(List<TripListItem> items) {
            this.items = items;
        }

        void updateItems(List<TripListItem> newItems) {
            items = newItems;
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
                ((HeaderViewHolder) holder).tvHeader.setText(item.getHeader());
            } else {
                TripViewHolder tripHolder = (TripViewHolder) holder;
                Trip trip = item.getTrip();

                Car car = dbHelper.getCar(trip.getCarId());
                String distanceUnit = car != null ? car.getDistanceUnit() : "км";
                String fuelUnit = car != null ? car.getFuelUnit() : "л";
                String consumptionUnit = car != null ? car.getFuelConsumptionUnit() : "л/100км";

                tripHolder.tvName.setText(trip.getName());
                tripHolder.tvDateTime.setText(formatDateTime(trip.getStartDateTime()));
                tripHolder.tvDistance.setText(String.format(Locale.getDefault(), "%.1f %s", trip.getDistance(), distanceUnit));
                tripHolder.tvFuelSpent.setText(String.format(Locale.getDefault(), "%.2f %s", trip.getFuelSpent(), fuelUnit));
                tripHolder.tvFuelConsumption.setText(String.format(Locale.getDefault(), "%.2f %s", trip.getFuelConsumption(), consumptionUnit));
                tripHolder.tvDuration.setText(formatDuration(trip.getStartDateTime(), trip.getEndDateTime()));

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

        private String formatDuration(String start, String end) {
            try {
                Date startDate = DB_DATE_FORMAT.parse(start);
                Date endDate = DB_DATE_FORMAT.parse(end);
                long durationMs = endDate.getTime() - startDate.getTime();
                long hours = TimeUnit.MILLISECONDS.toHours(durationMs);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60;
                return String.format(Locale.getDefault(), "%d ч %02d мин", hours, minutes);
            } catch (Exception e) {
                return "--";
            }
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView tvHeader;
            HeaderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvHeader = itemView.findViewById(R.id.tv_header_title);
            }
        }

        class TripViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDateTime, tvDistance, tvDuration, tvFuelSpent, tvFuelConsumption;

            TripViewHolder(@NonNull View itemView) {
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

    private void showDeleteConfirmation(Trip trip) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить поездку?")
                .setMessage("Эта операция необратима.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    dbHelper.deleteTrip(trip.getId());
                    loadTripsForCar();
                    Toast.makeText(getContext(), "Поездка удалена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}