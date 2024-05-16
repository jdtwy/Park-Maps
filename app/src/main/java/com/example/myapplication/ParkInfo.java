package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ParkInfo extends AppCompatActivity {
    FirebaseFirestore db;

    // put in every activity which can send user to generate content (can make this a class that evey activity extends from in future)
    FirebaseAuth mAuth;
    boolean loggedIn = false;
    String mode;

    LinearLayout linearLayoutInfo;
    RecyclerView recyclerViewReviews;
    Button btnShowInfo;
    Button btnShowReviews;
    Button btnGoBackMap;
    Button btnGoAddReview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_park_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        linearLayoutInfo = findViewById(R.id.linearLayoutInfo);
        recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
        btnShowInfo = findViewById(R.id.btnShowInfo);
        btnShowReviews = findViewById(R.id.btnShowReviews);
        btnGoBackMap = findViewById(R.id.btnGoBackMap);
        btnGoAddReview = findViewById(R.id.btnGoAddReview);

        // enable 'logged-in'-user features
        if (mAuth.getCurrentUser() != null) {
            loggedIn = true;
        }

        // set mode for activity
        Intent intent = getIntent();
        if (intent != null) {
            mode = intent.getStringExtra("mode");
            if (mode == null) {
                mode = "info";
            }
        }

        // choose starting info view
        if (mode.equals("info")) {
            linearLayoutInfo.setVisibility(View.VISIBLE);
        } else {
            recyclerViewReviews.setVisibility(View.VISIBLE);
        }

        btnShowInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearLayoutInfo.setVisibility(View.VISIBLE);
                recyclerViewReviews.setVisibility(View.INVISIBLE);
            }
        });

        btnShowReviews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearLayoutInfo.setVisibility(View.INVISIBLE);
                recyclerViewReviews.setVisibility(View.VISIBLE);
            }
        });

        btnGoBackMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Take user to MainActivity
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });

        btnGoAddReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loggedIn) {
                    // Take user to AddReview
                    startActivity(new Intent(getApplicationContext(), AddReview.class));
                } else {
                    Toast.makeText(ParkInfo.this, "You must be logged in to leave a review!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}