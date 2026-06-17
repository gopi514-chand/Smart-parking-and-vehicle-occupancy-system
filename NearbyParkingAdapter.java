package com.pavani.smart_parking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NearbyParkingAdapter extends RecyclerView.Adapter<NearbyParkingAdapter.ViewHolder> {

    private final List<String> parkingLots;
    private final OnParkingLotClickListener listener;

    public interface OnParkingLotClickListener {
        void onParkingLotClick(String parkingLot);
    }

    public NearbyParkingAdapter(List<String> parkingLots, OnParkingLotClickListener listener) {
        this.parkingLots = parkingLots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nearby_parking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String parkingLot = parkingLots.get(position);
        holder.tvParkingLotName.setText(parkingLot);
        holder.itemView.setOnClickListener(v -> listener.onParkingLotClick(parkingLot));
    }

    @Override
    public int getItemCount() {
        return parkingLots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvParkingLotName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvParkingLotName = itemView.findViewById(R.id.tv_parking_lot_name);
        }
    }
}
