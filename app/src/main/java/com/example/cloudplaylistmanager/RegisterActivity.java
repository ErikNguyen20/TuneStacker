package com.example.cloudplaylistmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.cloudplaylistmanager.Utils.FirebaseManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private static final String LOG_TAG = "RegisterActivity";

    FirebaseAuth authentication = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        this.authentication = FirebaseAuth.getInstance();

        RegisterUser("20nguyened@gmail.com","Password123");
    }

    public void OnSuccess() {
        FirebaseManager.getInstance().InitializeUserData();
        //Launch Activity to landing page.
    }

    public void RegisterUser(String email, String password) {
        if(email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,"Fields cannot be empty.",Toast.LENGTH_SHORT).show();
        }
        else if(password.length() < 6) {
            Toast.makeText(this,"Password Length must be at least 6 characters long.",Toast.LENGTH_SHORT).show();
        }
        else if(this.authentication != null) {
            this.authentication.createUserWithEmailAndPassword(email, password).addOnCompleteListener(RegisterActivity.this, task -> {
                if(task.isSuccessful()) {
                    Toast.makeText(this,"User successfully registered.",Toast.LENGTH_LONG).show();
                    OnSuccess();
                    Log.d(LOG_TAG,"User Successfully Registered.");
                }
                else {
                    if(task.getException() != null) {
                        String errorMsg = (task.getException().getMessage() != null) ? task.getException().getMessage() : "An Error has Occurred";
                        Toast.makeText(this,"User failed to register.",Toast.LENGTH_LONG).show();
                        Log.e(LOG_TAG, errorMsg);
                    }
                    else {
                        Log.e(LOG_TAG,"An Error has Occurred");
                    }
                }
            });
        }
    }
}