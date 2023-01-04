package com.example.cloudplaylistmanager.Utils;

public interface UploadToCloudListener {
    void onComplete(String songMetadataPath);
    void onError(String message);
}
