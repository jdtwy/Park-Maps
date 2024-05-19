package com.example.myapplication;

import static java.lang.Math.floor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.Distribution;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements MapsActivity.OnMarkerClickListener {
    private static final String TAG = "DEBUG";
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
    LinearLayout layoutReviews;
    LinearLayout parkInfoContainer;

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
        layoutReviews = findViewById(R.id.layoutReviews);
        parkInfoContainer = findViewById(R.id.parkInfoContainer);

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
        if (parkData.isEmpty()) {
            // in-case hideUIPayload
            parkInfoContainer.setVisibility(View.GONE);
        } else {
            // load collection references
            CollectionReference reviewsCollectionRef = db.collection("reviews");
            CollectionReference usersCollectionRef = db.collection("users");

            Log.d(TAG, "" + parkData.toString());

            // get/set park name
            String parkName = (String) parkData.get("parkName");
            Log.d(TAG, parkName);
            textParkName.setText(parkName);

            // get/set average review rating
            ArrayList<String> parkReviewsHashes = (ArrayList<String>) parkData.get("parkReviews");

            if (!parkReviewsHashes.isEmpty()) {
                List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                for (String hash : parkReviewsHashes) {
                    Task<DocumentSnapshot> task = reviewsCollectionRef.document(hash).get();
                    tasks.add(task);
                }

                Tasks.whenAllSuccess(tasks)
                        .addOnSuccessListener(taskResults -> {
                            List<DocumentSnapshot> documents = new ArrayList<>();
                            for (Object result : taskResults) {
                                DocumentSnapshot document = (DocumentSnapshot) result;
                                documents.add(document);
                            }

                            List<Float> ratings = new ArrayList<>();
                            for (DocumentSnapshot document : documents) {
                                Double rating = document.getDouble("rating");
                                if (rating != null) {
                                    ratings.add(rating.floatValue());
                                }
                            }

                            float averageRating = average(ratings);

                            String reviewRatingString = getRatingString(averageRating);

                            textAverageReviewRating.setText(reviewRatingString);

                            // get/set park review modules
                            for (int idx = 0; idx < layoutReviews.getChildCount(); idx++) {
                                // get document
                                DocumentSnapshot reviewSnapshot = documents.get(idx);

                                // select reviewModule
                                View reviewModule = layoutReviews.getChildAt(idx);

                                // get/set review rating
                                float rating = reviewSnapshot.getDouble("rating").floatValue();

                                String ratingString = getRatingString(rating);

                                TextView textReviewRating = reviewModule.findViewById(R.id.textReviewRating);
                                textReviewRating.setText(ratingString);

                                // get/set review text
                                String reviewText = reviewSnapshot.getString("review");

                                TextView textReview = reviewModule.findViewById(R.id.textReview);
                                textReview.setText(reviewText);

                                // get/set features string
                                ArrayList<String> features = (ArrayList<String>) reviewSnapshot.get("features");

                                String featuresString = "";

                                for (String feature : features) {
                                    featuresString += feature;
                                }

                                TextView textReviewSymbols = reviewModule.findViewById(R.id.textReviewSymbols);
                                textReviewSymbols.setText(featuresString);

                                // get/set review leaver name
                                String userHash = reviewSnapshot.getString("user");

                                usersCollectionRef.document(userHash)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot document = task.getResult();
                                                    if (document.exists()) {
                                                        String username = document.getString("username");

                                                        TextView textReviewUsername = reviewModule.findViewById(R.id.textReviewUsername);
                                                        textReviewUsername.setText(username);
                                                    } else {
                                                        Log.d(TAG, "No such document");
                                                    }
                                                } else {
                                                    Log.d(TAG, "get failed with ", task.getException());
                                                }
                                            }
                                        });
                            }

                            // TODO: implement get/set park features modules
                        });
            }

            parkInfoContainer.setVisibility(View.VISIBLE);
        }
    }

    private float average(List<Float> nums) {
        float sum = 0;
        for (Float num : nums) {
            sum += num;
        }
        return sum / nums.size();
    }

    private String getRatingString(float rating) {
        final int TOTAL_STARS = 5;

        // get rating string, e.g "3.4 ★★★☆☆"
        int numFullStars = (int) floor(rating);
        int numEmptyStars = TOTAL_STARS - numFullStars;

        String reviewRatingString = "";
        reviewRatingString += String.format("%.1f", rating) + " ";

        for (int idx = 1; idx <= numFullStars; idx++) {
            reviewRatingString += "★";
        }

        for (int idx = 1; idx <= numEmptyStars; idx++) {
            reviewRatingString += "☆";
        }

        return reviewRatingString;
    }

}