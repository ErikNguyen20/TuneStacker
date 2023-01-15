package com.example.cloudplaylistmanager.ui.dashboard;

import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

import java.util.ArrayList;

public class DashboardViewModel extends ViewModel {

    private MutableLiveData<ArrayList<Pair<String,PlaylistInfo>>> myPlaylists;
    private MutableLiveData<ArrayList<Pair<String,PlaylistInfo>>> importedPlaylists;
    private MutableLiveData<PlaylistInfo> localVideos;

    private MutableLiveData<String> lastUpdate;

    public DashboardViewModel() {
        this.myPlaylists = new MutableLiveData<>();
        this.importedPlaylists = new MutableLiveData<>();
        this.localVideos = new MutableLiveData<>();

        this.lastUpdate = new MutableLiveData<>();
        this.lastUpdate.postValue(null);

        updateData();
    }

    /**
     * Allows UI elements to retrieve all nested playlists from the viewmodel.
     * @return List of Playlist Information (String = Key, PlaylistInfo = Data)
     */
    public LiveData<ArrayList<Pair<String,PlaylistInfo>>> getMyPlaylists() {
        return this.myPlaylists;
    }

    /**
     * Allows UI elements to retrieve all imported playlists from the viewmodel.
     * @return List of Playlist Information (String = Key, PlaylistInfo = Data)
     */
    public LiveData<ArrayList<Pair<String,PlaylistInfo>>> getImportedPlaylists() {
        return this.importedPlaylists;
    }

    /**
     * Allows UI elements to retrieve local audio items from the viewmodel.
     * @return Playlist Information
     */
    public LiveData<PlaylistInfo> getLocalVideos() {
        return this.localVideos;
    }

    /**
     * Updates the UI elements that rely on this viewmodel for data.
     */
    public void updateData() {
        String recentUpdate = DataManager.getInstance().GetDataLastUpdate();
        //Only induce an update on the UI if the data was actually updated in DataManager.
        if(this.lastUpdate.getValue() == null ||
                !recentUpdate.equals(this.lastUpdate.getValue())) {

            //Retrieves all imported playlists.
            ArrayList<Pair<String,PlaylistInfo>> fetchedImportsPlaylist = DataManager.getInstance().GetImportedPlaylists();
            if(fetchedImportsPlaylist != null) {
                this.importedPlaylists.postValue(fetchedImportsPlaylist);
            }

            //Retrieves all nested playlists.
            ArrayList<Pair<String,PlaylistInfo>> fetchedMyPlaylists = DataManager.getInstance().GetNestedPlaylists();
            if(fetchedMyPlaylists != null) {
                this.myPlaylists.postValue(fetchedMyPlaylists);
            }

            //Retrieves all locally saved songs.
            PlaylistInfo fetchedSavedSongs = DataManager.getInstance().ConstructPlaylistFromLocalFiles();
            if(fetchedSavedSongs != null) {
                this.localVideos.postValue(fetchedSavedSongs);
            }

            this.lastUpdate.postValue(recentUpdate);
        }
    }
}