package com.example.cloudplaylistmanager.Utils;

public interface FetchPlaylistListener {
    void onComplete(PlaylistInfo fetchedPlaylist);

    void onError(String message);
}
