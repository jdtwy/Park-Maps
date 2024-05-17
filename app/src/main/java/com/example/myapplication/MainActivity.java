package com.example.myapplication;

import static java.lang.Math.floor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements MapsActivity.OnMarkerClickListener {
    private static final String TAG = "TAG";
    FirebaseFirestore db;

    // put in every activity which can send user to generate content (can make this a class that evey activity extends from in future)
    FirebaseAuth mAuth;
    boolean loggedIn = false;

    Button btnGoAuth;
    Button btnGoAddPark;
    Button btnGoInfo;
    Button btnGoReviews;
    TextView textParkName;
    TextView textAverageReviewRating;

    Map<Object, String> parkData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        btnGoAuth = findViewById(R.id.btnGoAuth);
        btnGoAddPark = findViewById(R.id.btnGoAddPark);
        btnGoInfo = findViewById(R.id.btnGoInfo);
        btnGoReviews = findViewById(R.id.btnGoReviews);
        textParkName = findViewById(R.id.textParkName);
        textAverageReviewRating = findViewById(R.id.textAverageReviewRating);

        // enable 'logged-in'-user features
        if (mAuth.getCurrentUser() != null) {
            loggedIn = true;
        }

        if (loggedIn) {
            btnGoAuth.setVisibility(View.VISIBLE);
        }

        btnGoAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Take user to Login
                startActivity(new Intent(getApplicationContext(), Login.class));
            }
        });

        btnGoAddPark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loggedIn) {
                    // Take user to AddPark
                    startActivity(new Intent(getApplicationContext(), AddPark.class));
                } else {
                    Toast.makeText(MainActivity.this, "You must be logged in to add a park!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnGoInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Take user to ParkInfo and set mode to Info
                startActivity((new Intent(getApplicationContext(), ParkInfo.class)
                        .putExtra("mode", "info")
                ));
            }
        });

        btnGoReviews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Take user to ParkInfo and set mode to Reviews
                startActivity((new Intent(getApplicationContext(), ParkInfo.class)
                        .putExtra("mode", "reviews")
                ));
            }
        });

        loadMapsFragment();
    }

    private void loadMapsFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        MapsActivity mapsFragment = new MapsActivity();
        fragmentTransaction.replace(R.id.map_container, mapsFragment);
        fragmentTransaction.commit();
        Log.d(TAG, "load maps fragment has ran");
    }

    @Override
    public void updateUI(Map<String, Object> parkData) {
        Log.d("MainActivity", "updateUI called with parkId: " + parkData);

        // load data from reviews collection
        CollectionReference reviewsCollectionRef = db.collection("reviews");

        // get/set park name
        String parkName = (String) parkData.get("name");
        textParkName.setText(parkName);

        // get/set average rating
        ArrayList<String> parkReviewsHashes = (ArrayList<String>) parkData.get("parkReviews");

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String hash : parkReviewsHashes) {
            Task<DocumentSnapshot> task = reviewsCollectionRef.document(hash).get();
            tasks.add(task);
        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(taskResults -> {
                    List<Float> ratings = new ArrayList<>();
                    for (Object result : taskResults) {
                        DocumentSnapshot document = (DocumentSnapshot) result;
                        Double rating = document.getDouble("rating");
                        if (rating != null) {
                            ratings.add(rating.floatValue());
                        }
                    }

                    float averageRating = calculateAverage(ratings);

                    String reviewRatingString = getRatingString(averageRating);

                    textAverageReviewRating.setText(reviewRatingString);

                    // TODO: JAMES/OWEN: START WORKING FROM HERE I GUESS
                    // PURPOSE OF THIS FUNCTION IS TO UPDATE ALL THE UI. CURRENTLY ONLY UPDATED REVIEW RATING AND PARK NAME
                    // TODO: UPDATE FEATURES UI ELEMENT AND REVIEWS UI ELEMENT, DEFINED IN linear_layout_review.xml and linear_layout_feature.xml
                    // FEEL FREE TO CHANGE THESE IF THEY SUCK
                })
                .addOnFailureListener(e -> {
                    // Handle any errors
                });
    }

    private float calculateAverage(List<Float> ratings) {
        float sum = 0;
        for (Float rating : ratings) {
            sum += rating;
        }
        return sum / ratings.size();
    }

    private String getRatingString(float ratingString) {
        final int TOTAL_STARS = 5;

        // get rating string, e.g "3.4 ★★★☆☆"
        int numFullStars = (int) floor(ratingString);
        int numEmptyStars = TOTAL_STARS - numFullStars;

        String reviewRatingString = "";
        reviewRatingString += String.format("%.1f", ratingString) + " ";

        for (int idx = 1; idx <= numFullStars; idx++) {
            reviewRatingString += "★";
        }

        for (int idx = 1; idx <= numEmptyStars; idx++) {
            reviewRatingString += "☆";
        }

        return reviewRatingString;
    }

}