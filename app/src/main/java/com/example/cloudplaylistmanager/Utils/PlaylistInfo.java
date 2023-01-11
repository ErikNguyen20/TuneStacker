package com.example.cloudplaylistmanager.Utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class PlaylistInfo implements Serializable {
    @Expose
    private String title;
    @Expose
    private String linkSource;
    @Expose
    private long lastViewed;
    @Expose
    private ArrayList<PlaybackAudioInfo> insertedVideos;
    private ArrayList<Pair<String, PlaylistInfo>> importedPlaylists;
    @Expose
    private ArrayList<String> importedPlaylistsKeys;
    private LinkedHashSet<PlaybackAudioInfo> allVideos;

    public PlaylistInfo() {
        this.insertedVideos = new ArrayList<>();
        this.allVideos = new LinkedHashSet<>();
        this.importedPlaylists = new ArrayList<>();
        this.importedPlaylistsKeys = new ArrayList<>();
        this.linkSource = null;
        this.lastViewed = 0;
        this.title = "Unnamed Playlist";
    }

    public void UpdateAllVideos() {
        if(this.allVideos == null) {
            this.allVideos = new LinkedHashSet<>();
        }
        this.allVideos.clear();
        this.allVideos.addAll(this.insertedVideos);
        for(Pair<String, PlaylistInfo> pair : importedPlaylists) {
            this.allVideos.addAll(pair.second.getAllVideos());
        }
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getLastViewed() {
        return this.lastViewed;
    }

    public void updateLastViewed() {
        this.lastViewed = System.currentTimeMillis();
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

    public void ImportPlaylist(String key, PlaylistInfo other) {
        this.importedPlaylists.add(new Pair<>(key, other));
        this.importedPlaylistsKeys.add(key);
        UpdateAllVideos();
    }

    public ArrayList<Pair<String, PlaylistInfo>> GetImportedPlaylists() {
        return this.importedPlaylists;
    }

    public void RemoveImportedPlaylist(String key) {
        this.importedPlaylistsKeys.remove(key);
        for(int index = 0; index < this.importedPlaylists.size(); index++) {
            if(this.importedPlaylists.get(index).first.equals(key)) {
                this.importedPlaylists.remove(index);
                UpdateAllVideos();
                break;
            }
        }
    }

    public void ClearImportedPlaylists() {
        this.importedPlaylists.clear();
        UpdateAllVideos();
    }

    public ArrayList<String> GetImportedPlaylistKeys() {
        return this.importedPlaylistsKeys;
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
