package com.example.cloudplaylistmanager;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.example.cloudplaylistmanager.Utils.DataManager;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

import java.lang.ref.WeakReference;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MainActivity";
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final long UPDATE_CHECK_COOLDOWN_MILLISECONDS = 1000 * 60 * 60 * 24 * 3;
                                                                //millisec*sec*min*hour*day

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Removes the action bar.
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.hide();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Checks and Prompts for read/write storage permissions.
        if(!CheckPermission()) {
            RequestPermission();
        }
        else {
            LaunchLandingActivity();
        }
    }

    /**
     * Launches the Landing Activity.
     * This also loads the user's saved data.
     */
    public void LaunchLandingActivity() {
        //Initializes the saved data for the application.
        DataManager.Initialize(getApplicationContext());
        DataManager.getInstance().ConstructPlaylistFromLocalFiles();

        long last = DataManager.getInstance().GetLastUpdateTime();
        long current = new Date().getTime();
        if(Math.abs(current - last) >= UPDATE_CHECK_COOLDOWN_MILLISECONDS) {
            MainActivity.AsyncYoutubeDlUpdater request = new AsyncYoutubeDlUpdater(this);
            request.execute();
        }
        else {
            startActivity(new Intent(MainActivity.this,LandingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    /**
     * Checks to see if the user has granted read/write permissions to the app.
     */
    public boolean CheckPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        else {
            int write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Requests read/write permissions from the user.
     */
    private void RequestPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try{
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);
            }
            catch (Exception e) {
                e.printStackTrace();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    private final ActivityResultLauncher<Intent> storageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if(Environment.isExternalStorageManager()) {
                        Log.d("Permissions", "Storage Permission Granted");
                        LaunchLandingActivity();
                    }
                    else {
                        Log.d("Permissions", "Storage Permission Denied");
                        Toast.makeText(getApplicationContext(),"Please enable Storage Permissions to use this application.",Toast.LENGTH_LONG).show();
                    }
                }
            });

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == STORAGE_PERMISSION_CODE) {
            if(grantResults.length > 0) {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permissions", "Storage Permission Granted");
                    LaunchLandingActivity();
                }
                else {
                    Log.d("Permissions", "Storage Permission Denied");
                    Toast.makeText(getApplicationContext(),"Please enable Storage Permissions to use this application.",Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * AsyncTask class that asynchronously checks/updates the yt-dlp binary.
     * Extends {@link AsyncTask}
     */
    private static class AsyncYoutubeDlUpdater extends AsyncTask<Boolean, Integer, Integer>
    {
        WeakReference<MainActivity> activity;
        WeakReference<ProgressDialog> progress;

        // Important: DataManager must be initialized first
        public AsyncYoutubeDlUpdater(MainActivity activity)
        {
            this.activity = new WeakReference<>(activity);

            ProgressDialog dialog = new ProgressDialog(activity);
            dialog.setTitle("Checking for Update");
            dialog.setMessage("If this is taking while, then an update is being downloaded...");
            this.progress = new WeakReference<>(dialog);
        }

        @Override
        protected void onPreExecute() {
            if(this.activity.get() != null && this.progress.get() != null) {
                this.progress.get().show();
            }
        }

        @Override
        protected Integer doInBackground(Boolean... booleans) {
            try {
                YoutubeDL.UpdateStatus status = YoutubeDL.getInstance().updateYoutubeDL(activity.get());
                if(status == YoutubeDL.UpdateStatus.DONE) {
                    return 1;
                }
                else {
                    return 0;
                }
            } catch (YoutubeDLException e) {
                Log.e(LOG_TAG, "Failed to update YoutubeDL");
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer status) {
            if(this.activity.get() != null) {
                MainActivity context = this.activity.get();
                if(this.progress.get() != null) {
                    this.progress.get().dismiss();
                }

                if(status == 1) {
                    Toast.makeText(context.getApplicationContext(),"Yt-Dlp Update Downloaded.", Toast.LENGTH_LONG).show();
                }
                // Updates last update check so it won't do it again for a certain time-span
                if(status != -1) {
                    DataManager.getInstance().SaveLastUpdateCheck();
                }

                context.startActivity(new Intent(context,LandingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        }
    }
}