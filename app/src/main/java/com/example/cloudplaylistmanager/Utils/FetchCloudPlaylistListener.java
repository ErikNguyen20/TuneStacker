package com.example.cloudplaylistmanager.Utils;

import android.util.Pair;

import java.util.ArrayList;

public interface FetchCloudPlaylistListener {
    void onInitialInfo(String playlistPath, String playlistTitle);
    void onUpdate(String playlistPath, int index, PlaybackAudioInfo audio);
    void onError(String message);
}
