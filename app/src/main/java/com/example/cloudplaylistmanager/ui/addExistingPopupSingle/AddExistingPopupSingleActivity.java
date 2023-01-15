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

    /**
     * OnCreate Method sets up the UI.
     * @param savedInstanceState bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_existing_popup_single);

        //Gets the data passed through by the intent extras.
        this.isPlaylist = getIntent().getBooleanExtra(IS_PLAYLIST_TAG,false);
        this.uuidParentKey = getIntent().getStringExtra(PARENT_UUID_TAG);
        if(this.uuidParentKey != null && this.uuidParentKey.isEmpty()) {
            this.uuidParentKey = null;
            Toast.makeText(this,"Failed to get Parent Playlist's UUID.",Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
        this.selectedItems = new HashSet<>();

        this.searchEdit = findViewById(R.id.edit_text_search);
        this.confirmButton = findViewById(R.id.textView_confirm_button);


        if(this.isPlaylist) {
            //Sets up the UI view for adding playlists.
            this.recyclerView = findViewById(R.id.recyclerView);

            //Fetches and populates playlist data.
            ArrayList<Pair<String, PlaylistInfo>> fetchedPlaylist = DataManager.getInstance().GetImportedPlaylists();
            this.importedPlaylistData = new HashMap<>();
            if(fetchedPlaylist != null) {
                //Constructs a hashset for the existing audios that are already in the playlist.
                PlaylistInfo currentPlaylist = DataManager.getInstance().GetPlaylistFromKey(this.uuidParentKey);
                HashSet<String> existingKeys = new HashSet<>();
                if(currentPlaylist != null) {
                    existingKeys = currentPlaylist.GetImportedPlaylistKeys();
                }
                for(Pair<String, PlaylistInfo> item : fetchedPlaylist) {
                    //If the playlist has already been imported into the nested playlist, do not add it.
                    if(!existingKeys.contains(item.first)) {
                        this.importedPlaylistData.put(item.first, item.second);
                    }
                }
            }

            //Sets the recycler view/adapter
            this.recyclerView.setHasFixedSize(true);
            this.recyclerView.setLayoutManager(new LinearLayoutManager(this));

            this.adapter = new SelectItemsRecyclerAdapter(this, true, null, new RecyclerViewSelectItemsListener() {
                @Override
                public void ButtonClicked(String uuid) {
                    //Keeps track of the selection state of each item in the recycler view.
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
                    //Fetches the playlist information that is mapped to the uuid.
                    if(uuid == null || uuid.isEmpty() || importedPlaylistData.get(uuid) == null) {
                        return null;
                    }
                    return new Pair<>(importedPlaylistData.get(uuid),selectedItems.contains(uuid));
                }
            });
            this.recyclerView.setAdapter(this.adapter);

        } else {
            //Sets up the UI view for adding audios.
            this.recyclerView = findViewById(R.id.recyclerView);

            PlaylistInfo fetchedAudio = DataManager.getInstance().ConstructPlaylistFromLocalFiles();
            this.savedSongsData = new HashMap<>();
            if(fetchedAudio != null) {
                //Constructs a hashset for the existing audios that are already in the playlist.
                PlaylistInfo currentPlaylist = DataManager.getInstance().GetPlaylistFromKey(this.uuidParentKey);
                HashSet<String> existingAudios = new HashSet<>();
                for(PlaybackAudioInfo audio : currentPlaylist.getAllVideos()) {
                    existingAudios.add(audio.getTitle());
                }

                for(PlaybackAudioInfo audio : fetchedAudio.getAllVideos()) {
                    //If the playlist has already been imported into the nested playlist, do not add it.
                    if(!existingAudios.contains(audio.getTitle())) {
                        this.savedSongsData.put(UUID.randomUUID().toString(),audio);
                    }
                }
            }

            //Sets the recycler view/adapter
            this.recyclerView.setHasFixedSize(true);
            this.recyclerView.setLayoutManager(new LinearLayoutManager(this));

            this.adapter = new SelectItemsRecyclerAdapter(this, false, null, new RecyclerViewSelectItemsListener() {
                @Override
                public void ButtonClicked(String uuid) {
                    //Keeps track of the selection state of each item in the recycler view.
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
                    //Fetches the audio information that is mapped to the uuid.
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

        //Sets the click listener for the confirm button.
        this.confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConfirmAddButton();
            }
        });

        //Sets the search query listener.
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
            //Adds the selected playlist items into the nested playlist.
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
            //Adds the selected audio items into the nested playlist.
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
            //If the query is empty, display every item.
            if(this.isPlaylist) {
                ids.addAll(this.importedPlaylistData.keySet());
            } else {
                ids.addAll(this.savedSongsData.keySet());
            }
        }
        else {
            if (this.isPlaylist) {
                //Display queried playlist items.
                for (Map.Entry<String, PlaylistInfo> entry : this.importedPlaylistData.entrySet()) {
                    if (entry.getValue().getTitle().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
                        ids.add(entry.getKey());
                    }
                }
            } else {
                //Display queried audio items.
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