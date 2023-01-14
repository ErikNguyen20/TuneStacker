package com.example.cloudplaylistmanager.Utils;

public interface DownloadPlaylistListener {
    void onComplete(PlaylistInfo playlist);
    void onProgressUpdate(String message);
    void onError(int attempt, String error);
}
