package com.example.cloudplaylistmanager.ui.playlistviewnormal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import com.example.cloudplaylistmanager.Utils.ExportListener;
import com.example.cloudplaylistmanager.Utils.PlatformCompatUtility;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.Utils.SyncPlaylistListener;

import java.util.HashMap;

public class PlaylistImportActivity extends AppCompatActivity {
    public static final String SERIALIZE_TAG = "playlist_data";
    public static final String UUID_KEY_TAG = "playlist_uuid";
    public static final String PARENT_UUID_TAG = "playlist_parent_uuid";
    private static final String TOAST_KEY = "toast_key";
    private static final String PROGRESS_DIALOG_SHOW_KEY = "show_dialog_key";
    private static final String PROGRESS_DIALOG_HIDE_KEY = "hide_dialog_key";
    private static final String PROGRESS_DIALOG_UPDATE_KEY = "update_dialog_key";
    private static final String DATA_UPDATE_KEY = "update_current_key";

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

    /**
     * OnCreate Method sets up the UI.
     * @param savedInstanceState bundle
     */
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


        //Gets data from the intent extras.
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

        //Sets up the UI.
        this.icon = findViewById(R.id.imageView_playlist);
        this.title = findViewById(R.id.playlist_title);
        this.subtitle = findViewById(R.id.playlist_subtitle);
        this.source = findViewById(R.id.playlist_source);
        this.playAll = findViewById(R.id.button_playall);
        this.shufflePlay = findViewById(R.id.button_shuffleplay);
        this.recyclerView = findViewById(R.id.recyclerView_allSongs);

        UpdateUI();

        //Sets the click listener for the Play All button.
        this.playAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playlistInfo == null || playlistInfo.getAllVideos().isEmpty()) {
                    return;
                }
                //Launches the media player.
                DataManager.getInstance().UpdatePlaylistLastViewed(uuidKey);
                Intent intent = new Intent(PlaylistImportActivity.this, MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,playlistInfo);
                startActivity(intent);
            }
        });

        //Sets the click listener for the Shuffle Play button.
        this.shufflePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playlistInfo == null || playlistInfo.getAllVideos().isEmpty()) {
                    return;
                }
                //Launches the media player.
                DataManager.getInstance().UpdatePlaylistLastViewed(uuidKey);
                Intent intent = new Intent(PlaylistImportActivity.this, MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,playlistInfo);
                intent.putExtra(MediaPlayerActivity.SHUFFLED_TAG,true);
                startActivity(intent);
            }
        });

        //Sets the click listener for the Source button.
        this.source.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sends the user to the original playlist URL
                String url = source.getText().toString();
                Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
                startActivity(intent);
            }
        });

        //Sets the recycler view/adapter
        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        this.adapter = new SongsOptionsRecyclerAdapter(this, this.playlistInfo, false, R.menu.song_item_options, new RecyclerViewOptionsListener() {
            @Override
            public void SelectMenuOption(int position, int itemId, String optional) {
                if(itemId == R.id.export_option) {
                    //Exports the current audio item.
                    boolean success = DataManager.getInstance().ExportSong(optional,null);
                    if(success) {
                        Toast.makeText(PlaylistImportActivity.this,"Song Successfully Exported.",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(PlaylistImportActivity.this,"Song Failed to Export.",Toast.LENGTH_SHORT).show();
                    }
                } else if(itemId == R.id.delete_option) {
                    //Removes the current audio item.
                    boolean success = DataManager.getInstance().RemoveSongFromPlaylist(uuidKey,optional);
                    if(success) {
                        UpdateData();
                        Toast.makeText(PlaylistImportActivity.this,"Song Removed.",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(PlaylistImportActivity.this,"Failed to Remove Song.",Toast.LENGTH_SHORT).show();
                    }
                } else if(itemId == R.id.play_option) {
                    //Creates a new playlist item.
                    PlaybackAudioInfo audio = playlistInfo.getAllVideos().get(position);
                    PlaylistInfo singlePlaylistItem = new PlaylistInfo();
                    singlePlaylistItem.setTitle(audio.getTitle());
                    singlePlaylistItem.AddAudioToPlaylist(audio);

                    //Launches Media Player
                    Intent intent = new Intent(PlaylistImportActivity.this,MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,singlePlaylistItem);
                    intent.putExtra(MediaPlayerActivity.POSITION_TAG,position);
                    startActivity(intent);
                }
            }

            @Override
            public void ButtonClicked(int viewType, int position) {
                if(viewType != SongsOptionsRecyclerAdapter.ADD_ITEM_TOKEN) {
                    //Launches the media player.
                    DataManager.getInstance().UpdatePlaylistLastViewed(uuidKey);
                    Intent intent = new Intent(PlaylistImportActivity.this, MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,playlistInfo);
                    intent.putExtra(MediaPlayerActivity.POSITION_TAG,position);
                    startActivity(intent);
                }
            }
        });
        this.recyclerView.setAdapter(this.adapter);

        //Sets the callback for the item touch helper.
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(this.simpleCallback);
        itemTouchHelper.attachToRecyclerView(this.recyclerView);

        //Creates the progress dialog.
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setTitle("Syncing Playlist");
        this.progressDialog.setCanceledOnTouchOutside(false);
        this.progressDialog.setCancelable(false);
    }

    //Handles dragging of items in the recycler view.
    private ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            if(viewHolder.getItemViewType() == SongsOptionsRecyclerAdapter.ADD_ITEM_TOKEN || target.getItemViewType() == SongsOptionsRecyclerAdapter.ADD_ITEM_TOKEN) {
                return false;
            }

            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            //Swaps positions of the items in the adapter.
            adapter.swapDataPoints(fromPosition, toPosition);

            return true;
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);

            //Updates the positioning of the audios in DataManager.
            HashMap<String, Integer> order = playlistInfo.SetItemsOrder(adapter.getAudios());
            DataManager.getInstance().UpdateOrderOfItemsInPlaylist(uuidKey,order);

            UpdateUI();
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if(viewHolder.getItemViewType() == SongsOptionsRecyclerAdapter.ADD_ITEM_TOKEN) {
                return 0;
            }
            else {
                return super.getMovementFlags(recyclerView, viewHolder);
            }
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
    };

    /**
     * Updates the data on the UI by fetching it from DataManager.
     */
    public void UpdateData() {
        PlaylistInfo fetchedPlaylist = DataManager.getInstance().GetPlaylistFromKey(this.uuidKey);
        if(fetchedPlaylist != null) {
            this.playlistInfo = fetchedPlaylist;
            this.adapter.updateData(this.playlistInfo);
            UpdateUI();
        }
    }

    /**
     * Updates the UI's elements and displays.
     */
    public void UpdateUI() {
        if(this.playlistInfo == null) {
            return;
        }

        this.title.setText(this.playlistInfo.getTitle());
        this.subtitle.setText(new String(" " + this.playlistInfo.getAllVideos().size() + " songs"));
        this.source.setText(this.playlistInfo.getLinkSource() == null ? "" : this.playlistInfo.getLinkSource());
        this.source.setSelected(true);

        //Gets the icon of the first audio item.
        if(!this.playlistInfo.getAllVideos().isEmpty()) {
            PlaybackAudioInfo sourceAudio = this.playlistInfo.getAllVideos().iterator().next();
            Bitmap bitmap = DataManager.getInstance().GetThumbnailImage(sourceAudio);
            if(bitmap != null) {
                this.icon.setImageBitmap(bitmap);
            }
            else {
                this.icon.setImageResource(R.drawable.med_res);
            }
        }
    }

    /**
     * Creates the popup menu on the top right of the action bar.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.imported_playlist_options,menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Handles when an menu option is selected from the top right popup menu.
     * @param item The menu item that was selected.
     * @return If the option was successfully handled.
     */
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //Closes this current activity.
            this.finish();
        } else if(id == R.id.export_option) {
            //Exports the playlist.
            StartProgressDialog("Exporting Playlist...");
            DataManager.getInstance().ExportPlaylist(uuidKey, new ExportListener() {
                @Override
                public void onComplete(String message) {
                    SendToast(message);
                    HideProgressDialog();
                }

                @Override
                public void onProgress(String message) {
                    UpdateProgressDialog(message);
                }
            });
        } else if(id == R.id.sync_option) {
            //Syncs the playlist
            PlaylistInfo currentPlaylist = DataManager.getInstance().GetPlaylistFromKey(this.uuidKey);
            if(currentPlaylist != null) {
                StartProgressDialog("Syncing Playlist...");
                //Uses the PlatformCompatUtility to sync the playlist.
                PlatformCompatUtility.SyncPlaylist(this.uuidKey, new SyncPlaylistListener() {
                    @Override
                    public void onComplete() {
                        //When it is complete, notify the user.
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
                            //Notifies the user if the sync had a critical error.
                            SendToast(message);
                            HideProgressDialog();
                        }
                        else {
                            UpdateProgressDialog(message);
                        }
                    }
                });
            }
        } else if(id == R.id.delete_option) {
            //Calls the DataManager to delete the playlist.
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

    /**
     * Updates the displayed data on the UI.
     */
    public void UpdateDataHandlerCall() {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(DATA_UPDATE_KEY,"UPDATE");
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }

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
}