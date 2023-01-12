package com.example.cloudplaylistmanager.ui.dashboard;

import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.HashMap;

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

    public LiveData<ArrayList<Pair<String,PlaylistInfo>>> getMyPlaylists() {
        return this.myPlaylists;
    }

    public LiveData<ArrayList<Pair<String,PlaylistInfo>>> getImportedPlaylists() {
        return this.importedPlaylists;
    }

    public LiveData<PlaylistInfo> getLocalVideos() {
        return this.localVideos;
    }

    public void updateData() {
        String recentUpdate = DataManager.getInstance().GetDataLastUpdate();
        if(this.lastUpdate.getValue() == null ||
                !recentUpdate.equals(this.lastUpdate.getValue())) {


            ArrayList<Pair<String,PlaylistInfo>> fetchedImportsPlaylist = DataManager.getInstance().GetImportedPlaylists();
            if(fetchedImportsPlaylist != null) {
                this.importedPlaylists.postValue(fetchedImportsPlaylist);
            }

            ArrayList<Pair<String,PlaylistInfo>> fetchedMyPlaylists = DataManager.getInstance().GetNestedPlaylists();
            if(fetchedMyPlaylists != null) {
                this.myPlaylists.postValue(fetchedMyPlaylists);
            }

            PlaylistInfo fetchedSavedSongs = DataManager.getInstance().ConstructPlaylistFromLocalFiles();
            if(fetchedSavedSongs != null) {
                fetchedSavedSongs.setTitle("Saved Songs");
                this.localVideos.postValue(fetchedSavedSongs);
            }

            this.lastUpdate.postValue(recentUpdate);
        }
    }
}