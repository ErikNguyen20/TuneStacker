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

        //We will initialize this data in this constructor. (TEST VALUES)
        ArrayList<Pair<String,PlaylistInfo>> test = new ArrayList<>();
        PlaylistInfo testItem = new PlaylistInfo();
        testItem.setTitle("Test LOL");
        test.add(new Pair<>("lmao",testItem));
        this.myPlaylists.postValue(test);

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
                this.importedPlaylists.setValue(fetchedImportsPlaylist);
            }

            ArrayList<Pair<String,PlaylistInfo>> fetchedMyPlaylists = DataManager.getInstance().GetNestedPlaylists();
            if(fetchedMyPlaylists != null) {
                this.myPlaylists.setValue(fetchedMyPlaylists);
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