package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddPark extends AppCompatActivity {
    private static final String TAG = "TAG";
    private static final float SMALLEST_DISPLACEMENT = 0.5F;
    Button btnGoBackMap;
    Button btnGetLatLong;
    Button btnConfirm;
    EditText inputParkName;
    EditText inputLat;
    EditText inputLong;
    private double currentLat;
    private double currentLong;
    private FusedLocationProviderClient fusedLocationClient;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    private boolean loggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_park);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            loggedIn = true;
        }


        btnGoBackMap = findViewById(R.id.btnGoBackMap);
        btnConfirm = findViewById(R.id.btnCreatePark);
        btnGetLatLong = findViewById(R.id.getLatLong);
        inputParkName = findViewById(R.id.inputParkName);
        inputLat = findViewById(R.id.inputLat);
        inputLong = findViewById(R.id.inputLong);

        btnGoBackMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Take user to MainActivity
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });

        btnGetLatLong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLocationUpdates();
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loggedIn) {
                    String parkName = inputParkName.getText().toString();
                    if (parkName.isEmpty()) {
                        Toast.makeText(AddPark.this, "Park was not created, every park needs a name", Toast.LENGTH_LONG).show();
                    } else {
                        String latitude = String.valueOf(inputLat.getText());
                        String longitude = String.valueOf(inputLong.getText());
                        if (latitude.isEmpty() || longitude.isEmpty()) {
                            Toast.makeText(AddPark.this, "Park was not created, try pressing Get Position to fill out latitude and longitude fields", Toast.LENGTH_LONG).show();
                        } else {
                            addPark(parkName, latitude, longitude);
                        }
                    }
                } else {
                    Toast.makeText(AddPark.this, "Park was not created, try logging in first!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public interface ParkExistenceCallback {
        void onResult(boolean exists);
    }

    public void doesParkExist(String parkName, ParkExistenceCallback callback) {
        db.collection("parks")
                .whereEqualTo("parkName", parkName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            boolean exists = !task.getResult().isEmpty();
                            callback.onResult(exists);
                        } else {
                            // Handle the error appropriately, for example:
                            callback.onResult(false);
                        }
                    }
                });
    }

    private void addPark(String parkName, String latitude, String longitude) {
        doesParkExist(parkName, new ParkExistenceCallback() {
            @Override
            public void onResult(boolean exists) {
                if (exists) {
                    Toast.makeText(AddPark.this, "Park was not created, park name already exists", Toast.LENGTH_LONG).show();
                } else {
                    Map<String, Object> userDocument = new HashMap<>();

                    // add fields to the document
                    userDocument.put("parkName", parkName);
                    userDocument.put("latitude", latitude);
                    userDocument.put("Longitude", longitude);
                    userDocument.put("parkReviews", new ArrayList<>());

                    db.collection("parks").add(userDocument);

                    // Now query the entry we just made to find the document ID
                    final String[] parkDocumentId = new String[1];

                    db.collection("parks")
                            .whereEqualTo("parkName", parkName)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                        parkDocumentId[0] = document.getId();

                                        // now query the users database using the park id
                                        Query query = db.collection("users").whereEqualTo("uid", mAuth.getUid());
                                        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    for (DocumentSnapshot document : task.getResult()) {
                                                        String userDocumentId = document.getId();

                                                        Map<String, Object> updates = new HashMap<>();
                                                        updates.put("parksAdded", FieldValue.arrayUnion(parkDocumentId[0]));

                                                        db.collection("users").document(userDocumentId).update(updates);
                                                        Toast.makeText(AddPark.this, "Park was created successfully!", Toast.LENGTH_LONG).show();
                                                    }
                                                } else {
                                                    Log.d(TAG, "Error getting documents: ", task.getException());
                                                    Toast.makeText(AddPark.this, "Error assigning park to user", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    } else {
                                        Log.d(TAG, "Error getting documents: ", task.getException());
                                    }
                                }
                            });
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(500)
                .setMinUpdateDistanceMeters(SMALLEST_DISPLACEMENT)
                .build();

        //grab current location
        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult.getLastLocation() != null) {
                    currentLat = locationResult.getLastLocation().getLatitude();
                    currentLong = locationResult.getLastLocation().getLongitude();
                    Log.d(TAG, "Location updated: " + currentLat + ", " + currentLong);

                    // Update the EditText fields with the new location
                    inputLat.setText(String.valueOf(currentLat));
                    inputLong.setText(String.valueOf(currentLong));
                } else {
                    Toast.makeText(AddPark.this, "Location unavailable", Toast.LENGTH_LONG).show();
                }
            }
        }, null);
    }
}