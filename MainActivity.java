package com.pavani.smart_parking;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pavani.smart_parking.model.ParkingBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final int TOTAL_SPOTS = 70; // Increased to cover example data (up to 69)
    private final int[] spotStatus = new int[TOTAL_SPOTS]; // 0: filled(red), 1: empty(green), 2: booked(yellow)
    private DatabaseReference mDatabaseLot;
    private DatabaseReference mDatabaseSlots;
    private ParkingLotAdapter adapter;
    private final List<Object> items = new ArrayList<>();

    // Local cache of data
    private final Map<String, Boolean> currentBookings = new HashMap<>();
    private final Map<Integer, ParkingBox> currentBoxes = new HashMap<>();
    
    // Track user's specific booking
    private int bookedSpot = -1; 
    private boolean approvalDialogShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase References
        mDatabaseLot = FirebaseDatabase.getInstance().getReference("parking_lot");
        mDatabaseSlots = FirebaseDatabase.getInstance().getReference("parking_slot");

        RelativeLayout rootLayout = findViewById(R.id.root_layout);
        if (rootLayout.getBackground() instanceof AnimationDrawable) {
            AnimationDrawable animationDrawable = (AnimationDrawable) rootLayout.getBackground();
            animationDrawable.setEnterFadeDuration(2500);
            animationDrawable.setExitFadeDuration(5000);
            animationDrawable.start();
        }

        setupRecyclerView();
        setupFirebaseListeners();
        setupButtonClickListeners();
    }

    private void setupRecyclerView() {
        int spotIndex = 0;
        // Grid setup: 10 rows, 8 columns
        for (int i = 0; i < 10 * 8; i++) {
            int col = i % 8;
            if (col == 2 || col == 5) {
                items.add("road");
            } else {
                if (spotIndex < TOTAL_SPOTS) {
                    items.add(spotIndex++);
                }
            }
        }

        RecyclerView recyclerView = findViewById(R.id.parking_grid);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 8));
        adapter = new ParkingLotAdapter(items, spotStatus, this::onSpotClick);
        recyclerView.setAdapter(adapter);
    }

    private void onSpotClick(int spotIndex) {
        // Status 1 is Empty (Green)
        if (spotStatus[spotIndex] == 1) {
            showBookingDialog(spotIndex);
        } else {
            String status = spotStatus[spotIndex] == 2 ? "Booked" : "Filled";
            Toast.makeText(MainActivity.this, "This spot is " + status, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFirebaseListeners() {
        // Listener for 'parking_lot' (Bookings)
        mDatabaseLot.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentBookings.clear();
                DataSnapshot bookingsSnapshot = dataSnapshot.child("bookings");
                for (DataSnapshot snapshot : bookingsSnapshot.getChildren()) {
                    // Key is spot ID (string), Value is boolean
                    String key = snapshot.getKey();
                    Boolean booked = snapshot.getValue(Boolean.class);
                    if (key != null && booked != null && booked) {
                        currentBookings.put(key, true);
                    }
                }
                updateSpotStatuses();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Failed to read bookings", databaseError.toException());
            }
        });

        // Listener for 'parking_slot' (Box Status)
        mDatabaseSlots.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentBoxes.clear();
                Log.e("surya", String.valueOf(dataSnapshot.getChildren()));
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Key is "box X"
                    String key = snapshot.getKey();
                    ParkingBox box = snapshot.getValue(ParkingBox.class);

                    if (key != null && key.startsWith("box ") && box != null) {
                        try {
                            int spotId = Integer.parseInt(key.substring(4).trim());
                            currentBoxes.put(spotId, box);
                        } catch (NumberFormatException e) {
                            Log.e("Firebase", "Invalid box key: " + key);
                        }
                    }
                }
                updateSpotStatuses();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Failed to read slots", databaseError.toException());
            }
        });
    }

    private void updateSpotStatuses() {
        // Iterate through all spots and determine status
        for (int i = 0; i < TOTAL_SPOTS; i++) {
            // Note: My spot array is 0-indexed, but example keys are like "box 1", etc.
            // Assuming "box 1" corresponds to index 0, "box 20" to index 19??
            // OR "box 1" corresponds to index 1 if we display 1-based.
            // Let's assume the keys match the display ID.
            // Currently, the adapter displays (index + 1). So index 0 displays "1".
            // So "box 1" should map to index 0.

            // Logic:
            // 1. Check physical status from 'parking_slot/box X'
            //    If status != 0 (e.g. 2?), it is Occupied/Filled?
            //    Wait, schema says "status": 2 for "box 1" which has "access_request": "pending".
            //    "box 20" has "status": 0 and "access_request": "none".
            //    Usually 0 = Empty/free in sensors? Or 0 = Occupied?
            //    Reviewing schema:
            //      "parking_lot.empty_slots" includes [7, 20, 23, ...]
            //      "box 20" has "status": 0. So 0 seems to be EMPTY.
            //      "box 1" has "status": 2. Is 2 Occupied?
            //      Typically: 0 = Empty, 1 = Occupied.
            //      But here we have status 2.
            //      Let's look at "bookings".
            //      "20": true. But "box 20": status 0.
            //      So slot 20 is physically empty (0) but Booked (true). Result: Booked (Yellow).
            //      "27": false. "box 27": status 0. Result: Empty (Green).

            //    So:
            //    RED (Occupied) if 'box status' != 0 (assuming > 0 is occupied/error).
            //    YELLOW (Booked) if 'bookings' contains it as true AND not physically occupied.
            //    GREEN (Empty) if 'box status' == 0 AND not booked.

            //    BUT, what about "access_request": "pending"?
            //    Plan said: If "pending", treat as Booked.
            //    Let's refine:
            //     - If Box Status != 0 -> Filled (Red) (Physical sensor overrides all?)
            //         Wait, user prompted "box 1": status 2, access "pending".
            //         Maybe status 2 MEANS "pending/booked" in the hardware?
            //         "box 20": status 0. Booking "20": true. So this is pure booking.
            //     - Let's stick to the color Status codes for the APP:
            //       0: Filled (Red)
            //       1: Empty (Green)
            //       2: Booked (Yellow)

            int displayId = i + 1; // 1-based ID for lookup
            String bookingKey = String.valueOf(displayId); // "20"
            
            boolean isBookedMap = currentBookings.containsKey(bookingKey) && Boolean.TRUE.equals(currentBookings.get(bookingKey));
            
            ParkingBox box = currentBoxes.get(displayId);
            int sensorStatus = (box != null) ? box.status : 0; // Default to 0 (Empty)? Or 1?
            // In the schema, "box 5" (not shown) might be missing.
            // Let's assume if missing, it's 0 (empty) or unknown.
            // Actually, based on "empty_slots" list provided, "7, 20..." are empty.
            // "box 20" status is 0. So 0 matches Empty.
            
            String accessRequest = (box != null) ? box.access_request : "none";

            int finalStatus = 1; // Default Green

            if (sensorStatus != 0 && sensorStatus != 2) { 
                // Assuming non-zero is occupied. 
                // But wait, "box 1" has status 2. Is key 1 in empty_slots? No.
                // So 1 is NOT empty.
                // Is 1 booked? Not in "bookings" map.
                // It has "access_request": "pending".
                // If status 2 means "Obstacle/Car detected", then it is Filled(0 for UI).
                
                // Let's MAP Sensor Status to UI Status.
                // UI: 0=Filled, 1=Empty, 2=Booked.
                
                // Sensor (Hypothesis):
                // 0 = Empty
                // 1 = Occupied (Car present)
                // 2 = Reserved/Pending?? (Box 1 has status 2).
                
                // Let's use robust logic:
                // If Booked in Map -> Show Yellow (2).
                // If Sensor says Occupied (status=1?) -> Show Red (0).
                
                // Prioritization:
                // If Booking Map has specific TRUE -> Yellow.
                // If Sensor Status is 'Occupied' -> Red. (Real car is there).
                // What if Booked AND Car is there? -> Red (Car arrived).
                
                // REVISED Logic based on "box 1": status 2.
                // Box 1 is NOT in empty_slots.
                // Box 20 IS in empty_slots. Status 0.
                // So Status 0 = Empty.
                // Status 2 (Box 1) = Not Empty.
                
                finalStatus = 0; // Assume Filled (Red) initially if not 0
            } else if (sensorStatus == 0) {
                 finalStatus = 1; // Empty (Green)
            }
            
            // Apply Booked Overlay
            if (isBookedMap) {
                 // If physically empty (1) but booked -> Booked (Yellow - 2)
                 if (finalStatus == 1) {
                     finalStatus = 2;
                 }
                 // If physically filled (0) and booked -> Still Filled (Red - 0) ?
                 // Or does Booked mean "Reserved for me"?
                 // Typically Red overrides Yellow.
            }

            // Apply "Pending" Overlay from Box
            if ("pending".equalsIgnoreCase(accessRequest)) {
                // Treat as Booked?
               if (finalStatus == 1) {
                   finalStatus = 2; // Show as Booked/Pending
               }
            }
            
            // Special handling for Box 1 with Status 2
            // If Sensor Status is 2, maybe that explicitly means "Reserved/Waiting"?
            // Let's treat sensor status 2 as Booked (Yellow)?
            if (sensorStatus == 2) {
                 // If it is 'pending' request, it's basically booked/reserved state.
                 // Show as Yellow (2).
                 // Unless a car is actually ON it.
                 // If 'status=2' implies 'hardware reserved LED is on', treat as Booked.
                 finalStatus = 2;
            }
            
            spotStatus[i] = finalStatus;
        }

        // Check for Approval Alert
        checkBookingApproval();

        adapter.notifyDataSetChanged();
    }

    private void checkBookingApproval() {
        if (bookedSpot != -1) {
            ParkingBox myBox = currentBoxes.get(bookedSpot);
            // Check for status 1 ONLY (as per user request)
            if (myBox != null && !approvalDialogShown) {
                if (myBox.status == 1) {
                    approvalDialogShown = true;
                    runOnUiThread(() -> showApprovalDialog(bookedSpot, myBox.status));
                }
            }
        }
    }

    private void showApprovalDialog(int spotId, int status) {
        new AlertDialog.Builder(this)
                .setTitle("Booking Confirmation")
                .setMessage("Spot " + spotId + " status is " + status + ". Do you want to approve this?")
                .setPositiveButton("Approve", (dialog, which) -> {
                     dialog.dismiss();
                     // No action requested
                })
                .setNegativeButton("Disapprove", (dialog, which) -> {
                    // "update access_request key to denied"
                    mDatabaseSlots.child("box " + spotId).child("access_request").setValue("denied");
                    Toast.makeText(MainActivity.this, "Booking Denied", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }


    private void setupButtonClickListeners() {
        ImageView homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> showHomeDialog());

        ImageView searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> showSearchDialog());

        TextView locationButton = findViewById(R.id.location_button);
        locationButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LocationActivity.class);
            startActivity(intent);
        });

        TextView middleButton = findViewById(R.id.middle_button);
        middleButton.setOnClickListener(v -> {
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        });

        TextView profileButton = findViewById(R.id.profile_button);
        profileButton.setOnClickListener(v -> showProfileDialog());
    }

    private void showBookingDialog(final int spotIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_book_now, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        Button btnBook = view.findViewById(R.id.btn_book);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        
        btnBook.setOnClickListener(v -> {
            // Updated Booking Logic
            int displayId = spotIndex + 1;
            String key = String.valueOf(displayId);
            
            // 1. Set booking in parking_lot
            mDatabaseLot.child("bookings").child(key).setValue(true);
            
            // Set local tracking
            bookedSpot = displayId;
            approvalDialogShown = false; // Reset for new booking
            
            // 2. Update parking_slot box access_request to "2" as requested
            mDatabaseSlots.child("box " + displayId).child("access_request").setValue("none");
            
            // Update status to 2 for visual feedback (Booked)
            mDatabaseSlots.child("box " + displayId).child("status").setValue(2);

            Toast.makeText(MainActivity.this, "Spot " + displayId + " booked!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // --- DIALOGS (Home, Search, Profile) --- //

    private void showProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_profile, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showHomeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_home, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        TextView tvAvailable = view.findViewById(R.id.tv_available_spots);
        TextView tvOccupied = view.findViewById(R.id.tv_occupied_spots);

        int availableCount = 0;
        for (int status : spotStatus) {
            if (status == 1) availableCount++;
        }

        tvAvailable.setText("Available Spots: " + availableCount);
        tvOccupied.setText("Occupied/Booked: " + (TOTAL_SPOTS - availableCount));

        dialog.show();
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_search, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        final EditText etSpotNumber = view.findViewById(R.id.et_spot_number);
        Button btnSearchSpot = view.findViewById(R.id.btn_search_spot);

        btnSearchSpot.setOnClickListener(v -> {
            String spotText = etSpotNumber.getText().toString();
            if (TextUtils.isEmpty(spotText)) {
                Toast.makeText(this, "Please enter a spot number.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int spotNumber = Integer.parseInt(spotText) - 1;
                if (spotNumber >= 0 && spotNumber < TOTAL_SPOTS) {
                    Toast.makeText(this, "Spot " + (spotNumber + 1) + " is " + getStatusText(spotStatus[spotNumber]), Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Invalid spot number.", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private String getStatusText(int status) {
        switch (status) {
            case 0: return "Filled";
            case 1: return "Empty";
            case 2: return "Booked";
            default: return "Unknown";
        }
    }
}
