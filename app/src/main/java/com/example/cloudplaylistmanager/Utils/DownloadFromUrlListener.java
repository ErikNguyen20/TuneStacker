package com.example.cloudplaylistmanager.Utils;

public interface DownloadFromUrlListener {
    void onComplete(PlaybackAudioInfo audio);
    void onProgressUpdate(float progress, long etaSeconds);
    void onError(int attempt, String error);
}
