package com.example.cloudplaylistmanager.ui.playlistviewnested;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cloudplaylistmanager.MusicPlayer.MediaPlayerActivity;
import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.ExportListener;
import com.example.cloudplaylistmanager.Utils.PlatformCompatUtility;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.Utils.SyncPlaylistListener;
import com.google.android.material.tabs.TabLayout;

public class PlaylistNestedActivity extends AppCompatActivity {
    public static final String SERIALIZE_TAG = "playlist_data";
    public static final String UUID_KEY_TAG = "playlist_uuid";
    private static final String TOAST_KEY = "toast_key";
    private static final String PROGRESS_DIALOG_SHOW_KEY = "show_dialog_key";
    private static final String PROGRESS_DIALOG_HIDE_KEY = "hide_dialog_key";
    private static final String PROGRESS_DIALOG_UPDATE_KEY = "update_dialog_key";

    private ImageView icon;
    private TextView title;
    private TextView subtitle;
    private Button playAll;
    private Button shufflePlay;
    private ViewPager2 viewPager2;
    private TabLayout tabLayout;
    private PlaylistNestedViewPagerAdapter adapter;

    private PlaylistNestedViewModel playlistNestedViewModel;
    private PlaylistInfo playlistInfo;
    private String uuidKey;

    /**
     * OnCreate Method sets up the UI.
     * @param savedInstanceState bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_nested);

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
        if(this.playlistInfo == null) {
            this.finish();
            return;
        }

        //Sets up the UI.
        this.icon = findViewById(R.id.imageView_playlist);
        this.title = findViewById(R.id.playlist_title);
        this.subtitle = findViewById(R.id.playlist_subtitle);
        this.playAll = findViewById(R.id.button_playall);
        this.shufflePlay = findViewById(R.id.button_shuffleplay);
        this.viewPager2 = findViewById(R.id.viewPager);
        this.tabLayout = findViewById(R.id.tabLayout);

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
                Intent intent = new Intent(PlaylistNestedActivity.this, MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
                Intent intent = new Intent(PlaylistNestedActivity.this, MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,playlistInfo);
                intent.putExtra(MediaPlayerActivity.SHUFFLED_TAG,true);
                startActivity(intent);
            }
        });

        //Updates the data from the view model
        this.playlistNestedViewModel = new ViewModelProvider(this).get(PlaylistNestedViewModel.class);
        this.playlistNestedViewModel.updateData(this.uuidKey);

        //Sets the recycler view/adapter
        this.adapter = new PlaylistNestedViewPagerAdapter(getSupportFragmentManager(),getLifecycle());
        this.viewPager2.setAdapter(this.adapter);

        //Sets the tab change listener for the layout.
        this.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        this.viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        });

        //Sets listener for any changes in data from the view model.
        this.playlistNestedViewModel.getPlaylistData().observe(this, new Observer<Pair<String, PlaylistInfo>>() {
            @Override
            public void onChanged(Pair<String, PlaylistInfo> stringPlaylistInfoPair) {
                if(stringPlaylistInfoPair == null) {
                    return;
                }

                playlistInfo = stringPlaylistInfoPair.second;
                UpdateUI();
            }
        });

        //Creates the progress dialog.
        this.progressDialog = new ProgressDialog(this);
        this.progressDialog.setTitle("Syncing");
        this.progressDialog.setCanceledOnTouchOutside(false);
        this.progressDialog.setCancelable(false);
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
        getMenuInflater().inflate(R.menu.nested_playlist_options,menu);
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
        } else if(id == R.id.rename_option) {
            //Starts rename dialog prompt
            ShowRenameDialog();
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
            //Syncs the playlist current playlist.
            PlaylistInfo currentNestedPlaylist = DataManager.getInstance().GetPlaylistFromKey(this.uuidKey);
            if(currentNestedPlaylist != null) {

                StartProgressDialog("Syncing Playlists...");
                //Uses the PlatformCompatUtility to sync the playlist.
                PlatformCompatUtility.SyncPlaylistsMultiple(currentNestedPlaylist.GetImportedPlaylistKeys(), new SyncPlaylistListener() {
                    @Override
                    public void onComplete() {
                        //When it is complete, notify the user and send an update to the view model.
                        playlistNestedViewModel.updateData(uuidKey);
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
            DataManager.getInstance().RemovePlaylist(this.uuidKey,null);
            this.finish();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * Prompts the user with a rename dialog that allows the user to enter a new name for the
     * playlist. When submitted, it will change the name of the playlist in DataManager.
     */
    public void ShowRenameDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_text_input);

        //Sets up UI.
        TextView title = dialog.findViewById(R.id.popup_title);
        EditText name = dialog.findViewById(R.id.edit_text_name);
        TextView confirm = dialog.findViewById(R.id.textView_confirm_button);

        title.setText(R.string.create_playlist_display);
        name.setHint(R.string.popup_create_playlist_hint);

        //Sets the click listener for the confirm button.
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputName = name.getText().toString().trim();
                if(!inputName.isEmpty()) {
                    //Creates and updates the data.
                    DataManager.getInstance().RenamePlaylist(uuidKey, inputName);
                    playlistNestedViewModel.updateData(uuidKey);
                    dialog.dismiss();
                    Toast.makeText(PlaylistNestedActivity.this,"Playlist renamed to: \"" + inputName + "\"",Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.playlistNestedViewModel.updateData(this.uuidKey);
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
        }
    };
}