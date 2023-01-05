package com.example.cloudplaylistmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.cloudplaylistmanager.Utils.FirebaseManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private static final String LOG_TAG = "RegisterActivity";

    private FirebaseAuth authentication = null;

    private EditText emailEdit;
    private EditText passwordEdit;
    private Button registerButton;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        this.authentication = FirebaseAuth.getInstance();

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_new_24);
            actionBar.setTitle("Back");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        this.emailEdit = findViewById(R.id.edit_text_email);
        this.passwordEdit = findViewById(R.id.edit_text_password);
        this.registerButton = findViewById(R.id.button_register_confirm);
        this.progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Registering.");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        this.registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEdit.getText().toString();
                String password = passwordEdit.getText().toString();

                RegisterUser(email, password);
            }
        });


        //RegisterUser("20nguyened@gmail.com","Password123");
    }

    public void OnSuccess() {
        FirebaseManager.getInstance().InitializeUserData();
        startActivity(new Intent(RegisterActivity.this,LandingActivity.class));
        this.finish();
    }

    public void RegisterUser(String email, String password) {
        if(email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,"Fields cannot be empty.",Toast.LENGTH_SHORT).show();
        }
        else if(password.length() < 6) {
            Toast.makeText(this,"Password Length must be at least 6 Characters Long.",Toast.LENGTH_SHORT).show();
        }
        else if(this.authentication != null) {
            this.progressDialog.show();
            this.authentication.createUserWithEmailAndPassword(email, password).addOnCompleteListener(RegisterActivity.this, task -> {
                this.progressDialog.hide();
                if(task.isSuccessful()) {
                    Toast.makeText(this,"User successfully registered.",Toast.LENGTH_LONG).show();
                    OnSuccess();
                    Log.d(LOG_TAG,"User Successfully Registered.");
                }
                else {
                    if(task.getException() != null) {
                        String errorMsg = (task.getException().getMessage() != null) ? task.getException().getMessage() : "An Error has Occurred";
                        Toast.makeText(this,errorMsg,Toast.LENGTH_LONG).show();
                        Log.e(LOG_TAG, errorMsg);
                    }
                    else {
                        Toast.makeText(this,"User failed to register.",Toast.LENGTH_LONG).show();
                        Log.e(LOG_TAG,"User failed to register.");
                    }
                }
            });
        }
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}