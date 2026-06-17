package com.pavani.smart_parking.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParkingLot {
    public Map<String, Boolean> bookings = new HashMap<>();
    public List<Integer> empty_slots;

    public ParkingLot() {
        // Default constructor required for calls to DataSnapshot.getValue(ParkingLot.class)
    }
}
