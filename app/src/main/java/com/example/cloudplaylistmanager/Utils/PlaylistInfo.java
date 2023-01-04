package com.example.cloudplaylistmanager.Utils;

import android.util.Pair;

import com.example.cloudplaylistmanager.Platforms.YoutubeUtilities;

import java.util.ArrayList;

public class PlaylistInfo {
    private String title;
    private String databasePath;
    private String linkSource;
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

    public String getDatabasePath() {
        return this.databasePath;
    }

    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }

    public String getLinkSource() {
        return this.linkSource;
    }

    public void setLinkSource(String linkSource) {
        this.linkSource = linkSource;
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

    public Pair<ArrayList<PlaybackAudioInfo>, ArrayList<PlaybackAudioInfo>> ComparePlaylists(PlaylistInfo other) {
        //Audios that the source playlist has, but not in the OTHER playlist.
        ArrayList<PlaybackAudioInfo> added = new ArrayList<>();
        //Audios that the source playlist does NOT have, but the OTHER playlist has.

        for(PlaybackAudioInfo sourceAudio : this.videos) {
            if(other.videos.contains(sourceAudio)) {
                other.videos.remove(sourceAudio);
            }
            else {
                added.add(sourceAudio);
            }
        }

        return new Pair<>(added, other.videos);
    }
}
