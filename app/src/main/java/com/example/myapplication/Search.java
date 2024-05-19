package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Search extends AppCompatActivity {
    EditText inputSearchBar;
    LinearLayout layoutAllLocations;
    TextView textSearchState;
    FirebaseFirestore db;

    private final static String TAG = "DEBUG";
    private final List<Map<String,Object>> parkInfo = new ArrayList<Map<String,Object>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.d(TAG, "search has been created");

        db = FirebaseFirestore.getInstance();

        queryDatabaseForParks();

        inputSearchBar = findViewById(R.id.inputSearchBar);
        layoutAllLocations = findViewById(R.id.layoutAllLocations);
        textSearchState = findViewById(R.id.textSearchState);

        inputSearchBar.addTextChangedListener(new TextWatcher() {
            //necessary abstract method to override
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textSearchState.setVisibility(View.INVISIBLE);
                String searchText = s.toString();

                performSearch(searchText);
            }
            //necessary abstract method to override
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void queryDatabaseForParks() {
        final Executor executor = Executors.newSingleThreadExecutor();
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
                                        Log.d(TAG, map.toString());
                                        parkInfo.add(map);
                                    }
                                }
                            }
                        });
            }
        });
    }

    private void performSearch(String searchText) {
        //set visibility of all layouts to invisible
        setToInvisible();
        //find all parks that match the search string
        List<Map<String,Object>> foundParks = new ArrayList<Map<String,Object>>();
        for (Map<String, Object> map : parkInfo) {
            if (Objects.equals((String) map.get("parkName"), searchText)) {
                foundParks.add(map);
                Log.d(TAG, foundParks.toString());
            }
        }
        layoutAllLocations = findViewById(R.id.layoutAllLocations);
        for (int i = 0; i < foundParks.size(); i++) {
            View layout = layoutAllLocations.getChildAt(i);
            layout.setVisibility(View.VISIBLE);
            TextView parkNameView = layout.findViewById(R.id.parkName);
            TextView parkCoordinantsView = layout.findViewById(R.id.parkCoordinants);
            String parkName = (String) foundParks.get(i).get("parkName");
            String latitude = (String) foundParks.get(i).get("latitude");
            String longitude = (String) foundParks.get(i).get("longitude");
            String parkCoordinants = latitude + " " + longitude;
            parkNameView.setText(parkName);
            parkCoordinantsView.setText(parkCoordinants);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class)
                            .putExtra("latitude", latitude)
                            .putExtra("longitude", longitude)
                    );
                }
            });
        }
    }

    //Helper method
    private void setToInvisible() {
        for (int i = 0; i < layoutAllLocations.getChildCount(); i++) {
            View layout = layoutAllLocations.getChildAt(i);
            layout.setVisibility(View.INVISIBLE);
        }
    }
}