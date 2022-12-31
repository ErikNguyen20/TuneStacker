package com.example.cloudplaylistmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    FirebaseAuth authentication = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.authentication = FirebaseAuth.getInstance();
    }

    public void OnSuccess() {

    }

    public void Login(String email, String password) {
        if(email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,"Field(s) cannot be empty.",Toast.LENGTH_SHORT).show();
        }
        else {
            this.authentication.signInWithEmailAndPassword(email, password).addOnCompleteListener(LoginActivity.this, task -> {
                if(task.isSuccessful()) {
                    Toast.makeText(this,"User successfully logged in.",Toast.LENGTH_LONG).show();
                    OnSuccess();
                }
                else {
                    Toast.makeText(this,"Incorrect email or password.",Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}