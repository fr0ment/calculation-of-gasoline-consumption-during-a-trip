package com.example.cogcdat;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ListView listView;
    private CalculationAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<CalculationResult> calculations;
    public Button btnBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        listView = findViewById(R.id.listView);
        dbHelper = new DatabaseHelper(this);
        btnBack = findViewById(R.id.buttonBack);

        loadCalculations();
        // Долгое нажатие для удаления
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                CalculationResult calculation = calculations.get(position);
                dbHelper.deleteCalculation(calculation.getId());
                Toast.makeText(HistoryActivity.this, "Запись удалена", Toast.LENGTH_SHORT).show();
                loadCalculations(); // Обновляем список
                return true;
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadCalculations() {
        calculations = dbHelper.getAllCalculations();
        if (calculations.isEmpty()) {
            Toast.makeText(this, "История расчетов пуста", Toast.LENGTH_SHORT).show();
        } else {
            adapter = new CalculationAdapter(this, calculations);
            listView.setAdapter(adapter);
        }
    }
}