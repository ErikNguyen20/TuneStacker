package com.example.cloudplaylistmanager.ui.playlistviewnormal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cloudplaylistmanager.MusicPlayer.MediaPlayerActivity;
import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewOptionsListener;
import com.example.cloudplaylistmanager.RecyclerAdapters.SongsOptionsRecyclerAdapter;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlatformCompatUtility;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.Utils.SyncPlaylistListener;
import com.example.cloudplaylistmanager.ui.playlistviewnested.PlaylistNestedActivity;

public class PlaylistImportActivity extends AppCompatActivity {
    public static final String SERIALIZE_TAG = "playlist_data";
    public static final String UUID_KEY_TAG = "playlist_uuid";
    public static final String PARENT_UUID_TAG = "playlist_parent_uuid";
    private static final String TOAST_KEY = "toast_key";
    private static final String PROGRESS_DIALOG_SHOW_KEY = "show_dialog_key";
    private static final String PROGRESS_DIALOG_HIDE_KEY = "hide_dialog_key";
    private static final String PROGRESS_DIALOG_UPDATE_KEY = "update_dialog_key";
    private static final String DATA_UPDATE_KEY = "update_current_key";

    //Handler that handles toast and progress dialog messages.
    private ProgressDialog progressDialog;
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
            if(msg.getData().getString(DATA_UPDATE_KEY) != null) {
                UpdateData();
            }
        }
    };

    private ImageView icon;
    private TextView title;
    private TextView subtitle;
    private TextView source;
    private Button playAll;
    private Button shufflePlay;
    private RecyclerView recyclerView;
    private SongsOptionsRecyclerAdapter adapter;

    private PlaylistInfo playlistInfo;
    private String uuidKey;
    private String uuidParentKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_import);

        //Sets action bar style.
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_new_24);
            actionBar.setTitle("Back");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        this.playlistInfo = (PlaylistInfo) getIntent().getSerializableExtra(SERIALIZE_TAG);
        this.uuidKey = getIntent().getStringExtra(UUID_KEY_TAG);
        this.uuidParentKey = getIntent().getStringExtra(PARENT_UUID_TAG);
        if(this.uuidParentKey != null && this.uuidParentKey.isEmpty()) {
            this.uuidParentKey = null;
        }
        if(this.playlistInfo == null) {
            this.finish();
            return;
        }

        this.icon = findViewById(R.id.imageView_playlist);
        this.title = findViewById(R.id.playlist_title);
        this.subtitle = findViewById(R.id.playlist_subtitle);
        this.source = findViewById(R.id.playlist_source);
        this.playAll = findViewById(R.id.button_playall);
        this.shufflePlay = findViewById(R.id.button_shuffleplay);
        this.recyclerView = findViewById(R.id.recyclerView_allSongs);

        UpdateUI();

        this.playAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playlistInfo == null || playlistInfo.getAllVideos().isEmpty()) {
                    return;
                }
                DataManager.getInstance().UpdatePlaylistLastViewed(uuidKey);
                Intent intent = new Intent(PlaylistImportActivity.this, MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,playlistInfo);
                startActivity(intent);
            }
        });
        this.shufflePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playlistInfo == null || playlistInfo.getAllVideos().isEmpty()) {
                    return;
                }
                DataManager.getInstance().UpdatePlaylistLastViewed(uuidKey);
                Intent intent = new Intent(PlaylistImportActivity.this, MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,playlistInfo);
                intent.putExtra(MediaPlayerActivity.SHUFFLED_TAG,true);
                startActivity(intent);
            }
        });
        this.source.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = source.getText().toString();
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
                startActivity(intent);
            }
        });

        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        this.adapter = new SongsOptionsRecyclerAdapter(this, this.playlistInfo, false, new RecyclerViewOptionsListener() {
            @Override
            public void SelectMenuOption(int position, int itemId, String optional) {
                if(itemId == R.id.export_option) {

                } else if(itemId == R.id.backup_option) {

                } else if(itemId == R.id.delete_option) {
                    boolean success = DataManager.getInstance().RemoveSongFromPlaylist(uuidKey,optional);
                    if(success) {
                        UpdateData();
                        Toast.makeText(PlaylistImportActivity.this,"Song Removed.",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(PlaylistImportActivity.this,"Failed to Remove Song.",Toast.LENGTH_SHORT).show();
                    }
                } else if(itemId == R.id.sync_option) {

                }
            }

            @Override
            public void ButtonClicked(int viewType, int position) {
                if(viewType != SongsOptionsRecyclerAdapter.ADD_ITEM_TOKEN) {
                    DataManager.getInstance().UpdatePlaylistLastViewed(uuidKey);
                    Intent intent = new Intent(PlaylistImportActivity.this, MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,playlistInfo);
                    intent.putExtra(MediaPlayerActivity.POSITION_TAG,position);
                    startActivity(intent);
                }
            }
        });
        this.recyclerView.setAdapter(this.adapter);

        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setTitle("Syncing Playlist");
        this.progressDialog.setCanceledOnTouchOutside(false);
    }

    public void UpdateData() {
        PlaylistInfo fetchedPlaylist = DataManager.getInstance().GetPlaylistFromKey(this.uuidKey);
        if(fetchedPlaylist != null) {
            this.playlistInfo = fetchedPlaylist;
            this.adapter.updateData(this.playlistInfo);
            UpdateUI();
        }
    }

    public void UpdateUI() {
        if(this.playlistInfo == null) {
            return;
        }

        this.title.setText(this.playlistInfo.getTitle());
        this.subtitle.setText(new String(" " + this.playlistInfo.getAllVideos().size() + " songs"));
        this.source.setText(this.playlistInfo.getLinkSource() == null ? "" : this.playlistInfo.getLinkSource());
        this.source.setSelected(true);
        if(!this.playlistInfo.getAllVideos().isEmpty()) {
            PlaybackAudioInfo sourceAudio = this.playlistInfo.getAllVideos().iterator().next();
            if(sourceAudio.getThumbnailType() == PlaybackAudioInfo.PlaybackMediaType.LOCAL) {
                Bitmap bitmap = BitmapFactory.decodeFile(sourceAudio.getThumbnailSource());
                if (bitmap != null) {
                    this.icon.setImageBitmap(bitmap);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.imported_playlist_options,menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
        } else if(id == R.id.export_option) {

        } else if(id == R.id.sync_option) {
            PlaylistInfo currentPlaylist = DataManager.getInstance().GetPlaylistFromKey(this.uuidKey);
            if(currentPlaylist != null) {
                StartProgressDialog("Syncing Playlist...");
                PlatformCompatUtility.SyncPlaylist(this.uuidKey, new SyncPlaylistListener() {
                    @Override
                    public void onComplete() {
                        UpdateDataHandlerCall();
                        SendToast("Successfully Synced Playlist.");
                        HideProgressDialog();
                    }

                    @Override
                    public void onProgress(String message) {
                        UpdateProgressDialog(message);
                    }

                    @Override
                    public void onError(int code, String message) {
                        if(code == -1) {
                            SendToast(message);
                            HideProgressDialog();
                        }
                        else {
                            UpdateProgressDialog(message);
                        }
                    }
                });
            }
        } else if(id == R.id.backup_option) {

        } else if(id == R.id.delete_option) {
            DataManager.getInstance().RemovePlaylist(this.uuidKey,this.uuidParentKey);
            this.finish();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        if(this.progressDialog != null) {
            this.progressDialog.dismiss();
        }
        super.onDestroy();
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

    public void UpdateDataHandlerCall() {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(DATA_UPDATE_KEY,"UPDATE");
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }
}