package com.example.cloudplaylistmanager.Downloader;

import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

public interface DownloadPlaylistListener {
    void onComplete(PlaylistInfo playlist);
    void onProgressUpdate(String message);
    void onError(int attempt, String error);
}
