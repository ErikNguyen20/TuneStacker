package com.example.cloudplaylistmanager.Utils;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * A class that represents a playlist object, containing all the necessary playlist
 * data. This class is also implements {@link Serializable}.
 * Fields with @Expose indicate that the Gson serializable can touch those fields.
 */
public class PlaylistInfo implements Serializable {
    @Expose
    private String title;
    @Expose
    private String linkSource;
    @Expose
    private long lastViewed;
    @Expose
    protected ArrayList<PlaybackAudioInfo> insertedVideos;
    @Expose
    private ArrayList<String> importedPlaylistsKeys;

    private LinkedHashSet<PlaybackAudioInfo> allVideos;
    private ArrayList<SerializablePair<String, PlaylistInfo>> importedPlaylists;

    /**
     * Instantiates a new PlaylistsInfo object.
     */
    public PlaylistInfo() {
        this.insertedVideos = new ArrayList<>();
        this.allVideos = new LinkedHashSet<>();
        this.importedPlaylists = new ArrayList<>();
        this.importedPlaylistsKeys = new ArrayList<>();
        this.linkSource = null;
        this.lastViewed = 0;
        this.title = "Unnamed Playlist";
    }

    /**
     * Updates and populates the allVideos LinkedHashSet with videos that were inserted into
     * this playlist, and its children playlists.
     */
    public void UpdateAllVideos() {
        if(this.allVideos == null) {
            this.allVideos = new LinkedHashSet<>();
        }
        this.allVideos.clear();
        this.allVideos.addAll(this.insertedVideos);
        for(SerializablePair<String, PlaylistInfo> pair : importedPlaylists) {
            this.allVideos.addAll(pair.second.getAllVideos());
        }
    }

    /**
     * Fetches and returns the title of the playlist.
     * @return Title of the playlist.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Sets the title of the playlist.
     * @param title Title of the playlist.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Retrieves the last viewed time in milliseconds.
     * @return Last viewed time in milliseconds.
     */
    public long getLastViewed() {
        return this.lastViewed;
    }

    /**
     * Updates the last viewed time based on the system's current time in milliseconds.
     */
    public void updateLastViewed() {
        this.lastViewed = System.currentTimeMillis();
    }

    /**
     * Returns the original URL link source of the playlist.
     * @return URL link source of the playlist.
     */
    public String getLinkSource() {
        return this.linkSource;
    }

    /**
     * Returns the original URl link source of the playlist.
     * @param linkSource Original URL link source of the playlist.
     */
    public void setLinkSource(String linkSource) {
        this.linkSource = linkSource;
    }

    /**
     * Returns a list of all of the videos of this playlist, including the imported ones.
     * @return A list of all of the videos, including imports.
     */
    public LinkedHashSet<PlaybackAudioInfo> getAllVideos() {
        return this.allVideos;
    }

    /**
     * Returns a list of all inserted videos. This list does not include imported playlists.
     * @return A list of inserted videos. Does not include imports.
     */
    public ArrayList<PlaybackAudioInfo> getInsertedVideos() {
        return this.insertedVideos;
    }


    /**
     * Returns a list of all of the import keys that are currently included in this playlist.
     * @return List of imported playlist keys
     */
    public ArrayList<String> GetImportedPlaylistKeys() {
        return this.importedPlaylistsKeys;
    }

    /**
     * Checks to see if a given key belongs to an import in this playlist.
     * @return If the import exists.
     */
    public boolean ContainsImportedKey(String key) {
        return this.importedPlaylistsKeys.contains(key);
    }

    /**
     * Checks to see if a particular audio is contained in this playlist.
     * @return If the audio exists.
     */
    public boolean ContainsAudio(PlaybackAudioInfo audio) {
        return this.insertedVideos.contains(audio);
    }

    /**
     * Removes an audio with the given name.
     * @return If the removal was successful.
     */
    public boolean RemoveAudio(String audioTitle) {
        for(int index = 0; index < this.insertedVideos.size(); index++) {
            if(audioTitle.equals(this.insertedVideos.get(index).getTitle())) {
                this.insertedVideos.remove(index);
                UpdateAllVideos();
                return true;
            }
        }
        return false;
    }

    /**
     * Adds an audio into the playlist.
     * @param audio Audio information.
     */
    public void AddAudioToPlaylist(PlaybackAudioInfo audio) {
        this.insertedVideos.add(audio);
        UpdateAllVideos();
    }

    /**
     * Returns a list of all imported playlists. Each entry in the list is a pair, with the first
     * entry being the key reference to the playlist in the data, and the second being the object.
     * @return List of all imported playlists.
     */
    public ArrayList<SerializablePair<String, PlaylistInfo>> GetImportedPlaylists() {
        return this.importedPlaylists;
    }

    /**
     * Imports a playlist into this playlist.
     * @param key Key reference of the playlist in the data.
     * @param playlistInfo Playlist information.
     */
    public void ImportPlaylist(String key, PlaylistInfo playlistInfo) {
        this.importedPlaylists.add(new SerializablePair<>(key, playlistInfo));
        this.importedPlaylistsKeys.add(key);
        UpdateAllVideos();
    }

    /**
     * Imports a playlist into this playlist without updating the list of keys.
     * @param key Key reference of the playlist in the data.
     * @param other Playlist information.
     */
    public void ImportPlaylistWithoutUpdatingKeys(String key, PlaylistInfo other) {
        this.importedPlaylists.add(new SerializablePair<>(key, other));
        UpdateAllVideos();
    }

    /**
     * Removes an imported playlist from the playlist using the given key reference.
     * @param key Key reference of the playlist in the data.
     */
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

    /**
     * Clears all imported playlist data from the playlist.
     * Does not clear the keys.
     */
    public void ClearImportedPlaylists() {
        this.importedPlaylists.clear();
        UpdateAllVideos();
    }

}
