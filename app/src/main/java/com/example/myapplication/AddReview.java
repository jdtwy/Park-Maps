package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddReview extends AppCompatActivity {
    FirebaseFirestore db;
    FirebaseAuth mAuth;

    Button btnGoBackMap;
    Button btnCreateReview;
    TextView textParkName;
    TextView inputReviewScore;
    TextView inputReviewText;
    String parkData;

    String Tag = "DEBUG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_review);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        btnGoBackMap = findViewById(R.id.btnGoBackMap);
        btnCreateReview = findViewById(R.id.btnCreateReview);
        textParkName = findViewById(R.id.textParkName);
        inputReviewScore = findViewById(R.id.inputReviewScore);
        inputReviewText = findViewById(R.id.inputReviewText);

        // get parkData from previous activity
        Intent intent = getIntent();
        if (intent != null) {
            parkData = intent.getStringExtra("parkData");
        }

        String parkHash = getParkHash(parkData);

        //Log.d(Tag, parkData);

        String parkName = getParkName(parkData);

        textParkName.setText(parkName);

        btnGoBackMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Take user to ParkInfo
                startActivity(new Intent(getApplicationContext(), ParkInfo.class));
            }
        });

        btnCreateReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float rating = Float.parseFloat(inputReviewScore.getText().toString());
                String review = inputReviewText.getText().toString();
                String dummyFeatures = "\uD83C\uDF56\uD83D\uDEBE"; // change this to get stuff from checkboxes

                addReview(parkHash, rating, review, dummyFeatures);
            }
        });
    }

    private void addReview(String parkHash, float rating, String review, String features) {
        Map<String, Object> reviewDocument = new HashMap<>();

        reviewDocument.put("uid", mAuth.getUid());
        reviewDocument.put("parkHash", parkHash);
        reviewDocument.put("rating", rating);
        reviewDocument.put("review", review);
        reviewDocument.put("features", features);

        db.collection("reviews")
                .add(reviewDocument);
    }

    private String getParkName(String parkData) {
        String startMarker = "parkName=";
        String endMarker = ",";

        int startIndex = parkData.indexOf(startMarker) + startMarker.length();

        int endIndex = parkData.indexOf(endMarker, startIndex);

        String parkName = parkData.substring(startIndex, endIndex).trim();

        return parkName;
    }

    private String getParkHash(String parkData) {
        String startMarker = "hash=";
        String endMarker = ",";

        int startIndex = parkData.indexOf(startMarker) + startMarker.length();

        int endIndex = parkData.indexOf(endMarker, startIndex);

        String parkHash = parkData.substring(startIndex, endIndex).trim();

        return parkHash;
    }
}