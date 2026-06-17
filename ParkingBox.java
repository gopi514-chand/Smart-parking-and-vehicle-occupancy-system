package com.pavani.smart_parking.model;

public class ParkingBox {
    public String access_request;
    public int status;

    public ParkingBox() {
        // Default constructor required for calls to DataSnapshot.getValue(ParkingBox.class)
    }

    public ParkingBox(String access_request, int status) {
        this.access_request = access_request;
        this.status = status;
    }
}
