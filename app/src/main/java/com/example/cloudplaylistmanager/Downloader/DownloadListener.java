package com.example.cloudplaylistmanager.Downloader;

import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;

public interface DownloadListener {
    void onComplete(PlaybackAudioInfo audio);
    void onProgressUpdate(float progress, long etaSeconds);
    void onError(int attempt, String error);
}
