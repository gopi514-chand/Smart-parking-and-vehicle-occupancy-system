package com.pavani.smart_parking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ParkingLotAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SPOT = 0;
    private static final int TYPE_ROAD = 1;

    private final List<Object> items;
    private final int[] spotStatus;
    private final OnSpotClickListener listener;

    public interface OnSpotClickListener {
        void onSpotClick(int spotIndex);
    }

    public ParkingLotAdapter(List<Object> items, int[] spotStatus, OnSpotClickListener listener) {
        this.items = items;
        this.spotStatus = spotStatus;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof Integer) {
            return TYPE_SPOT;
        } else {
            return TYPE_ROAD;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SPOT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_parking_spot, parent, false);
            return new SpotViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_road, parent, false);
            view.setBackgroundResource(R.drawable.vertical_road);
            return new RoadViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_SPOT) {
            SpotViewHolder spotViewHolder = (SpotViewHolder) holder;
            int spotIndex = (Integer) items.get(position);
            spotViewHolder.spot.setText(String.valueOf(spotIndex + 1));

            // This is the corrected code to get the real status
            int status = spotStatus[spotIndex];

            switch (status) {
                case 0: // occupied
                    spotViewHolder.spot.setBackgroundResource(R.drawable.parking_spot_occupied_bg);
                    break;
                case 1: // available
                    spotViewHolder.spot.setBackgroundResource(R.drawable.parking_spot_available_bg);
                    break;
                case 2: // reserved
                    spotViewHolder.spot.setBackgroundResource(R.drawable.parking_spot_reserved_bg);
                    break;
            }
            spotViewHolder.itemView.setOnClickListener(v -> listener.onSpotClick(spotIndex));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class SpotViewHolder extends RecyclerView.ViewHolder {
        TextView spot;

        public SpotViewHolder(@NonNull View itemView) {
            super(itemView);
            spot = itemView.findViewById(R.id.spot);
        }
    }

    public static class RoadViewHolder extends RecyclerView.ViewHolder {
        public RoadViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
