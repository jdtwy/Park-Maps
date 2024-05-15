package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    FirebaseAuth mAuth;

    EditText inputEmailView;
    EditText inputUsernameView;
    EditText inputPasswordView;
    Button btnCreateUser;
    Button btnGoLogin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        inputEmailView = findViewById(R.id.inputEmail);
        inputUsernameView = findViewById(R.id.inputUsername);
        inputPasswordView = findViewById(R.id.inputPassword);
        btnCreateUser = findViewById(R.id.btnCreateUser);
        btnGoLogin = findViewById(R.id.btnGoLogin);

        btnCreateUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a new account
                String email = inputEmailView.getText().toString();
                String username = inputUsernameView.getText().toString();
                String password = inputPasswordView.getText().toString();

                if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(Register.this, "Email/Username/Password cannot be empty.", Toast.LENGTH_SHORT).show();
                } else {
                    // add handling for username already existing

                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(Register.this, "Account created.", Toast.LENGTH_SHORT).show();
                                        FirebaseUser user = mAuth.getCurrentUser();

                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(username)
                                                .build();

                                        user.updateProfile(profileUpdates)
                                                .addOnCompleteListener(profileTask -> {
                                                    if (profileTask.isSuccessful()) {
                                                        // Username added
                                                        Toast.makeText(Register.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                                                        // Take user to MainActivity
                                                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                                    } else {
                                                        // Failed to add username
                                                        Toast.makeText(Register.this, "Account failed to create.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Toast.makeText(Register.this, "Account failed to create.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        btnGoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Take user to Login
                startActivity(new Intent(getApplicationContext(), Login.class));
            }
        });
    }

    /* OLD
    private void addUser() {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("password", password);
        user.put("parksAdded", new ArrayList<>()); // collection of park ids
        user.put("reviewsAdded", new ArrayList<>()); // collection of review ids
        user.put("profilePic", null);
        user.put("creationTime", Timestamp.now());

        db.collection("users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("MYDEBUG", "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("MYDEBUG", "Error adding document", e);
                    }
                });
    }*/
}