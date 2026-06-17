package com.pavani.smart_parking;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class LocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private RecyclerView rvNearbyParking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        rvNearbyParking = findViewById(R.id.rv_nearby_parking);
        rvNearbyParking.setLayoutManager(new LinearLayoutManager(this));

        Button btnNavigation = findViewById(R.id.btn_navigation);
        btnNavigation.setOnClickListener(v -> {
            // Hardcoded destination for demonstration purposes
            LatLng destination = new LatLng(37.7749, -122.4194); // San Francisco
            launchNavigation(destination);
        });

        setupNearbyParking();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    private void setupNearbyParking() {
        List<String> nearbyParking = new ArrayList<>();
        nearbyParking.add("Parking Lot A - 5 spots available");
        nearbyParking.add("Parking Lot B - 2 spots available");
        nearbyParking.add("Parking Lot C - 10 spots available");

        NearbyParkingAdapter adapter = new NearbyParkingAdapter(nearbyParking, (parkingLot) -> {
            Toast.makeText(this, "Clicked on " + parkingLot, Toast.LENGTH_SHORT).show();
            // You could add logic here to show the selected parking lot on the map
        });
        rvNearbyParking.setAdapter(adapter);
    }

    private void launchNavigation(LatLng destination) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destination.latitude + "," + destination.longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show();
        }
    }
}
