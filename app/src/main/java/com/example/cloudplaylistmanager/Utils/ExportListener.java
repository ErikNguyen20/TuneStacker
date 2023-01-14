package com.example.cloudplaylistmanager.Utils;

public interface ExportListener {
    void onComplete(String message);
    void onProgress(String message);
}
