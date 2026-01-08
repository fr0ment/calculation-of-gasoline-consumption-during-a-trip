package com.example.cogcdat_2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsFragment extends Fragment {

    private DatabaseHelper dbHelper;

    // UI элементы
    private TextView tvTotalDistance, tvTotalFuel, tvAvgConsumption;
    private TextView tvNoData, tvWarningTitle, tvWarningDetails;
    private LineChart chartFuelConsumption;
    private View cardWarnings;
    private View carSelectionView;
    private ImageView ivSelectedCarIcon;
    private TextView tvSelectedCarName;

    // Форматы дат
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat CHART_DATE_FORMAT = new SimpleDateFormat("dd.MM", Locale.getDefault());

    // Данные аналитики
    private List<Trip> allTrips = new ArrayList<>();
    private Map<String, Float> dailyConsumption = new HashMap<>();
    private double totalDistance = 0.0;
    private double totalFuel = 0.0;
    private double avgConsumption = 0.0;

    // Выбранный автомобиль
    private Car selectedCar = null;

    // Список всех автомобилей (только реальные)
    private List<Car> carList = new ArrayList<>();

    private View rootView;
    private View layoutMonitoringSection;
    private View layoutChart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_analytics, container, false);

        dbHelper = new DatabaseHelper(getContext());

        initViews(rootView);
        setupCarSelection();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAnalyticsData();
    }

    private void initViews(View view) {
        tvTotalDistance = view.findViewById(R.id.tv_total_distance);
        tvTotalFuel = view.findViewById(R.id.tv_total_fuel);
        tvAvgConsumption = view.findViewById(R.id.tv_avg_consumption);
        chartFuelConsumption = view.findViewById(R.id.chart_fuel_consumption);
        cardWarnings = view.findViewById(R.id.card_warnings);
        layoutMonitoringSection = view.findViewById(R.id.layout_monitoring_section);
        layoutChart = view.findViewById(R.id.layout_chart);
        tvNoData = view.findViewById(R.id.tv_no_data);
        tvWarningTitle = view.findViewById(R.id.tv_warning_title);
        tvWarningDetails = view.findViewById(R.id.tv_warning_details);

        carSelectionView = view.findViewById(R.id.car_selection_view);
        ivSelectedCarIcon = view.findViewById(R.id.iv_selected_car_icon);
        tvSelectedCarName = view.findViewById(R.id.tv_selected_car_name);
    }

    private void setupCarSelection() {
        carSelectionView.setOnClickListener(v -> showCarSelectionDialog());
    }

    private void loadAnalyticsData() {
        carList = dbHelper.getAllCars();

        if (carList.isEmpty()) {
            carSelectionView.setVisibility(View.GONE);
            tvNoData.setText("Нет добавленных автомобилей");
            tvNoData.setVisibility(View.VISIBLE);
            hideAllDataViews();
            return;
        }

        carSelectionView.setVisibility(View.VISIBLE);

        // Восстанавливаем выбранный автомобиль
        int savedCarId = SelectedCarManager.getSelectedCarId(requireContext());
        selectedCar = null;

        for (Car car : carList) {
            if (car.getId() == savedCarId) {
                selectedCar = car;
                break;
            }
        }

        // Если сохранённого нет — выбираем первый автомобиль
        if (selectedCar == null) {
            selectedCar = carList.get(0);
            SelectedCarManager.setSelectedCarId(requireContext(), selectedCar.getId());
        }

        updateSelectedCarDisplay(selectedCar);

        // Загружаем поездки только для выбранного автомобиля
        allTrips = dbHelper.getTripsForCar(selectedCar.getId());

        if (allTrips.isEmpty()) {
            showNoDataState();
            return;
        }

        // Сортируем по дате (от старых к новым для графика)
        allTrips.sort((t1, t2) -> {
            try {
                Date d1 = DB_DATE_FORMAT.parse(t1.getStartDateTime());
                Date d2 = DB_DATE_FORMAT.parse(t2.getStartDateTime());
                return d1.compareTo(d2);
            } catch (ParseException e) {
                return 0;
            }
        });

        calculateTotals();
        calculateDailyConsumption();
        updateStatistics();
        updateChart();
        checkAnomalies();

        tvNoData.setVisibility(View.GONE);
        layoutChart.setVisibility(View.VISIBLE);
        cardWarnings.setVisibility(View.GONE); // пока скрываем, можно включить позже
    }

    private void calculateTotals() {
        totalDistance = 0.0;
        totalFuel = 0.0;
        for (Trip trip : allTrips) {
            totalDistance += trip.getDistance();
            totalFuel += trip.getFuelSpent();
        }
        avgConsumption = totalDistance > 0 ? (totalFuel / totalDistance) * 100 : 0.0;
    }

    private void calculateDailyConsumption() {
        Map<String, Double> dailyDist = new HashMap<>();
        Map<String, Double> dailyFuel = new HashMap<>();

        for (Trip trip : allTrips) {
            try {
                Date date = DB_DATE_FORMAT.parse(trip.getStartDateTime());
                String key = CHART_DATE_FORMAT.format(date);

                dailyDist.put(key, dailyDist.getOrDefault(key, 0.0) + trip.getDistance());
                dailyFuel.put(key, dailyFuel.getOrDefault(key, 0.0) + trip.getFuelSpent());
            } catch (ParseException e) {
                Log.e("Analytics", "Ошибка парсинга даты", e);
            }
        }

        dailyConsumption.clear();
        for (String key : dailyFuel.keySet()) {
            double dist = dailyDist.getOrDefault(key, 0.0);
            if (dist > 0) {
                dailyConsumption.put(key, (float) ((dailyFuel.get(key) / dist) * 100));
            }
        }
    }

    private void updateStatistics() {
        tvTotalDistance.setText(String.format(Locale.getDefault(), "%.0f км", totalDistance));
        tvTotalFuel.setText(String.format(Locale.getDefault(), "%.1f л", totalFuel));
        tvAvgConsumption.setText(String.format(Locale.getDefault(), "%.1f л/100км", avgConsumption));
    }

    private void updateChart() {
        if (dailyConsumption.isEmpty()) {
            chartFuelConsumption.clear();
            chartFuelConsumption.setNoDataText("Нет данных для графика");
            return;
        }

        List<String> labels = new ArrayList<>(dailyConsumption.keySet());
        Collections.sort(labels);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            entries.add(new Entry(i, dailyConsumption.get(labels.get(i))));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Расход топлива");
        dataSet.setColor(getResources().getColor(R.color.primary, null));
        dataSet.setCircleColor(getResources().getColor(R.color.primary, null));
        dataSet.setValueTextColor(getResources().getColor(R.color.text_primary, null));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);

        LineData lineData = new LineData(dataSet);
        chartFuelConsumption.setData(lineData);
        chartFuelConsumption.getDescription().setEnabled(false);
        chartFuelConsumption.getLegend().setEnabled(false);

        XAxis xAxis = chartFuelConsumption.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });

        chartFuelConsumption.getAxisRight().setEnabled(false);
        chartFuelConsumption.getAxisLeft().setAxisMinimum(0f);
        chartFuelConsumption.invalidate();
    }

    private void checkAnomalies() {
        // Можно включить позже — пока просто скрываем карточку
        cardWarnings.setVisibility(View.GONE);
    }

    private void showNoDataState() {
        tvNoData.setVisibility(View.VISIBLE);
        tvNoData.setText(String.format("Нет данных для автомобиля %s. Добавьте поездки, чтобы увидеть статистику.",
                selectedCar.getName()));

        tvTotalDistance.setText("0 км");
        tvTotalFuel.setText("0 л");
        tvAvgConsumption.setText("0.0 л/100км");

        hideAllDataViews();
    }

    private void hideAllDataViews() {
        layoutChart.setVisibility(View.GONE);
        layoutMonitoringSection.setVisibility(View.GONE); // Скрываем весь блок
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
        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_view_cars);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        CarSelectionAdapter adapter = new CarSelectionAdapter(carList, car -> {
            selectedCar = car;
            SelectedCarManager.setSelectedCarId(requireContext(), car.getId());
            updateSelectedCarDisplay(car);
            loadAnalyticsData(); // Перезагружаем аналитику для нового авто
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Подсвечиваем текущий выбранный
        int selectedPos = -1;
        for (int i = 0; i < carList.size(); i++) {
            if (carList.get(i).getId() == selectedCar.getId()) {
                selectedPos = i;
                break;
            }
        }
        adapter.setSelectedPosition(selectedPos);

        recyclerView.setAdapter(adapter);

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}