package com.example.cloudplaylistmanager.Utils;

public interface DownloadPlaylistListener {
    void onComplete(PlaylistInfo playlist);
    void onProgressUpdate(int progress, int outOf);
    void onError(int attempt, String error);
}
