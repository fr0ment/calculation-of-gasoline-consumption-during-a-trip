package com.example.cogcdat_2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsFragment extends Fragment {

    private DatabaseHelper dbHelper;

    // UI элементы
    private Spinner spinnerCars;
    private TextView tvTotalDistance, tvTotalFuel, tvAvgConsumption;
    private TextView tvNoData, tvWarningTitle, tvWarningDetails;
    private LineChart chartFuelConsumption;
    private View cardWarnings;

    // Форматы дат
    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat CHART_DATE_FORMAT = new SimpleDateFormat("dd.MM", Locale.getDefault());

    // Для хранения данных аналитики
    private List<Trip> allTrips = new ArrayList<>();
    private Map<String, Float> dailyConsumption = new HashMap<>();
    private double totalDistance = 0.0;
    private double totalFuel = 0.0;
    private double avgConsumption = 0.0;

    // Настройки для определения аномалий
    private static final double CONSUMPTION_THRESHOLD_MULTIPLIER = 1.15; // 15% выше среднего
    private static final int MIN_TRIPS_FOR_ANALYSIS = 5;
    private static final int RECENT_TRIPS_COUNT = 5;

    // Текущий выбранный автомобиль
    private Car selectedCar = null;

    // Списки для адаптера
    private List<Car> allCarsList = new ArrayList<>();
    private List<Car> spinnerCarsList = new ArrayList<>();

    // Для хранения предупреждений по автомобилям
    private Map<Integer, String> carWarnings = new HashMap<>();
    private List<Car> carsWithAnomalies = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        dbHelper = new DatabaseHelper(getContext());

        // Инициализация UI элементов
        initViews(view);

        // Настройка Spinner с автомобилями
        setupCarSpinner();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем данные при возвращении на фрагмент
        refreshCarSpinner();
        loadAnalyticsData();
    }

    private void initViews(View view) {
        spinnerCars = view.findViewById(R.id.spinner_cars);
        tvTotalDistance = view.findViewById(R.id.tv_total_distance);
        tvTotalFuel = view.findViewById(R.id.tv_total_fuel);
        tvAvgConsumption = view.findViewById(R.id.tv_avg_consumption);
        chartFuelConsumption = view.findViewById(R.id.chart_fuel_consumption);
        cardWarnings = view.findViewById(R.id.card_warnings);
        tvNoData = view.findViewById(R.id.tv_no_data);
        tvWarningTitle = view.findViewById(R.id.tv_warning_title);
        tvWarningDetails = view.findViewById(R.id.tv_warning_details);
    }

    private void setupCarSpinner() {
        // Получаем список всех автомобилей
        allCarsList = dbHelper.getAllCars();

        // Создаем список для Spinner: сначала фиктивный автомобиль "Все", затем все реальные
        spinnerCarsList.clear();

        // Создаем фиктивный автомобиль для отображения "Все автомобили"
        Car allCarsDummy = new Car();
        allCarsDummy.setId(-1); // Специальный ID для "всех автомобилей"
        allCarsDummy.setName("Все автомобили");
        allCarsDummy.setDistanceUnit("км");
        allCarsDummy.setFuelUnit("л");
        allCarsDummy.setFuelConsumptionUnit("л/100км");

        spinnerCarsList.add(allCarsDummy);
        spinnerCarsList.addAll(allCarsList);

        // Создаем кастомный адаптер для Spinner (теперь передаем только список Car)
        CarSpinnerAdapter adapter = new CarSpinnerAdapter(
                requireContext(),
                spinnerCarsList
        );

        spinnerCars.setAdapter(adapter);

        // Устанавливаем обработчик выбора
        spinnerCars.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Теперь здесь получаем Car, а не String
                Car selected = adapter.getItem(position);

                if (selected.getId() == -1) {
                    // Выбраны все автомобили
                    selectedCar = null;
                } else {
                    // Выбран конкретный автомобиль
                    selectedCar = selected;
                }

                // Перезагружаем данные с учетом выбранного автомобиля
                loadAnalyticsData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCar = null;
                loadAnalyticsData();
            }
        });
    }

    private void refreshCarSpinner() {
        // Обновляем список автомобилей
        allCarsList = dbHelper.getAllCars();

        // Обновляем список для Spinner
        spinnerCarsList.clear();

        // Фиктивный автомобиль "Все"
        Car allCarsDummy = new Car();
        allCarsDummy.setId(-1);
        allCarsDummy.setName("Все автомобили");
        allCarsDummy.setDistanceUnit("км");
        allCarsDummy.setFuelUnit("л");
        allCarsDummy.setFuelConsumptionUnit("л/100км");

        spinnerCarsList.add(allCarsDummy);
        spinnerCarsList.addAll(allCarsList);

        // Уведомляем адаптер об изменении данных
        if (spinnerCars.getAdapter() != null) {
            ((CarSpinnerAdapter) spinnerCars.getAdapter()).notifyDataSetChanged();
        }
    }

    private void loadAnalyticsData() {
        // Получаем поездки в зависимости от выбранного автомобиля
        allTrips = getTripsForSelectedCar();

        if (allTrips.isEmpty()) {
            showNoDataState();
            return;
        }

        // Скрываем сообщение "нет данных"
        tvNoData.setVisibility(View.GONE);

        // Рассчитываем статистику
        calculateStatistics();

        // Обновляем карточки статистики
        updateStatsCards();

        // Строим график
        buildConsumptionChart();

        // Проверяем на аномалии и отображаем предупреждения
        checkForConsumptionAnomalies();
    }

    private List<Trip> getTripsForSelectedCar() {
        List<Trip> trips = new ArrayList<>();

        if (selectedCar == null || selectedCar.getId() == -1) {
            // Если автомобиль не выбран или выбраны все авто, берем все поездки
            trips = getAllTripsFromAllCars();
        } else {
            // Берем поездки только для выбранного автомобиля
            trips = dbHelper.getTripsForCar(selectedCar.getId());

            // Сортируем по дате (от старых к новым)
            trips.sort((t1, t2) -> {
                try {
                    Date d1 = DB_DATE_FORMAT.parse(t1.getStartDateTime());
                    Date d2 = DB_DATE_FORMAT.parse(t2.getStartDateTime());
                    return d1.compareTo(d2);
                } catch (ParseException e) {
                    return 0;
                }
            });
        }

        return trips;
    }

    private List<Trip> getAllTripsFromAllCars() {
        List<Trip> allTrips = new ArrayList<>();
        List<Car> allCars = dbHelper.getAllCars();

        for (Car car : allCars) {
            List<Trip> carTrips = dbHelper.getTripsForCar(car.getId());
            allTrips.addAll(carTrips);
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

        return allTrips;
    }

    private void calculateStatistics() {
        totalDistance = 0.0;
        totalFuel = 0.0;
        dailyConsumption.clear();

        for (Trip trip : allTrips) {
            totalDistance += trip.getDistance();
            totalFuel += trip.getFuelSpent();

            // Группируем по дням для графика
            try {
                Date tripDate = DB_DATE_FORMAT.parse(trip.getStartDateTime());
                String dayKey = CHART_DATE_FORMAT.format(tripDate);

                // Если для этого дня уже есть запись, обновляем среднее значение
                if (dailyConsumption.containsKey(dayKey)) {
                    float currentAvg = dailyConsumption.get(dayKey);
                    float newAvg = (currentAvg + (float) trip.getFuelConsumption()) / 2;
                    dailyConsumption.put(dayKey, newAvg);
                } else {
                    dailyConsumption.put(dayKey, (float) trip.getFuelConsumption());
                }
            } catch (ParseException e) {
                // Пропускаем некорректные даты
            }
        }

        // Рассчитываем средний расход
        if (totalDistance > 0 && totalFuel > 0) {
            avgConsumption = (totalFuel / totalDistance) * 100;
        } else {
            avgConsumption = 0.0;
        }
    }

    private void updateStatsCards() {
        // Определяем единицы измерения
        String distanceUnit = "км";
        String fuelUnit = "л";
        String consumptionUnit = "л/100км";

        if (!allTrips.isEmpty()) {
            // Если выбран конкретный автомобиль, берем его единицы
            if (selectedCar != null && selectedCar.getId() != -1) {
                distanceUnit = selectedCar.getDistanceUnit();
                fuelUnit = selectedCar.getFuelUnit();
                consumptionUnit = selectedCar.getFuelConsumptionUnit();
            } else {
                // Для "всех автомобилей" берем единицы из первой поездки
                Car firstCar = dbHelper.getCar(allTrips.get(0).getCarId());
                if (firstCar != null) {
                    distanceUnit = firstCar.getDistanceUnit();
                    fuelUnit = firstCar.getFuelUnit();
                    consumptionUnit = firstCar.getFuelConsumptionUnit();
                }
            }
        }

        // Общий пробег
        tvTotalDistance.setText(String.format(Locale.getDefault(), "%.1f %s", totalDistance, distanceUnit));

        // Всего топлива
        tvTotalFuel.setText(String.format(Locale.getDefault(), "%.1f %s", totalFuel, fuelUnit));

        // Средний расход
        tvAvgConsumption.setText(String.format(Locale.getDefault(), "%.2f %s", avgConsumption, consumptionUnit));
    }

    private void buildConsumptionChart() {
        if (dailyConsumption.isEmpty()) {
            chartFuelConsumption.setVisibility(View.GONE);
            return;
        }

        // Получаем метки для оси X
        final List<String> labels = getChartLabels();

        // Подготовка данных для графика
        ArrayList<Entry> entries = new ArrayList<>();

        for (int i = 0; i < labels.size(); i++) {
            String day = labels.get(i);
            if (dailyConsumption.containsKey(day)) {
                entries.add(new Entry(i, dailyConsumption.get(day)));
            }
        }

        if (entries.isEmpty()) {
            chartFuelConsumption.setVisibility(View.GONE);
            return;
        }

        // Настройка внешнего вида графика
        chartFuelConsumption.getDescription().setEnabled(false);
        chartFuelConsumption.setDragEnabled(true);
        chartFuelConsumption.setScaleEnabled(true);
        chartFuelConsumption.setPinchZoom(true);
        chartFuelConsumption.setDrawGridBackground(false);

        // Убираем легенду
        chartFuelConsumption.getLegend().setEnabled(false);

        // Настройка оси X
        XAxis xAxis = chartFuelConsumption.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(Math.min(labels.size(), 7));
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
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

        // Настройка оси Y
        YAxis leftAxis = chartFuelConsumption.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(requireContext(), R.color.chart_grid_color));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

        YAxis rightAxis = chartFuelConsumption.getAxisRight();
        rightAxis.setEnabled(false);

        // Создание набора данных
        LineDataSet dataSet = new LineDataSet(entries, "Расход топлива");

        // Настройка стиля линии
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.chart_line_color));
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.chart_text_color));
        dataSet.setValueTextSize(10f);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.chart_circle_color));
        dataSet.setDrawCircleHole(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        // Форматирование значений на графике
        String consumptionUnit = "л/100км";
        if (selectedCar != null && selectedCar.getId() != -1) {
            consumptionUnit = selectedCar.getFuelConsumptionUnit();
        } else if (!allTrips.isEmpty()) {
            Car firstCar = dbHelper.getCar(allTrips.get(0).getCarId());
            if (firstCar != null) {
                consumptionUnit = firstCar.getFuelConsumptionUnit();
            }
        }

        final String finalUnit = consumptionUnit;
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f %s", value, finalUnit);
            }
        });

        // Создание данных для графика
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        LineData lineData = new LineData(dataSets);
        chartFuelConsumption.setData(lineData);

        // Обновление графика
        chartFuelConsumption.invalidate();
        chartFuelConsumption.setVisibility(View.VISIBLE);
    }

    private List<String> getChartLabels() {
        // Получаем список дат для оси X
        List<String> labels = new ArrayList<>(dailyConsumption.keySet());

        // Сортируем даты (старые -> новые)
        labels.sort((d1, d2) -> {
            try {
                Date date1 = CHART_DATE_FORMAT.parse(d1);
                Date date2 = CHART_DATE_FORMAT.parse(d2);
                return date1.compareTo(date2);
            } catch (ParseException e) {
                return 0;
            }
        });

        // Ограничиваем до последних 7 записей для лучшей читаемости
        if (labels.size() > 7) {
            labels = labels.subList(labels.size() - 7, labels.size());
        }

        return labels;
    }

    private void checkForConsumptionAnomalies() {
        // Очищаем предыдущие предупреждения
        carWarnings.clear();
        carsWithAnomalies.clear();
        cardWarnings.setVisibility(View.GONE);

        if (selectedCar == null || selectedCar.getId() == -1) {
            // РЕЖИМ "ВСЕ АВТОМОБИЛИ" - проверяем КАЖДЫЙ автомобиль отдельно
            checkAnomaliesForAllCars();
        } else {
            // РЕЖИМ КОНКРЕТНОГО АВТОМОБИЛЯ - проверяем только выбранный
            checkAnomaliesForSelectedCar();
        }
    }

    private void checkAnomaliesForSelectedCar() {
        if (selectedCar == null || selectedCar.getId() == -1) {
            return;
        }

        AnomalyResult anomaly = checkCarForAnomaly(selectedCar);

        if (anomaly != null) {
            // Найдена аномалия
            tvWarningTitle.setText("Повышенный расход топлива!");
            tvWarningDetails.setText(anomaly.getWarningMessage());
            cardWarnings.setVisibility(View.VISIBLE);

            // Сохраняем для отладки
            Log.d("Analytics", "Обнаружена аномалия для " + selectedCar.getName() +
                    ": увеличение на " + anomaly.increasePercent + "%");
        } else {
            // Нет аномалий - скрываем карточку
            cardWarnings.setVisibility(View.GONE);
            Log.d("Analytics", "Для автомобиля " + selectedCar.getName() + " аномалий не обнаружено");
        }
    }

    private void checkAnomaliesForAllCars() {
        List<Car> allCars = dbHelper.getAllCars();
        carsWithAnomalies.clear();
        carWarnings.clear();

        boolean hasAnyAnomaly = false;
        StringBuilder allWarningsText = new StringBuilder();
        int anomalyCount = 0;

        for (Car car : allCars) {
            AnomalyResult anomaly = checkCarForAnomaly(car);

            if (anomaly != null) {
                carsWithAnomalies.add(car);
                carWarnings.put(car.getId(), anomaly.getWarningMessage());
                hasAnyAnomaly = true;
                anomalyCount++;

                // Форматируем сообщение для каждого автомобиля
                allWarningsText.append("• ")
                        .append(anomaly.getWarningMessage())
                        .append("\n\n");
            }
        }

        // Показываем или скрываем карточку
        if (hasAnyAnomaly) {
            // Убираем последние два переноса строки
            if (allWarningsText.length() > 2) {
                allWarningsText.setLength(allWarningsText.length() - 2);
            }

            if (anomalyCount == 1) {
                tvWarningTitle.setText("Повышенный расход топлива!");
            } else {
                tvWarningTitle.setText(String.format("Повышенный расход топлива! (%d авто)", anomalyCount));
            }
            tvWarningDetails.setText(allWarningsText.toString());
            cardWarnings.setVisibility(View.VISIBLE);

            Log.d("Analytics", "Найдено автомобилей с аномалиями: " + anomalyCount);
        } else {
            cardWarnings.setVisibility(View.GONE);
            Log.d("Analytics", "Автомобилей с аномалиями не найдено");
        }
    }

    private List<Trip> getRecentTripsForCar(int carId, int count) {
        // Получаем все поездки для автомобиля
        List<Trip> allCarTrips = dbHelper.getTripsForCar(carId);

        if (allCarTrips.isEmpty()) {
            return new ArrayList<>();
        }

        // Сортируем по дате (от новых к старым)
        allCarTrips.sort((t1, t2) -> {
            try {
                Date d1 = DB_DATE_FORMAT.parse(t1.getStartDateTime());
                Date d2 = DB_DATE_FORMAT.parse(t2.getStartDateTime());
                return d2.compareTo(d1); // Новые сверху
            } catch (ParseException e) {
                return 0;
            }
        });

        // Берем первые N поездок (самые последние)
        int endIndex = Math.min(count, allCarTrips.size());
        return new ArrayList<>(allCarTrips.subList(0, endIndex));
    }

    private void showNoDataState() {
        tvNoData.setVisibility(View.VISIBLE);

        // Формируем текст в зависимости от выбора
        if (selectedCar != null && selectedCar.getId() != -1) {
            tvNoData.setText(String.format("Нет данных для автомобиля %s. Добавьте поездки, чтобы увидеть статистику.",
                    selectedCar.getName()));
        } else {
            tvNoData.setText("Нет данных для анализа. Добавьте поездки, чтобы увидеть статистику.");
        }

        tvTotalDistance.setText("0 км");
        tvTotalFuel.setText("0 л");
        tvAvgConsumption.setText("0.0 л/100км");
        chartFuelConsumption.setVisibility(View.GONE);
        cardWarnings.setVisibility(View.GONE);
    }
    /**
     * Проверяет автомобиль на аномалии по расходу топлива
     * Сравнивает последние 5 поездок с предыдущими поездками (не включая последние 5)
     */
    private AnomalyResult checkCarForAnomaly(Car car) {
        if (car == null) return null;

        // Получаем все поездки автомобиля (уже отсортированы от новых к старым из DatabaseHelper)
        List<Trip> allTrips = dbHelper.getTripsForCar(car.getId());

        // Нужно минимум 6 поездок: 5 последних + хотя бы 1 старая для сравнения
        if (allTrips.size() < 6) {
            Log.d("Analytics", "Для автомобиля " + car.getName() + " недостаточно поездок: " + allTrips.size());
            return null;
        }

        // Берем последние 5 поездок (первые 5 в списке, так как список отсортирован от новых к старым)
        List<Trip> recentTrips = allTrips.subList(0, 5);

        // Берем все остальные поездки (старые) для расчета общего среднего
        // Это все поездки, начиная с 6-й и до конца
        List<Trip> previousTrips = allTrips.subList(5, allTrips.size());

        // Рассчитываем средний расход для последних 5 поездок
        double recentAvgConsumption = calculateAverageConsumption(recentTrips);

        // Рассчитываем средний расход для всех старых поездок
        double previousAvgConsumption = calculateAverageConsumption(previousTrips);

        if (previousAvgConsumption == 0) {
            Log.d("Analytics", "Предыдущий средний расход равен 0 для " + car.getName());
            return null;
        }

        // Проверяем разницу
        double increasePercent = ((recentAvgConsumption - previousAvgConsumption) / previousAvgConsumption) * 100;

        Log.d("Analytics", String.format(Locale.getDefault(),
                "Автомобиль %s: общий средний = %.2f л/100км, последние 5 = %.2f л/100км, разница = %.1f%%",
                car.getName(), previousAvgConsumption, recentAvgConsumption, increasePercent));

        // Если расход вырос более чем на 15%
        if (increasePercent >= 15.0) {
            return new AnomalyResult(car, increasePercent, recentAvgConsumption, previousAvgConsumption);
        }

        return null;
    }

    /**
     * Вспомогательный класс для хранения результата проверки аномалии
     */
    private static class AnomalyResult {
        Car car;
        double increasePercent;
        double recentConsumption;
        double previousConsumption;

        AnomalyResult(Car car, double increasePercent, double recentConsumption, double previousConsumption) {
            this.car = car;
            this.increasePercent = increasePercent;
            this.recentConsumption = recentConsumption;
            this.previousConsumption = previousConsumption;
        }

        String getWarningMessage() {
            return String.format(Locale.getDefault(),
                    "Последние 5 поездок на автомобиле \"%s\" показывают расход на %.1f%% выше среднего " +
                            "(было: %.1f л/100км, стало: %.1f л/100км).",
                    car.getName(), increasePercent, previousConsumption, recentConsumption);
        }
    }

    /**
     * Рассчитывает средний расход топлива для списка поездок
     */
    private double calculateAverageConsumption(List<Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            return 0.0;
        }

        double totalDistance = 0.0;
        double totalFuel = 0.0;

        for (Trip trip : trips) {
            totalDistance += trip.getDistance();
            totalFuel += trip.getFuelSpent();
        }

        if (totalDistance == 0) {
            return 0.0;
        }

        return (totalFuel / totalDistance) * 100;
    }
}