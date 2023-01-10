package com.example.cloudplaylistmanager.ui.playlistviewnested;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

public class PlaylistNestedViewModel extends ViewModel {
    private MutableLiveData<PlaylistInfo> playlistInfo;
    private MutableLiveData<String> lastUpdated;

    public PlaylistNestedViewModel() {
        this.playlistInfo = new MutableLiveData<>();
        this.lastUpdated = new MutableLiveData<>();
        this.lastUpdated.postValue(null);
    }

    public void updateData(String key) {
        if(this.lastUpdated == null ||
                !DataManager.getInstance().GetDataLastUpdate().equals(this.lastUpdated.getValue())) {

            this.playlistInfo.postValue(DataManager.getInstance().GetPlaylistFromKey(key));
        }
    }

    public LiveData<PlaylistInfo> getPlaylistData() {
        return this.playlistInfo;
    }
}
