package com.example.cogcdat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class CalculationAdapter extends ArrayAdapter<CalculationResult> {

    private Context context;
    private List<CalculationResult> calculations;

    public CalculationAdapter(Context context, List<CalculationResult> calculations) {
        super(context, R.layout.list_item_calculation, calculations);
        this.context = context;
        this.calculations = calculations;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_calculation, parent, false);
        }

        CalculationResult calculation = calculations.get(position);

        TextView tvDistance = convertView.findViewById(R.id.tvDistance);
        TextView tvFuel = convertView.findViewById(R.id.tvFuel);
        TextView tvCost = convertView.findViewById(R.id.tvCost);
        TextView tvTimestamp = convertView.findViewById(R.id.tvTimestamp);

        tvDistance.setText(String.format("Расстояние: %.1f км", calculation.getDistance()));
        tvFuel.setText(String.format("Топливо: %.1f л", calculation.getFuelNeeded()));
        tvCost.setText(String.format("Стоимость: %.2f руб", calculation.getTripCost()));
        tvTimestamp.setText(calculation.getTimestamp());

        return convertView;
    }
}