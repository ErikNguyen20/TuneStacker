package com.example.cloudplaylistmanager.Utils;

public interface SyncPlaylistListener {
    void onComplete();
    void onProgress(String message);
    void onError(int code, String message);
}
