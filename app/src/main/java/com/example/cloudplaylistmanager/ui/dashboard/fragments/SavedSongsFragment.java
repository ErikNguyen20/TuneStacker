package com.example.cloudplaylistmanager.ui.dashboard.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cloudplaylistmanager.MainActivity;
import com.example.cloudplaylistmanager.MusicPlayer.MediaPlayerActivity;
import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewItemClickedListener;
import com.example.cloudplaylistmanager.RecyclerAdapters.SongsRecyclerAdapter;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.DownloadListener;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.ui.dashboard.DashboardViewModel;

import java.io.File;

/**
 * A simple {@link Fragment} subclass.
 */
public class SavedSongsFragment extends Fragment {
    private static final String TOAST_KEY = "toast_key";

    private PlaylistInfo savedSongs;
    private SongsRecyclerAdapter songsAdapter;
    private DashboardViewModel viewModel;

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
            Toast.makeText(getContext(),msg.getData().getString(TOAST_KEY),Toast.LENGTH_LONG).show();
        }
    };



    public SavedSongsFragment() {
        this.savedSongs = new PlaylistInfo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_songs, container, false);

        //Instantiates new Recycler View and sets the adapter.
        RecyclerView songRecyclerView = view.findViewById(R.id.recyclerView_saved_songs);
        songRecyclerView.setHasFixedSize(true);
        songRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        //Implement a listener to capture clicks on the recycler view items.
        this.songsAdapter = new SongsRecyclerAdapter(getContext(), this.savedSongs, true, new RecyclerViewItemClickedListener() {
            @Override
            public void onClicked(int viewType, int position) {
                if(viewType != SongsRecyclerAdapter.ADD_ITEM_TOKEN) {
                    Intent intent = new Intent(getActivity(),MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,savedSongs);
                    intent.putExtra(MediaPlayerActivity.POSITION_TAG,position);
                    Log.e("ClickedPosition",String.valueOf(position));
                    startActivity(intent);
                }
                else {
                    LaunchDialogPopup();
                }
            }
        });
        songRecyclerView.setAdapter(this.songsAdapter);

        //Fetches data from the ViewModel.
        this.viewModel = new ViewModelProvider(requireParentFragment()).get(DashboardViewModel.class);
        this.viewModel.getLocalVideos().observe(getViewLifecycleOwner(), new Observer<PlaylistInfo>() {
            @Override
            public void onChanged(PlaylistInfo playlistInfo) {
                if(playlistInfo == null) {
                    return;
                }
                savedSongs = playlistInfo;
                songsAdapter.updateData(playlistInfo);
            }
        });
        return view;
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

    public void LaunchDialogPopup() {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.create_new_song_popup);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView downloadButton = dialog.findViewById(R.id.textView_download_button);
        TextView chooseButton = dialog.findViewById(R.id.textView_choose_file);
        TextView fileName = dialog.findViewById(R.id.textView_file_name);
        EditText urlField = dialog.findViewById(R.id.edit_text_url);
        this.selectedFile.setValue(null);

        getUriResult().observe(this, new Observer<Uri>() {
            public void onChanged(Uri uri) {
                if(uri == null) {
                    return;
                }

                String uriName = DataManager.getInstance().GetFileNameFromUri(uri);
                uriName = uriName == null ? uri.getLastPathSegment() : uriName;
                fileName.setText(uriName);
                Log.d("FileSelected",uriName);
            }
        });

        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent = Intent.createChooser(intent, "Choose an Audio File");

                activityResultLauncher.launch(intent);
            }
        });
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = selectedFile.getValue();
                String urlInput = urlField.getText().toString();
                ProgressDialog progressDialog = new ProgressDialog(getContext());
                progressDialog.setTitle("Downloading");
                progressDialog.setMessage("Preparing for download...");
                progressDialog.setCanceledOnTouchOutside(false);

                if(uri != null) {
                    progressDialog.show();
                    DataManager.getInstance().DownloadFileFromDirectoryToDirectory(uri, new DownloadListener() {
                        @Override
                        public void onComplete(PlaybackAudioInfo audio) {
                            viewModel.updateData();
                            progressDialog.dismiss();
                            SendToast("Complete!");
                        }

                        @Override
                        public void onProgressUpdate(float progress, long etaSeconds) {}

                        @Override
                        public void onError(int attempt, String error) {
                            progressDialog.dismiss();
                            SendToast(error);
                        }
                    });
                    dialog.dismiss();
                }
                else if(!urlInput.isEmpty()){
                    progressDialog.show();
                    DataManager.getInstance().DownloadSongToDirectoryFromUrl(urlInput, new DownloadListener() {
                        @Override
                        public void onComplete(PlaybackAudioInfo audio) {
                            viewModel.updateData();
                            progressDialog.dismiss();
                            SendToast("Complete!");
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
                    dialog.dismiss();
                }
                else {
                    SendToast("Please select a means of adding an Audio Source.");
                }
            }
        });

        dialog.show();
    }
}