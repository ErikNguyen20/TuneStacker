package com.example.cloudplaylistmanager.RecyclerAdapters;

import android.util.Pair;

import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

public interface RecyclerViewSelectItemsListener {
    void ButtonClicked(String uuid);
    Pair<PlaybackAudioInfo,Boolean> FetchAudioInformation(String uuid);
    Pair<PlaylistInfo,Boolean> FetchPlaylistInformation(String uuid);
}
