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
import android.app.ProgressDialog;
import android.content.Intent;
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
import com.example.cloudplaylistmanager.Utils.DownloadListener;
import com.example.cloudplaylistmanager.Utils.DownloadPlaylistListener;
import com.example.cloudplaylistmanager.Utils.PlatformCompatUtility;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

public class AddNewPopupSingleActivity extends AppCompatActivity {
    public static final String PARENT_UUID_KEY_TAG = "parent_uuid_key";
    public static final String IS_PLAYLIST_TAG = "is_playlist";
    private static final String TOAST_KEY = "toast_key";

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
    private final Handler toastHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Toast.makeText(getApplicationContext(),msg.getData().getString(TOAST_KEY),Toast.LENGTH_LONG).show();
        }
    };

    TextView downloadButton;
    TextView chooseButton;
    TextView fileName;
    EditText urlField;

    private String uuidParentKey;
    private boolean isPlaylist;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.uuidParentKey = getIntent().getStringExtra(PARENT_UUID_KEY_TAG);
        this.isPlaylist = getIntent().getBooleanExtra(IS_PLAYLIST_TAG,false);

        if(!this.isPlaylist) {
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
                    DownloadAudio();
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
            setContentView(R.layout.activity_add_new_popup_single_playlist);

            this.downloadButton = findViewById(R.id.textView_download_button);
            this.urlField = findViewById(R.id.edit_text_url);

            this.downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DownloadPlaylist();
                }
            });
        }

        setFinishOnTouchOutside(true);
    }

    private void DownloadPlaylist() {
        String urlInput = this.urlField.getText().toString();
        if(urlInput.isEmpty() || PlatformCompatUtility.PlaylistUrlSource(urlInput) == PlatformCompatUtility.Platform.UNKNOWN) {
            SendToast("Invalid Url.");
            return;
        }
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Downloading");
        progressDialog.setMessage("Preparing for download...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
        PlatformCompatUtility.DownloadPlaylist(urlInput, new DownloadPlaylistListener() {
            @Override
            public void onComplete(PlaylistInfo playlist) {
                DataManager.getInstance().CreateNewPlaylist(playlist,false,null);
                progressDialog.dismiss();
                SendToast("Complete!");
                finish();
            }

            @Override
            public void onProgressUpdate(int progress, int outOf) {
                progressDialog.setMessage("Download Progress: " + progress + "/" + outOf);
            }

            @Override
            public void onError(int attempt, String error) {
                if(attempt == -1) {
                    progressDialog.dismiss();
                    SendToast(error);
                }
                else {
                    progressDialog.setMessage(error);
                }
            }
        });

    }

    private void DownloadAudio() {
        Uri uri = this.selectedFile.getValue();
        String urlInput = this.urlField.getText().toString();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Downloading");
        progressDialog.setMessage("Preparing for download...");
        progressDialog.setCanceledOnTouchOutside(false);

        if(uri != null) {
            progressDialog.show();
            DataManager.getInstance().DownloadFileFromDirectoryToDirectory(uri, new DownloadListener() {
                @Override
                public void onComplete(PlaybackAudioInfo audio) {
                    progressDialog.dismiss();
                    SendToast("Complete!");
                    finish();
                }

                @Override
                public void onProgressUpdate(float progress, long etaSeconds) {}

                @Override
                public void onError(int attempt, String error) {
                    progressDialog.dismiss();
                    SendToast(error);
                }
            });
        }
        else if(!urlInput.isEmpty()){
            progressDialog.show();
            DataManager.getInstance().DownloadSongToDirectoryFromUrl(urlInput, new DownloadListener() {
                @Override
                public void onComplete(PlaybackAudioInfo audio) {
                    progressDialog.dismiss();
                    SendToast("Complete!");
                    finish();
                }

                @Override
                public void onProgressUpdate(float progress, long etaSeconds) {
                    progressDialog.setMessage("Download Progress: " + progress);
                }

                @Override
                public void onError(int attempt, String error) {
                    if(attempt == -1) {
                        progressDialog.dismiss();
                        SendToast(error);
                    }
                    else {
                        progressDialog.setMessage("Failed, retrying download. Retrying: " + attempt);
                    }
                }
            });
        }
        else {
            SendToast("Please select a means of adding an Audio Source.");
        }
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
}