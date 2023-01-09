package com.example.cloudplaylistmanager.ui.playlistviewnested;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

public class PlaylistNestedViewModel extends ViewModel {
    private MutableLiveData<PlaylistInfo> playlistInfo;

    public PlaylistNestedViewModel() {
        this.playlistInfo = new MutableLiveData<>();
    }

    public void setPlaylistData(PlaylistInfo playlistInfo) {
        this.playlistInfo.setValue(playlistInfo);
    }

    public LiveData<PlaylistInfo> getPlaylistData() {
        return this.playlistInfo;
    }
}
