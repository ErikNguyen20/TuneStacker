package com.example.cloudplaylistmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private static final String LOG_TAG = "LoginActivity";

    FirebaseAuth authentication = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.authentication = FirebaseAuth.getInstance();
    }

    public void OnSuccess() {
        //Launch Activity to landing page.
    }

    public void Login(String email, String password) {
        if(email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,"Field(s) cannot be empty.",Toast.LENGTH_SHORT).show();
        }
        else if(this.authentication != null) {
            this.authentication.signInWithEmailAndPassword(email, password).addOnCompleteListener(LoginActivity.this, task -> {
                if(task.isSuccessful()) {
                    Toast.makeText(this,"User successfully logged in.",Toast.LENGTH_LONG).show();
                    OnSuccess();
                }
                else {
                    if(task.getException() != null) {
                        String errorMsg = (task.getException().getMessage() != null) ? task.getException().getMessage() : "Authentication Failed";
                        Toast.makeText(this,errorMsg,Toast.LENGTH_LONG).show();
                        Log.e(LOG_TAG, errorMsg);
                    }
                    else {
                        Toast.makeText(this,"Authentication Failed.",Toast.LENGTH_LONG).show();
                        Log.e(LOG_TAG,"Authentication Failed.");
                    }
                }
            });
        }
    }
}