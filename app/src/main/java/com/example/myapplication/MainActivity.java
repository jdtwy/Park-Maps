package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    FirebaseFirestore db;

    // put in every activity which can send user to generate content (can make this a class that evey activity extends from in future)
    FirebaseAuth mAuth;
    boolean loggedIn = false;

    Button btnGoAuth;
    Button btnGoAddPark;
    Button btnGoInfo;
    Button btnGoReviews;

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

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.map_container, new MapsActivity());
            fragmentTransaction.commit();
        }
    }


}