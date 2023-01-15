package com.example.cloudplaylistmanager.ui.playlistviewnested;

import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

public class PlaylistNestedViewModel extends ViewModel {
    private MutableLiveData<Pair<String, PlaylistInfo>> playlistInfo;
    private MutableLiveData<String> lastUpdated;

    public PlaylistNestedViewModel() {
        this.playlistInfo = new MutableLiveData<>();
        this.lastUpdated = new MutableLiveData<>();
        this.lastUpdated.postValue(null);
    }

    public void updateData(String key) {
        String recentUpdate = DataManager.getInstance().GetDataLastUpdate();
        //Only induce an update on the UI if the data was actually updated in DataManager.
        if(this.lastUpdated == null ||
                !recentUpdate.equals(this.lastUpdated.getValue())) {

            PlaylistInfo playlist = DataManager.getInstance().GetPlaylistFromKey(key);
            if(playlist != null) {
                this.playlistInfo.postValue(new Pair<>(key,playlist));
            }

            this.lastUpdated.postValue(recentUpdate);
        }
    }

    public LiveData<Pair<String, PlaylistInfo>> getPlaylistData() {
        return this.playlistInfo;
    }
}
