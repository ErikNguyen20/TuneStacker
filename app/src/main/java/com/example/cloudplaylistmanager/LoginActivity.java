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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private static final String LOG_TAG = "LoginActivity";

    private FirebaseAuth authentication = null;

    private EditText emailEdit;
    private EditText passwordEdit;
    private Button loginButton;
    private Button forgotPasswordButton;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        this.authentication = FirebaseAuth.getInstance();

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_new_24);
            actionBar.setTitle("Back");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        this.emailEdit = findViewById(R.id.edit_text_email);
        this.passwordEdit = findViewById(R.id.edit_text_password);
        this.loginButton = findViewById(R.id.button_login_confirm);
        this.forgotPasswordButton = findViewById(R.id.button_forgot_password);
        this.progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Logging in.");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        this.loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEdit.getText().toString();
                String password = passwordEdit.getText().toString();

                Login(email, password);
            }
        });

        this.forgotPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Launch Activity to forgot password activity.
            }
        });

    }

    public void OnSuccess() {
        startActivity(new Intent(LoginActivity.this,LandingActivity.class));
        this.finish();
    }

    public void Login(String email, String password) {
        if(email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,"Field(s) cannot be empty.",Toast.LENGTH_SHORT).show();
        }
        else if(this.authentication != null) {
            this.progressDialog.show();
            this.authentication.signInWithEmailAndPassword(email, password).addOnCompleteListener(LoginActivity.this, task -> {
                this.progressDialog.hide();
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

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}