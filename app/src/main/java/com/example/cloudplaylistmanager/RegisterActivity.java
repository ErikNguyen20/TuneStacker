package com.example.cloudplaylistmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    FirebaseAuth authentication = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        this.authentication = FirebaseAuth.getInstance();

        //RegisterUser("20nguyened@gmail.com","Password123");
    }

    public void OnSuccess() {

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
                }
                else {
                    Toast.makeText(this,"User failed to register.",Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}