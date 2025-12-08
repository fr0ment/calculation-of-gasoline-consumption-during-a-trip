package com.example.cogcdat_2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.List;

public class CarSpinnerAdapter extends ArrayAdapter<Car> {

    private final LayoutInflater inflater;
    private final int resourceId;

    public CarSpinnerAdapter(Context context, List<Car> cars, boolean isDropdown) {
        super(context, R.layout.spinner_car_dropdown, cars);
        this.inflater = LayoutInflater.from(context);
        this.resourceId = R.layout.spinner_car_dropdown;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(resourceId, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.iv_car_icon);
            holder.name = convertView.findViewById(R.id.tv_car_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Car car = getItem(position);
        if (car != null) {
            holder.name.setText(car.getName());

            String imagePath = car.getImagePath();
            if (imagePath != null && !imagePath.isEmpty() && new File(imagePath).exists()) {
                Glide.with(getContext())
                        .load(new File(imagePath))
                        .circleCrop()
                        .placeholder(R.drawable.ic_car_outline)
                        .error(R.drawable.ic_car_outline)
                        .into(holder.icon);
            } else {
                holder.icon.setImageResource(R.drawable.ic_car_outline);
            }
        }

        return convertView;
    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
    }
}