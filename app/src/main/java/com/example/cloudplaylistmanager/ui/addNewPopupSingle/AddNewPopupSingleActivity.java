package com.example.cloudplaylistmanager.ui.addNewPopupSingle;

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Downloader.DownloadListener;
import com.example.cloudplaylistmanager.Downloader.DownloadService;
import com.example.cloudplaylistmanager.Utils.PlatformCompatUtility;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;

/**
 * Popup Dialog Activity that implements the interface to download a new Song or Playlist.
 * IntentExtra IS_PLAYLIST_TAG must be sent to determine if the popup will be for adding
 * an audio file or for adding a playlist.
 */
public class AddNewPopupSingleActivity extends AppCompatActivity {
    private static final String LOG_TAG = "PopupSingleActivity";
    public static final String PARENT_UUID_KEY_TAG = "parent_uuid_key";
    public static final String IS_PLAYLIST_TAG = "is_playlist";
    private static final String TOAST_KEY = "toast_key";
    private static final String PROGRESS_DIALOG_SHOW_KEY = "show_dialog_key";
    private static final String PROGRESS_DIALOG_HIDE_KEY = "hide_dialog_key";
    private static final String PROGRESS_DIALOG_UPDATE_KEY = "update_dialog_key";

    private TextView downloadButton;
    private TextView chooseButton;
    private TextView fileName;
    private EditText urlField;
    private ProgressDialog progressDialog;

    private BroadcastReceiver updateUIReceiver;

    private String uuidParentKey;
    private boolean isPlaylist;


    /**
     * OnCreate Method sets up the UI.
     * @param savedInstanceState bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Gets data from the intent extras.
        this.uuidParentKey = getIntent().getStringExtra(PARENT_UUID_KEY_TAG);
        this.isPlaylist = getIntent().getBooleanExtra(IS_PLAYLIST_TAG,false);

        if(!this.isPlaylist) {
            //Loads the New Song Popup.
            setContentView(R.layout.activity_add_new_popup_single_song);

            this.downloadButton = findViewById(R.id.textView_download_button);
            this.chooseButton = findViewById(R.id.textView_choose_file);
            this.fileName = findViewById(R.id.textView_file_name);
            this.urlField = findViewById(R.id.edit_text_url);

            //Sets the click listener for the choose button, which opens the file explorer.
            this.chooseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Launches intent to open the file explorer.
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("audio/*");
                    intent = Intent.createChooser(intent, "Choose an Audio File");

                    activityResultLauncher.launch(intent);
                }
            });
            //Sets the click listener for the download button.
            this.downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(GetConnectivityStatus() == ConnectivityManager.TYPE_MOBILE) {
                        //If the device is using mobile data, then prompt the user with a popup.
                        Dialog dialog = new Dialog(AddNewPopupSingleActivity.this);
                        dialog.setContentView(R.layout.popup_confirm_button);

                        TextView continueButton = dialog.findViewById(R.id.textView_continue_button);
                        continueButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //Downloads the audio.
                                dialog.dismiss();
                                DownloadAudio();
                            }
                        });

                        dialog.show();
                    }
                    else {
                        //Downloads the audio.
                        DownloadAudio();
                    }
                }
            });

            //Listens for the Uri change, which is triggered by the activitylauncher callback.
            getUriResult().observe(this, new Observer<Uri>() {
                public void onChanged(Uri uri) {
                    if(uri == null) {
                        return;
                    }
                    //Sets the UI display that shows the user the file name.
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

            //Sets the click listener for the download button.
            this.downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(GetConnectivityStatus() == ConnectivityManager.TYPE_MOBILE) {
                        //If the device is using mobile data, then prompt the user with a popup.
                        Dialog dialog = new Dialog(getApplicationContext());
                        dialog.setContentView(R.layout.popup_confirm_button);

                        TextView continueButton = dialog.findViewById(R.id.textView_continue_button);
                        continueButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //Downloads the playlist.
                                dialog.dismiss();
                                DownloadPlaylist();
                            }
                        });

                        dialog.show();
                    }
                    else {
                        //Downloads the playlist.
                        DownloadPlaylist();
                    }
                }
            });
        }

        setFinishOnTouchOutside(true);

        //Sets up progress dialog.
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setTitle("Downloading");
        this.progressDialog.setCanceledOnTouchOutside(false);
        this.progressDialog.setCancelable(false);

        //Sets up broadcast receiver for download updates.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.BROADCAST_ACTION_IDENTIFIER);
        this.updateUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent == null) {
                    return;
                }
                String broadcastType = intent.getStringExtra(DownloadService.BROADCAST_ACTION_IDENTIFIER);
                switch(broadcastType) {
                    case DownloadService.BROADCAST_NOTIFICATION_COMPLETE:
                        //Notify the user that the download was complete.
                        HideProgressDialog();
                        SendToast("Download Complete!");
                        finish();
                        break;
                    case DownloadService.BROADCAST_NOTIFICATION_PROGRESS_MESSAGE:
                        String messageProgress = intent.getStringExtra(DownloadService.BROADCAST_NOTIFICATION_PROGRESS_MESSAGE);
                        //Notify the user of the download progress.
                        if(messageProgress != null && !messageProgress.isEmpty()) {
                            UpdateProgressDialog(messageProgress);
                        }
                        break;
                    case DownloadService.BROADCAST_NOTIFICATION_ERROR:
                        int errorCode = intent.getIntExtra(DownloadService.BROADCAST_NOTIFICATION_ERROR_CODE,0);
                        String messageError = intent.getStringExtra(DownloadService.BROADCAST_NOTIFICATION_ERROR_MESSAGE);
                        if(errorCode == -1) {
                            //If there is a critical error, display the error and stop the download.
                            HideProgressDialog();
                            SendToast(messageError);
                        }
                        else {
                            UpdateProgressDialog(messageError);
                        }
                        break;
                }
            }
        };
        registerReceiver(this.updateUIReceiver, filter);
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

        //Downloads playlist using the PlatformCompactUtility in DownloadService.
        Intent serviceIntent = new Intent(this, DownloadService.class);
        serviceIntent.putExtra(DownloadService.INTENT_URL_TAG, urlInput);
        serviceIntent.putExtra(DownloadService.INTENT_PLAYLIST_TAG,true);
        startService(serviceIntent);
    }

    /**
     * Downloads the song that was inputted in the url field or that was selected
     * as a file.
     */
    private void DownloadAudio() {
        Uri uri = this.selectedFile.getValue();
        String urlInput = this.urlField.getText().toString();

        if(uri != null) {
            //Transfers the selected file from the file system into the application.
            StartProgressDialog("Preparing for download...");
            DataManager.getInstance().DownloadFileFromDirectoryToDirectory(uri, new DownloadListener() {
                @Override
                public void onComplete(PlaybackAudioInfo audio) {
                    //When the audio is successfully downloaded, add the audio to DataManager.
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
                    //If there is an error, display the error and stop the transfer.
                    HideProgressDialog();
                    SendToast(error);
                }
            });
        }
        else if(!urlInput.isEmpty()){
            //Checks if download service has been bound.

            StartProgressDialog("Preparing for download...");

            //Downloads audio using the PlatformCompactUtility.
            Intent serviceIntent = new Intent(this, DownloadService.class);
            serviceIntent.putExtra(DownloadService.INTENT_URL_TAG, urlInput);
            serviceIntent.putExtra(DownloadService.INTENT_PLAYLIST_TAG,false);
            startService(serviceIntent);
        }
        else {
            SendToast("Please select a means of adding an Audio Source.");
        }
    }


    /**
     * Gets the connectivity status of the application.
     * @return Connectivity Code. {@link ConnectivityManager}
     */
    public int GetConnectivityStatus() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.isConnected()) {
            return networkInfo.getType();
        }
        return -1;
    }

    /**
     * Returns the Uri result from the file selection activity launcher.
     * @return Uri results from the activitylauncher.
     */
    private LiveData<Uri> getUriResult() {
        return this.selectedFile;
    }

    /**
     * Sends a toast message using the handler.
     * @param message String message that will be sent.
     */
    public void SendToast(String message) {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(TOAST_KEY,message);
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }

    /**
     * Starts a progress dialog with an initial message.
     * @param message String message that will be sent.
     */
    public void StartProgressDialog(String message) {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putBoolean(PROGRESS_DIALOG_SHOW_KEY,true);
        bundle.putString(PROGRESS_DIALOG_UPDATE_KEY,message);
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }

    /**
     * Updates the progress dialog with a message.
     * @param message String message that will be sent.
     */
    public void UpdateProgressDialog(String message) {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(PROGRESS_DIALOG_UPDATE_KEY,message);
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }

    /**
     * Hides the progress dialog.
     */
    public void HideProgressDialog() {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putBoolean(PROGRESS_DIALOG_HIDE_KEY,true);
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }

    @Override
    protected void onDestroy() {
        if(this.progressDialog != null) {
            this.progressDialog.dismiss();
        }
        if(this.updateUIReceiver != null) {
            unregisterReceiver(this.updateUIReceiver);
        }
        super.onDestroy();
    }

    //Activity Result callback used for retrieving the selected file from the user.
    private final MutableLiveData<Uri> selectedFile = new MutableLiveData<>();
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == Activity.RESULT_OK) {
                    Intent resultIntent = result.getData();
                    if(resultIntent != null) {
                        Uri uri = resultIntent.getData();
                        selectedFile.setValue(uri);
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
}