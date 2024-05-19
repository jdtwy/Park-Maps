package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MapsActivity extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "DEBUG";
    private static final float SMALLEST_DISPLACEMENT = 0.5F;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;

    Map<String, Object> parkData;
    private OnMarkerClickListener listener;
    private LatLng position;

    FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        Log.d(TAG, "Initializing map fragment");

        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey("position")) {
            position = bundle.getParcelable("position");
            Log.d(TAG, "position parsed");
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (position == null) {
            if (hasPermissions()) {
                getLastLocation();
                createLocationRequest();
            } else {
                askPermissions();
            }
        }

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        LatLng latLng = new LatLng(latitude, longitude);
                        if (mMap != null) {
                            Log.d(TAG, "location result callback triggered");
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        }
                    }
                }
            }
        };

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    private boolean hasPermissions() {
        boolean permissionStatus = true;
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission is granted: " + permission);
            } else {
                Log.d(TAG, "Permission is not granted: " + permission);
                permissionStatus = false;
            }
        }
        return permissionStatus;
    }

    private void askPermissions() {
        if (!hasPermissions()) {
            Log.d(TAG, "Launching multiple contract permission launcher for ALL required permissions");
            multiplePermissionActivityResultLauncher.launch(PERMISSIONS);
        } else {
            Log.d(TAG, "All permissions are already granted");
        }
    }

    //Result launcher for permissions
    private final ActivityResultLauncher<String[]> multiplePermissionActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
        Log.d(TAG, "Launcher result: " + isGranted.toString());
        getLastLocation(); // get start location
        createLocationRequest(); // set up the location tracking
        if (isGranted.containsValue(false)) {
            Log.d(TAG, "At least one of the permissions was not granted, please enable permissions to ensure app functionality");
        }
    });

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Log.d(TAG, "Last Location detected " + location.getLatitude() + " " + location.getLongitude());
                LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
                if (mMap != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 19));
                }
                createLocationRequest();
            } else {
                Toast.makeText(requireContext(), "No location detected", Toast.LENGTH_SHORT).show();
                createLocationRequest();
            }
        });
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest.Builder(500)
                .setMinUpdateDistanceMeters(SMALLEST_DISPLACEMENT)
                .build();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (OnMarkerClickListener) context;
    }

    @SuppressLint("MissingPermission")
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Map ready");

        if (!mFusedLocationClient.getLastLocation().isSuccessful()) {
            if (position != null) {
                stopLocationUpdates();
                Log.d(TAG, "location updates stopped" + position.toString());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 19));
                mMap.setMyLocationEnabled(true);
                SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
                View mapView = mapFragment.getView();
                View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
                locationButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        createLocationRequest();
                        startLocationUpdates();
                        mMap.setMyLocationEnabled(true);
                    }
                });
            } else {
                createLocationRequest(); // set up the location tracking
                Log.d(TAG, "Setting up location tracking");
            }
        }
        changeMyLocationButtonPosition();

        parkData = null;

        populateMarkers(googleMap);

        // get park data when marker clicked
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                String parkId = marker.getSnippet(); // expecting doc hash, e.g 'a24MalBm5zYtMaSGrTbP', so marker must have this snippet
                setParkData(parkId);

                return false;
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                Map<String, Object> hideUIPayload = new HashMap<>();

                if (listener != null) {
                    listener.updateUI(hideUIPayload);
                }
            }
        });
    }

    private void populateMarkers(GoogleMap googleMap) {
        final Executor executor = Executors.newSingleThreadExecutor();
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        executor.execute(new Runnable() {
            @Override
            public void run() {
                db.collection("parks")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Map<String, Object> map = document.getData();
                                        String parkName = (String) map.get("parkName");
                                        float latitude = Float.parseFloat((String) map.get("latitude"));
                                        float longitude = Float.parseFloat((String) map.get("longitude"));
                                        LatLng pos = new LatLng(latitude, longitude);
                                        String parkHash = document.getId();

                                        // Update UI on the main thread
                                        mainHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                googleMap.addMarker(new MarkerOptions()
                                                        .position(pos)
                                                        .title(parkName)
                                                        .snippet(parkHash));
                                            }
                                        });
                                    }
                                }
                            }
                        });
            }
        });
    }

    // Define an interface
    public interface OnMarkerClickListener {
        void updateUI(Map<String, Object> parkData);
    }

    private void changeMyLocationButtonPosition() {
        // Get 'My Location' view
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        View mapView = mapFragment.getView();
        View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));

        // Position button middle-right of screen
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();

        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        locationButton.setLayoutParams(layoutParams);
    }

    private void setParkData(String parkId) {
        Log.d(TAG, "Selected park hash: " + parkId);
        DocumentReference parkRef = db.collection("parks").document(parkId);

        parkRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot parkDataSnapshot = task.getResult();
                    Log.d(TAG, parkDataSnapshot.toString());
                    if (parkDataSnapshot.exists()) {
                        parkData = parkDataSnapshot.getData();
                        Log.d(TAG, "Document data: " + parkData);

                        if (listener != null) {
                            listener.updateUI(parkData);
                        }
                    } else {
                        Log.d(TAG, "No such document in setParkData");
                    }
                } else {
                    Log.d(TAG, "Document get failed with ", task.getException());
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Failed to get park with hash: " + parkId);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasPermissions()) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopLocationUpdates();
    }
}