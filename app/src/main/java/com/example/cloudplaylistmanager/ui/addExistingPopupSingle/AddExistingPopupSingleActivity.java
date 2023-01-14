package com.example.cloudplaylistmanager.ui.addExistingPopupSingle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewSelectItemsListener;
import com.example.cloudplaylistmanager.RecyclerAdapters.SelectItemsRecyclerAdapter;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Popup Dialog Activity that implements the interface to add an existing Song or Playlist.
 * IntentExtra IS_PLAYLIST_TAG must be sent to determine if the popup will be for adding
 * an audio file or for adding a playlist.
 * IntentExtra PARENT_UUID_TAG must be sent to add the songs/playlists to the correct playlist.
 */
public class AddExistingPopupSingleActivity extends AppCompatActivity {
    public static final String IS_PLAYLIST_TAG = "is_playlist";
    public static final String PARENT_UUID_TAG = "playlist_parent_uuid";

    private boolean isPlaylist;
    private String uuidParentKey;
    private HashMap<String, PlaybackAudioInfo> savedSongsData;
    private HashMap<String, PlaylistInfo> importedPlaylistData;
    private HashSet<String> selectedItems;

    private TextView confirmButton;
    private android.widget.SearchView searchEdit;
    private RecyclerView recyclerView;
    private SelectItemsRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_existing_popup_single);

        //Gets the data passed through by the intent extras.
        this.isPlaylist = getIntent().getBooleanExtra(IS_PLAYLIST_TAG,false);
        this.uuidParentKey = getIntent().getStringExtra(PARENT_UUID_TAG);
        if(this.uuidParentKey != null && this.uuidParentKey.isEmpty()) {
            this.uuidParentKey = null;
        }
        this.selectedItems = new HashSet<>();

        this.searchEdit = findViewById(R.id.edit_text_search);
        this.confirmButton = findViewById(R.id.textView_confirm_button);



        if(this.isPlaylist) {
            this.recyclerView = findViewById(R.id.recyclerView);

            ArrayList<Pair<String, PlaylistInfo>> fetchedPlaylist = DataManager.getInstance().GetImportedPlaylists();
            this.importedPlaylistData = new HashMap<>();
            if(fetchedPlaylist != null) {
                for(Pair<String, PlaylistInfo> item : fetchedPlaylist) {
                    this.importedPlaylistData.put(item.first, item.second);
                }
            }

            this.recyclerView.setHasFixedSize(true);
            this.recyclerView.setLayoutManager(new LinearLayoutManager(this));

            this.adapter = new SelectItemsRecyclerAdapter(this, true, null, new RecyclerViewSelectItemsListener() {
                @Override
                public void ButtonClicked(String uuid) {
                    if(!selectedItems.contains(uuid)) {
                        selectedItems.add(uuid);
                    }
                    else {
                        selectedItems.remove(uuid);
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public Pair<PlaybackAudioInfo, Boolean> FetchAudioInformation(String uuid) {
                    return null;
                }

                @Override
                public Pair<PlaylistInfo, Boolean> FetchPlaylistInformation(String uuid) {
                    if(uuid == null || uuid.isEmpty() || importedPlaylistData.get(uuid) == null) {
                        return null;
                    }
                    return new Pair<>(importedPlaylistData.get(uuid),selectedItems.contains(uuid));
                }
            });
            this.recyclerView.setAdapter(this.adapter);

        } else {
            this.recyclerView = findViewById(R.id.recyclerView);

            PlaylistInfo fetchedAudio = DataManager.getInstance().ConstructPlaylistFromLocalFiles();
            this.savedSongsData = new HashMap<>();
            if(fetchedAudio != null) {
                for(PlaybackAudioInfo audio : fetchedAudio.getAllVideos()) {
                    this.savedSongsData.put(UUID.randomUUID().toString(),audio);
                }
            }

            this.recyclerView.setHasFixedSize(true);
            this.recyclerView.setLayoutManager(new LinearLayoutManager(this));

            this.adapter = new SelectItemsRecyclerAdapter(this, false, null, new RecyclerViewSelectItemsListener() {
                @Override
                public void ButtonClicked(String uuid) {
                    if(!selectedItems.contains(uuid)) {
                        selectedItems.add(uuid);
                    }
                    else {
                        selectedItems.remove(uuid);
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public Pair<PlaybackAudioInfo, Boolean> FetchAudioInformation(String uuid) {
                    if(uuid == null || uuid.isEmpty() || savedSongsData.get(uuid) == null) {
                        return null;
                    }
                    return new Pair<>(savedSongsData.get(uuid),selectedItems.contains(uuid));
                }

                @Override
                public Pair<PlaylistInfo, Boolean> FetchPlaylistInformation(String uuid) {
                    return null;
                }
            });
            this.recyclerView.setAdapter(this.adapter);
        }

        this.confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConfirmAddButton();
            }
        });

        this.searchEdit.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                FilterDataset(s);
                return false;
            }
        });

        FilterDataset(null);
    }


    /**
     * Adds the songs/playlists based on the items that the user selected.
     */
    public void ConfirmAddButton() {
        if(this.selectedItems.isEmpty()) {
            this.finish();
        }
        if(this.uuidParentKey == null) {
            Toast.makeText(this,"Parent Playlist State is Invalid!",Toast.LENGTH_SHORT).show();
            return;
        }
        if(this.isPlaylist) {
            for(String item : this.selectedItems) {
                if(this.importedPlaylistData.containsKey(item)) {
                    boolean success = DataManager.getInstance().AddImportPlaylistToNested(this.uuidParentKey, item);
                    if(success) {
                        Toast.makeText(this,"Successfully Added.",Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,"Failed to Add Playlist.",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else {
            for(String item : this.selectedItems) {
                if(this.savedSongsData.containsKey(item)) {
                    boolean success = DataManager.getInstance().AddSongToPlaylist(this.uuidParentKey,this.savedSongsData.get(item));
                    if(success) {
                        Toast.makeText(this,"Successfully Added.",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(this,"Failed to Add Song.",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        this.finish();
    }

    /**
     * Filters the Recycler View based on the text query.
     * @param query Text Input query
     */
    public void FilterDataset(String query) {
        if(query == null) {
            query = "";
        }
        ArrayList<String> ids = new ArrayList<>();

        if(query.isEmpty()) {
            if(this.isPlaylist) {
                ids.addAll(this.importedPlaylistData.keySet());
            } else {
                ids.addAll(this.savedSongsData.keySet());
            }
        }
        else {
            if (this.isPlaylist) {
                for (Map.Entry<String, PlaylistInfo> entry : this.importedPlaylistData.entrySet()) {
                    if (entry.getValue().getTitle().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
                        ids.add(entry.getKey());
                    }
                }
            } else {
                for (Map.Entry<String, PlaybackAudioInfo> entry : this.savedSongsData.entrySet()) {
                    if (entry.getValue().getTitle().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
                        ids.add(entry.getKey());
                    }
                }
            }
        }

        if(this.adapter != null) {
            this.adapter.FilterResults(ids);
        }
    }
}