package com.example.cloudplaylistmanager.ui.addNewPopupSingle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.DownloadListener;
import com.example.cloudplaylistmanager.Utils.DownloadPlaylistListener;
import com.example.cloudplaylistmanager.Utils.PlatformCompatUtility;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

public class AddNewPopupSingleActivity extends AppCompatActivity {
    private static final String LOG_TAG = "PopupSingleActivity";
    private static final String WAKE_LOCK_TAG = "popup:single";
    public static final String PARENT_UUID_KEY_TAG = "parent_uuid_key";
    public static final String IS_PLAYLIST_TAG = "is_playlist";
    private static final String TOAST_KEY = "toast_key";
    private static final String PROGRESS_DIALOG_SHOW_KEY = "show_dialog_key";
    private static final String PROGRESS_DIALOG_HIDE_KEY = "hide_dialog_key";
    private static final String PROGRESS_DIALOG_UPDATE_KEY = "update_dialog_key";

    //Activity Result callback used for retrieving the selected file from the user.
    private final MutableLiveData<Uri> selectedFile = new MutableLiveData<>();
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        Intent resultIntent = result.getData();
                        if(resultIntent != null) {
                            Uri uri = resultIntent.getData();
                            selectedFile.setValue(uri);
                        }
                    }
                }
            });
    //Handler that handles toast and progress dialog messages.
    private final Handler toastHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.getData().getString(TOAST_KEY) != null && !msg.getData().getString(TOAST_KEY).isEmpty()) {
                Toast.makeText(getApplicationContext(),msg.getData().getString(TOAST_KEY),Toast.LENGTH_LONG).show();
            }
            if(msg.getData().getString(PROGRESS_DIALOG_UPDATE_KEY) != null && !msg.getData().getString(PROGRESS_DIALOG_UPDATE_KEY).isEmpty()) {
                progressDialog.setMessage(msg.getData().getString(PROGRESS_DIALOG_UPDATE_KEY));
            }
            if(msg.getData().getBoolean(PROGRESS_DIALOG_SHOW_KEY)) {
                progressDialog.show();
            }
            if(msg.getData().getBoolean(PROGRESS_DIALOG_HIDE_KEY)) {
                progressDialog.hide();
            }
        }
    };

    private TextView downloadButton;
    private TextView chooseButton;
    private TextView fileName;
    private EditText urlField;
    private ProgressDialog progressDialog;

    private String uuidParentKey;
    private boolean isPlaylist;

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.uuidParentKey = getIntent().getStringExtra(PARENT_UUID_KEY_TAG);
        this.isPlaylist = getIntent().getBooleanExtra(IS_PLAYLIST_TAG,false);

        if(!this.isPlaylist) {
            //Loads the New Song Popup.
            setContentView(R.layout.activity_add_new_popup_single_song);

            this.downloadButton = findViewById(R.id.textView_download_button);
            this.chooseButton = findViewById(R.id.textView_choose_file);
            this.fileName = findViewById(R.id.textView_file_name);
            this.urlField = findViewById(R.id.edit_text_url);


            this.chooseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("audio/*");
                    intent = Intent.createChooser(intent, "Choose an Audio File");

                    activityResultLauncher.launch(intent);
                }
            });
            this.downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(GetConnectivityStatus() == ConnectivityManager.TYPE_MOBILE) {
                        Dialog dialog = new Dialog(getApplicationContext());
                        dialog.setContentView(R.layout.popup_confirm_button);

                        TextView continueButton = dialog.findViewById(R.id.textView_continue_button);
                        continueButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                                DownloadAudio();
                            }
                        });

                        dialog.show();
                    }
                    else {
                        DownloadAudio();
                    }
                }
            });

            getUriResult().observe(this, new Observer<Uri>() {
                public void onChanged(Uri uri) {
                    if(uri == null) {
                        return;
                    }

                    String uriName = DataManager.getInstance().GetFileNameFromUri(uri);
                    uriName = uriName == null ? uri.getLastPathSegment() : uriName;
                    fileName.setText(uriName);
                }
            });
        }
        else {
            //Loads the Import Playlist popup.
            setContentView(R.layout.activity_add_new_popup_single_playlist);

            this.downloadButton = findViewById(R.id.textView_download_button);
            this.urlField = findViewById(R.id.edit_text_url);

            this.downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(GetConnectivityStatus() == ConnectivityManager.TYPE_MOBILE) {
                        Dialog dialog = new Dialog(getApplicationContext());
                        dialog.setContentView(R.layout.popup_confirm_button);

                        TextView continueButton = dialog.findViewById(R.id.textView_continue_button);
                        continueButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                                DownloadPlaylist();
                            }
                        });

                        dialog.show();
                    }
                    else {
                        DownloadPlaylist();
                    }
                }
            });
        }


        setFinishOnTouchOutside(true);

        //Locks that ensure that the device stays on during the download processes.
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,WAKE_LOCK_TAG);
        this.wifiLock = ((WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL,LOG_TAG);

        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setTitle("Downloading");
        this.progressDialog.setCanceledOnTouchOutside(false);
    }


    /**
     * Downloads the playlist that was inputted in the url field.
     */
    private void DownloadPlaylist() {
        String urlInput = this.urlField.getText().toString();
        if(urlInput.isEmpty() || PlatformCompatUtility.PlaylistUrlSource(urlInput) == PlatformCompatUtility.Platform.UNKNOWN) {
            SendToast("Invalid Url.");
            return;
        }

        StartProgressDialog("Preparing for download...");
        if(this.wakeLock != null && !this.wakeLock.isHeld()) {
            this.wakeLock.acquire(120*60*1000L /*120 minutes*/);
        }
        if(this.wifiLock != null && !this.wifiLock.isHeld()) {
            this.wifiLock.acquire();
        }
        PlatformCompatUtility.DownloadPlaylist(urlInput, new DownloadPlaylistListener() {
            @Override
            public void onComplete(PlaylistInfo playlist) {
                if(uuidParentKey != null && !uuidParentKey.isEmpty()) {
                    DataManager.getInstance().CreateNewPlaylist(playlist,false,uuidParentKey);
                }
                else {
                    DataManager.getInstance().CreateNewPlaylist(playlist,false,null);
                }
                HideProgressDialog();
                SendToast("Complete!");
                finish();
            }

            @Override
            public void onProgressUpdate(String message) {
                UpdateProgressDialog(message);
            }

            @Override
            public void onError(int attempt, String error) {
                if(attempt == -1) {
                    HideProgressDialog();
                    SendToast(error);
                    ReleaseLocks();
                }
                else {
                    UpdateProgressDialog(error);
                }
            }
        });

    }

    /**
     * Downloads the song that was inputted in the url field or that was selected
     * as a file.
     */
    private void DownloadAudio() {
        Uri uri = this.selectedFile.getValue();
        String urlInput = this.urlField.getText().toString();

        if(uri != null) {
            StartProgressDialog("Preparing for download...");
            DataManager.getInstance().DownloadFileFromDirectoryToDirectory(uri, new DownloadListener() {
                @Override
                public void onComplete(PlaybackAudioInfo audio) {
                    if(uuidParentKey != null && !uuidParentKey.isEmpty()) {
                        DataManager.getInstance().AddSongToPlaylist(uuidParentKey,audio);
                    }
                    HideProgressDialog();
                    SendToast("Complete!");
                    finish();
                }

                @Override
                public void onProgressUpdate(float progress, long etaSeconds) {}

                @Override
                public void onError(int attempt, String error) {
                    HideProgressDialog();
                    SendToast(error);
                    ReleaseLocks();
                }
            });
        }
        else if(!urlInput.isEmpty()){
            StartProgressDialog("Preparing for download...");
            if(this.wakeLock != null && !this.wakeLock.isHeld()) {
                this.wakeLock.acquire(5*60*1000L /*5 minutes*/);
            }
            if(this.wifiLock != null && !this.wifiLock.isHeld()) {
                this.wifiLock.acquire();
            }
            PlatformCompatUtility.DownloadSong(urlInput, new DownloadListener() {
                @Override
                public void onComplete(PlaybackAudioInfo audio) {
                    if(uuidParentKey != null && !uuidParentKey.isEmpty()) {
                        DataManager.getInstance().AddSongToPlaylist(uuidParentKey,audio);
                    }
                    HideProgressDialog();
                    SendToast("Complete!");
                    finish();
                }

                @Override
                public void onProgressUpdate(float progress, long etaSeconds) {
                    UpdateProgressDialog("Download Progress: " + progress);
                }

                @Override
                public void onError(int attempt, String error) {
                    if(attempt == -1) {
                        HideProgressDialog();
                        SendToast(error);
                        ReleaseLocks();
                    }
                    else {
                        UpdateProgressDialog("Failed, retrying download. Retrying: " + attempt);
                    }
                }
            });
        }
        else {
            SendToast("Please select a means of adding an Audio Source.");
        }
    }

    public int GetConnectivityStatus() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected()) {
            return networkInfo.getType();
        }
        return -1;
    }

    private LiveData<Uri> getUriResult() {
        return this.selectedFile;
    }

    public void SendToast(String message) {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(TOAST_KEY,message);
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }

    public void StartProgressDialog(String message) {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putBoolean(PROGRESS_DIALOG_SHOW_KEY,true);
        bundle.putString(PROGRESS_DIALOG_UPDATE_KEY,message);
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }

    public void UpdateProgressDialog(String message) {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(PROGRESS_DIALOG_UPDATE_KEY,message);
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }

    public void HideProgressDialog() {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putBoolean(PROGRESS_DIALOG_HIDE_KEY,true);
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }

    public void ReleaseLocks() {
        if(this.wakeLock != null && this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
        if(this.wifiLock != null && this.wifiLock.isHeld()) {
            this.wifiLock.release();
        }
    }

    @Override
    protected void onDestroy() {
        ReleaseLocks();
        if(this.progressDialog != null) {
            this.progressDialog.dismiss();
        }
        super.onDestroy();
    }
}