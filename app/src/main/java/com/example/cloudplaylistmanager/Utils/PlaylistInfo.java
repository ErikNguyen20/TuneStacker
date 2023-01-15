package com.example.cloudplaylistmanager.Utils;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

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
    protected HashMap<String, PlaybackAudioInfo> insertedVideos; //String is the name of the audio
    @Expose
    private HashSet<String> importedPlaylistsKeys;
    @Expose
    private HashMap<String, Integer> audioOrder; //String is name of audio, integer is the index.


    private ArrayList<PlaybackAudioInfo> allVideos;
    private ArrayList<SerializablePair<String, PlaylistInfo>> importedPlaylists;

    /**
     * Instantiates a new PlaylistsInfo object.
     */
    public PlaylistInfo() {
        this.insertedVideos = new HashMap<>();
        this.importedPlaylists = new ArrayList<>();
        this.importedPlaylistsKeys = new HashSet<>();
        this.audioOrder = new HashMap<>();
        this.linkSource = null;
        this.lastViewed = 0;
        this.title = "Unnamed Playlist";
        this.allVideos = new ArrayList<>();
    }

    /**
     * Updates and populates the allVideos LinkedHashSet with videos that were inserted into
     * this playlist, and its children playlists.
     */
    public void UpdateAllVideos() {
        this.allVideos.clear();
        this.allVideos.addAll(this.insertedVideos.values());
        for(SerializablePair<String, PlaylistInfo> pair : importedPlaylists) {
            this.allVideos.addAll(pair.second.getAllVideos());
        }
        Collections.sort(this.allVideos, new Comparator<PlaybackAudioInfo>() {
            @Override
            public int compare(PlaybackAudioInfo audioLeft, PlaybackAudioInfo audioRight) {
                Integer positionLeft = audioOrder.get(audioLeft.getTitle());
                Integer positionRight = audioOrder.get(audioRight.getTitle());
                if(positionLeft != null && positionRight != null) {
                    return Integer.compare(positionLeft,positionRight);
                }
                else if(positionLeft != null) {
                    return -1;
                }
                else if(positionRight != null) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        });
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
    public ArrayList<PlaybackAudioInfo> getAllVideos() {
        return this.allVideos;
    }

    /**
     * Returns a list of all inserted videos. This list does not include imported playlists.
     * @return A list of inserted videos. Does not include imports.
     */
    public HashMap<String, PlaybackAudioInfo> getInsertedVideos() {
        return this.insertedVideos;
    }


    /**
     * Returns a list of all of the import keys that are currently included in this playlist.
     * @return List of imported playlist keys
     */
    public HashSet<String> GetImportedPlaylistKeys() {
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
        return this.insertedVideos.containsKey(audio.getTitle());
    }

    /**
     * Removes an audio with the given name.
     * @return If the removal was successful.
     */
    public boolean RemoveAudio(String audioTitle) {
        PlaybackAudioInfo audio = this.insertedVideos.remove(audioTitle);
        UpdateAllVideos();
        return audio != null;
    }

    /**
     * Adds an audio into the playlist.
     * @param audio Audio information.
     */
    public void AddAudioToPlaylist(PlaybackAudioInfo audio) {
        this.insertedVideos.put(audio.getTitle(),audio);
        UpdateAllVideos();
    }

    /**
     * Sets the audio items order in memory using an array of items.
     * @param order Order in which items should be placed.
     */
    public HashMap<String, Integer> SetItemsOrder(ArrayList<PlaybackAudioInfo> order) {
        if(order == null || order.isEmpty()) {
            return null;
        }
        for(int index = 0; index < order.size(); index++) {
            this.audioOrder.put(order.get(index).getTitle(),index);
        }
        UpdateAllVideos();
        return this.audioOrder;
    }

    /**
     * Sets the audio items order in memory using an already existing order map.
     * @param order Order in which items should be placed.
     */
    public void SetItemsOrder(HashMap<String, Integer> order) {
        this.audioOrder.putAll(order);
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
        //Iterates through the list of imported playlists and finds a playlist with the matching key.
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
