package com.example.cloudplaylistmanager.ui.dashboard;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;

public class DashboardViewModel extends ViewModel {

    private MutableLiveData<ArrayList<PlaylistInfo>> myPlaylists;
    private MutableLiveData<ArrayList<PlaylistInfo>> importedPlaylists;
    private MutableLiveData<PlaylistInfo> localVideos;

    public DashboardViewModel() {
        this.myPlaylists = new MutableLiveData<>();
        this.importedPlaylists = new MutableLiveData<>();
        this.localVideos = new MutableLiveData<>();

        //We will initialize this data in this constructor.
        ArrayList<PlaylistInfo> test = new ArrayList<>();
        PlaylistInfo testItem = new PlaylistInfo();
        testItem.setTitle("Test LOL");
        test.add(testItem);
        this.myPlaylists.setValue(test);

        PlaylistInfo fetchedSavedSongs = DataManager.getInstance().ConstructPlaylistFromLocalFiles();
        if(fetchedSavedSongs != null) {
            fetchedSavedSongs.setTitle("Saved Songs");
            this.localVideos.setValue(fetchedSavedSongs);
        }
    }

    public LiveData<ArrayList<PlaylistInfo>> getMyPlaylists() {
        return this.myPlaylists;
    }

    public LiveData<ArrayList<PlaylistInfo>> getImportedPlaylists() {
        return this.importedPlaylists;
    }

    public LiveData<PlaylistInfo> getLocalVideos() {
        return this.localVideos;
    }

    public void updateMyPlaylists() {

    }

    public void updateImportedPlaylists() {

    }

    public void updateLocalVideos() {

    }
}