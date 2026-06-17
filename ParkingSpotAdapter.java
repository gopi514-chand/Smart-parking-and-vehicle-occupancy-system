package com.pavani.smart_parking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ParkingSpotAdapter extends RecyclerView.Adapter<ParkingSpotAdapter.ViewHolder> {

    private final int[] spotStatus;
    private final OnSpotClickListener listener;

    public interface OnSpotClickListener {
        void onSpotClick(int position);
    }

    public ParkingSpotAdapter(int[] spotStatus, OnSpotClickListener listener) {
        this.spotStatus = spotStatus;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_parking_spot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int status = spotStatus[position];
        holder.spot.setText(String.valueOf(position + 1));

        switch (status) {
            case 0: // occupied
                holder.spot.setBackgroundResource(R.drawable.parking_spot_occupied_bg);
                break;
            case 1: // available
                holder.spot.setBackgroundResource(R.drawable.parking_spot_available_bg);
                break;
            case 2: // reserved
                holder.spot.setBackgroundResource(R.drawable.parking_spot_reserved_bg);
                break;
        }

        holder.itemView.setOnClickListener(v -> listener.onSpotClick(position));
    }

    @Override
    public int getItemCount() {
        return spotStatus.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView spot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            spot = itemView.findViewById(R.id.spot);
        }
    }
}
