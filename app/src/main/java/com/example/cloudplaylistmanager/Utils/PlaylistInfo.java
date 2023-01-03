package com.example.cloudplaylistmanager.Utils;

import com.example.cloudplaylistmanager.Platforms.YoutubeUtilities;

import java.util.ArrayList;

public class PlaylistInfo {
    private String title;
    private ArrayList<PlaybackAudioInfo> videos;

    public PlaylistInfo() {
        this.videos = new ArrayList<>();
        this.title = "Unknown Playlist";
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<PlaybackAudioInfo> getVideos() {
        return this.videos;
    }

    public void AddVideoToPlaylist(PlaybackAudioInfo video) {
        this.videos.add(video);
    }

    public void MergePlaylists(PlaylistInfo other) {
        this.videos.addAll(other.videos);
    }
}
