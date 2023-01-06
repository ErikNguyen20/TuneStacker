package com.example.cloudplaylistmanager.Utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class PlaylistInfo implements Serializable {
    private String title;
    private String linkSource;
    private ArrayList<PlaybackAudioInfo> insertedVideos;
    private ArrayList<PlaylistInfo> importedPlaylists;
    private LinkedHashSet<PlaybackAudioInfo> allVideos;

    public PlaylistInfo() {
        this.insertedVideos = new ArrayList<>();
        this.allVideos = new LinkedHashSet<>();
        this.importedPlaylists = new ArrayList<>();
        this.linkSource = null;
        this.title = "Unnamed Playlist";
    }

    private void UpdateAllVideos() {
        this.allVideos.clear();
        this.allVideos.addAll(this.insertedVideos);
        for(PlaylistInfo playlist : importedPlaylists) {
            this.allVideos.addAll(playlist.getAllVideos());
        }
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLinkSource() {
        return this.linkSource;
    }

    public void setLinkSource(String linkSource) {
        this.linkSource = linkSource;
    }

    public LinkedHashSet<PlaybackAudioInfo> getAllVideos() {
        return this.allVideos;
    }

    public ArrayList<PlaybackAudioInfo> getInsertedVideos() {
        return this.insertedVideos;
    }

    public void AddVideoToPlaylist(PlaybackAudioInfo video) {
        this.insertedVideos.add(video);
        this.allVideos.add(video);
    }

    public void ImportPlaylist(PlaylistInfo other) {
        this.importedPlaylists.add(other);
        UpdateAllVideos();
    }

    public ArrayList<PlaylistInfo> GetImportedPlaylists() {
        return this.importedPlaylists;
    }

    public void MergePlaylists(PlaylistInfo other) {
        this.insertedVideos.addAll(other.insertedVideos);
        UpdateAllVideos();
    }

    public Pair<ArrayList<PlaybackAudioInfo>, ArrayList<PlaybackAudioInfo>> ComparePlaylists(PlaylistInfo other) {
        //Audios that the source playlist has, but not in the OTHER playlist.
        ArrayList<PlaybackAudioInfo> added = new ArrayList<>();
        //Audios that the source playlist does NOT have, but the OTHER playlist has.

        for(PlaybackAudioInfo sourceAudio : this.insertedVideos) {
            if(other.insertedVideos.contains(sourceAudio)) {
                other.insertedVideos.remove(sourceAudio);
            }
            else {
                added.add(sourceAudio);
            }
        }

        return new Pair<>(added, other.insertedVideos);
    }
}
