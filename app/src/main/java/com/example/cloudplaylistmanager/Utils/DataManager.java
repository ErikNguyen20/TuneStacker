package com.example.cloudplaylistmanager.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Pair;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.ANResponse;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.example.cloudplaylistmanager.Downloader.DownloadListener;
import com.example.cloudplaylistmanager.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;
import com.yausername.youtubedl_android.mapper.VideoInfo;


import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;


/**
 * Manages all Data-Related Operations on this Application.
 * This class is a singleton class. In order for it to function, call the method
 * 'Initialize' before using any operations related to this class.
 */
public class DataManager {
    private static final String LOG_TAG = "DataManager";
    private static final String SAVED_PREFERENCES_NESTED_TAG = "nested";
    private static final String SAVED_PREFERENCES_IMPORT_TAG = "import";
    private static final String SAVED_PREFERENCES_SETTINGS_TAG = "settings";
    private static final String SAVED_PREFERENCES_UPDATE_TIME_TAG = "updatetime";
    private static final String LOCAL_DIRECTORY_AUDIO_STORAGE = "downloaded-songs";
    private static final String LOCAL_DIRECTORY_IMG_STORAGE = "thumbnails";
    private static final String EXPORT_DIRECTORY_NAME = "tunestacker-exports";

    private static final int MAX_AUDIO_DOWNLOAD_RETRIES = 12;
    private static final int MAX_FETCH_AUDIO_INFO_RETRIES = 6;


    private static DataManager instance = null;
    private boolean downloaderInitialized;
    private File appMusicDirectory;
    private File appImageDirectory;
    private File exportDirectory;
    private SharedPreferences sharedPreferences;

    private String dataLastUpdated;
    private HashMap<String, Bitmap> bitmapCache;                //String is the audio name
    private HashMap<String, Integer> lengthCache;               //String is the audio name
    private HashMap<String, PlaylistInfo> nestedPlaylistData;   //key is UUID
    private HashMap<String, PlaylistInfo> importedPlaylistData; //key is UUID
    private SettingsHolder settings;
    private String lastConstructedLocalPlaylist;
    private PlaylistInfo constructedLocalDataPlaylist;
    private final Context context;

    /**
     * Private Constructor for the instance of {@link DataManager}.
     * This class is a singleton class, meaning that there can only be one instance
     * of this class that exists at any given moment.
     * @param context Context of the Application.
     */
    private DataManager(Context context) {
        this.context = context;
        AndroidNetworking.initialize(context);

        try {
            this.appMusicDirectory = GetLocalMusicDirectory(context);
            this.appImageDirectory = GetLocalImageDirectory(context);
            this.exportDirectory = GetExportsDirectory(context);
            this.dataLastUpdated = UUID.randomUUID().toString();
            this.sharedPreferences = context.getSharedPreferences(LOG_TAG,Context.MODE_PRIVATE);
            this.bitmapCache = new HashMap<>();
            this.lengthCache = new HashMap<>();

            //Loads saved data
            LoadSettingsData();
            LoadImportedPlaylistsData();
            LoadNestedPlaylistsData();

            this.lastConstructedLocalPlaylist = UUID.randomUUID().toString();
            this.constructedLocalDataPlaylist = ConstructPlaylistFromLocalFiles();

            //Initializes various libraries.
            YoutubeDL.getInstance().init(context);
            FFmpeg.getInstance().init(context);
            this.downloaderInitialized = true;

        } catch(Exception e) {
            Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
            this.downloaderInitialized = false;
        }
    }

    /**
     * Creates a singleton instance of {@link DataManager}.
     * It also initializes {@link YoutubeDL}, {@link FFmpeg}, {@link AndroidNetworking}.
     * @param context Context of the Application.
     */
    public static void Initialize(Context context) {
        if(instance == null) {
            instance = new DataManager(context);
        }
    }

    /**
     * Fetches the singleton instance of {@link DataManager}.
     * Must call the Initialize Method in order to retrieve a NonNull instance.
     * @return Singleton instance.
     */
    public static DataManager getInstance() {
        return instance;
    }

    /**
     * Saves all nested playlists data to {@link SharedPreferences}.
     */
    public void SaveNestedData() {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().serializeNulls().create();
        String jsonNestedResult = gson.toJson(this.nestedPlaylistData);

        editor.putString(SAVED_PREFERENCES_NESTED_TAG, jsonNestedResult);
        editor.apply();
    }

    /**
     * Saves all imported playlists data to {@link SharedPreferences}.
     */
    public void SaveImportedData() {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().serializeNulls().create();
        String jsonImportResult = gson.toJson(this.importedPlaylistData);

        editor.putString(SAVED_PREFERENCES_IMPORT_TAG, jsonImportResult);
        editor.apply();
    }

    /**
     * Saves all Settings data to {@link SharedPreferences}.
     */
    public void SaveSettingsData() {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().serializeNulls().create();
        String jsonSettingsResult = gson.toJson(this.settings);

        editor.putString(SAVED_PREFERENCES_SETTINGS_TAG, jsonSettingsResult);
        editor.apply();
    }

    /**
     * Saves all last update checked time in milliseconds to {@link SharedPreferences}.
     */
    public void SaveLastUpdateCheck() {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();

        editor.putLong(SAVED_PREFERENCES_UPDATE_TIME_TAG, new Date().getTime());
        editor.apply();
    }

    /**
     * Clears all data from {@link SharedPreferences}.
     */
    public void ClearAllData() {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Loads all of the imported playlists data from {@link SharedPreferences}.
     * This method must be called AFTER loading the imported playlists.
     */
    private void LoadNestedPlaylistsData() {
        //Imports playlist data must be fetched first.
        Gson gson = new Gson();
        String json = this.sharedPreferences.getString(SAVED_PREFERENCES_NESTED_TAG,"");
        Type nestedPlaylistsType = new TypeToken<HashMap<String, PlaylistInfo>>(){}.getType();

        //Constructs the nested playlist data.
        this.nestedPlaylistData = gson.fromJson(json,nestedPlaylistsType);
        if(this.nestedPlaylistData == null) {
            this.nestedPlaylistData = new HashMap<>();
        }

        RefreshNestedPlaylist(null);
        this.dataLastUpdated = UUID.randomUUID().toString();
    }

    /**
     * Loads all of the imported playlists data from {@link SharedPreferences}.
     */
    private void LoadImportedPlaylistsData() {
        //Gets imported playlist through gson and shared preferences.
        Gson gson = new Gson();
        String json = this.sharedPreferences.getString(SAVED_PREFERENCES_IMPORT_TAG,"");
        Type importPlaylistsType = new TypeToken<HashMap<String, PlaylistInfo>>(){}.getType();

        HashMap<String, PlaylistInfo> imports = gson.fromJson(json, importPlaylistsType);
        if(imports == null) {
            imports = new HashMap<>();
        }

        //Constructs the imported playlist data.
        this.importedPlaylistData = new HashMap<>();
        for(Map.Entry<String, PlaylistInfo> entry : imports.entrySet()) {
            PlaylistInfo playlist = entry.getValue();
            playlist.UpdateAllVideos();
            this.importedPlaylistData.put(entry.getKey(),playlist);
        }

        this.dataLastUpdated = UUID.randomUUID().toString();
    }

    /**
     * Loads the Settings Data from {@link SharedPreferences}.
     */
    public void LoadSettingsData() {
        //Gets settings through gson and shared preferences.
        Gson gson = new Gson();
        String json = this.sharedPreferences.getString(SAVED_PREFERENCES_SETTINGS_TAG,"");
        Type settingsType = new TypeToken<SettingsHolder>(){}.getType();

        SettingsHolder fetchedSettings = gson.fromJson(json, settingsType);
        if(fetchedSettings == null) {
            this.settings = new SettingsHolder();
        }
        else {
            this.settings = fetchedSettings;
        }
    }

    /**
     * Gets the time in milliseconds that the app last checked for update {@link SharedPreferences}.
     */
    public long GetLastUpdateTime() {
        return this.sharedPreferences.getLong(SAVED_PREFERENCES_UPDATE_TIME_TAG, 0);
    }

    /**
     * Returns the settings of the application.
     * @return Settings object.
     */
    public SettingsHolder GetSettings() {
        return this.settings;
    }

    /**
     * Sets a field in the settings object to a specified value.
     * @param field Field of the settings item.
     * @param value New value of the field.
     */
    public void SetSettingsField(SettingsHolder.SettingsFields field, Object value) {
        this.settings.changeSettingsItem(field, value);
        SaveSettingsData();
    }

    /**
     * Refreshes the given nested playlist by refreshing all of the imported playlist data
     * that is within the nested playlist. If no key is specified, then the refresh will happen
     * on all nested playlists in the database.
     * @param key Key of the playlist that is to be fetched from the database.
     */
    public void RefreshNestedPlaylist(@Nullable String key) {
        if(key != null) {
            PlaylistInfo playlist = this.nestedPlaylistData.get(key);
            if(playlist != null) {
                playlist.ClearImportedPlaylists();

                //Re-adds the playlists into the nested playlists.
                HashSet<String> playlistKeys = playlist.GetImportedPlaylistKeys();
                for(String entry : playlistKeys) {
                    if(this.importedPlaylistData.containsKey(entry)) {
                        playlist.ImportPlaylistWithoutUpdatingKeys(entry, this.importedPlaylistData.get(entry));
                    }
                }
                this.nestedPlaylistData.put(key,playlist);
            }
        }
        else { //Update all nested playlists
            HashMap<String, PlaylistInfo> otherPlaylistData = new HashMap<>();
            //Iterates through every nested playlist.
            for(Map.Entry<String, PlaylistInfo> entry : this.nestedPlaylistData.entrySet()) {
                PlaylistInfo playlist = entry.getValue();
                playlist.ClearImportedPlaylists();

                //Re-adds the playlists into the nested playlists.
                HashSet<String> playlistKeys = playlist.GetImportedPlaylistKeys();
                for(String entryKey : playlistKeys) {
                    if(this.importedPlaylistData.containsKey(entryKey)) {
                        playlist.ImportPlaylistWithoutUpdatingKeys(entryKey, this.importedPlaylistData.get(entryKey));
                    }
                }
                otherPlaylistData.put(entry.getKey(), playlist);
            }
            this.nestedPlaylistData.clear();
            this.nestedPlaylistData.putAll(otherPlaylistData);
        }
        this.dataLastUpdated = UUID.randomUUID().toString();
    }

    /**
     * Returns all nested playlists from the database.
     * @return Fetched playlists.
     */
    public ArrayList<Pair<String, PlaylistInfo>> GetNestedPlaylists() {
        if(this.nestedPlaylistData == null) {
            LoadNestedPlaylistsData();
        }
        return PlaylistMapToArraylist(this.nestedPlaylistData);
    }

    /**
     * Returns all imported playlists from the database.
     * @return Fetched playlists.
     */
    public ArrayList<Pair<String, PlaylistInfo>> GetImportedPlaylists() {
        if(this.importedPlaylistData == null) {
            LoadImportedPlaylistsData();
        }
        return PlaylistMapToArraylist(this.importedPlaylistData);
    }

    /**
     * Returns a specified playlist from the database.
     * @param key Key of the playlist that is to be fetched from the database.
     * @return Fetched playlist. Null if not found.
     */
    public PlaylistInfo GetPlaylistFromKey(String key) {
        if(key == null || key.isEmpty()) {
            return null;
        }
        //If the key belongs to a nested playlist, return the item.
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            return playlist;
        }
        //If the key belongs to an imported playlist, return the item.
        playlist = this.importedPlaylistData.get(key);
        return playlist;
    }

    /**
     * Renames a playlist from the database.
     * @param key Key of the playlist that is to be renamed from the database.
     * @param newName New name of the playlist.
     */
    public void RenamePlaylist(String key, String newName) {
        //If the key belongs to a nested playlist, update it.
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            if(playlist.getTitle().equals(newName) || newName.isEmpty()) {
                return;
            }
            playlist.setTitle(newName);
            this.nestedPlaylistData.put(key,playlist);
            this.dataLastUpdated = UUID.randomUUID().toString();
            SaveNestedData();
            return;
        }
        //If the key belongs to an imported playlist, rename it.
        playlist = this.importedPlaylistData.get(key);
        if(playlist != null) {
            if(playlist.getTitle().equals(newName) || newName.isEmpty()) {
                return;
            }
            playlist.setTitle(newName);
            this.importedPlaylistData.put(key,playlist);
            RefreshNestedPlaylist(null);
            this.dataLastUpdated = UUID.randomUUID().toString();
            SaveImportedData();
        }
    }

    /**
     * Update's a playlist's last viewed value. This determines the order in which the
     * playlists are displayed in the views, with the most recently viewed playlist being first.
     * @param key Key of the playlist that is to be modified from the database.
     */
    public void UpdatePlaylistLastViewed(String key) {
        //If the key belongs to a nested playlist, update it.
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            playlist.updateLastViewed();
            this.nestedPlaylistData.put(key,playlist);
            this.dataLastUpdated = UUID.randomUUID().toString();
            SaveNestedData();
            return;
        }
        //If the key belongs to an imported playlist, update it.
        playlist = this.importedPlaylistData.get(key);
        if(playlist != null) {
            playlist.updateLastViewed();
            this.importedPlaylistData.put(key,playlist);
            this.dataLastUpdated = UUID.randomUUID().toString();
            SaveImportedData();
        }
    }

    /**
     * Adds a new audio source to a given playlist from the database.
     * @param key Key of the playlist that is to be modified from the database.
     * @param audio Newly added audio source.
     */
    public boolean AddSongToPlaylist(String key, PlaybackAudioInfo audio) {
        //If the key belongs to an nested playlist, add to it.
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            if(playlist.ContainsAudio(audio)) {
                return false;
            }
            playlist.AddAudioToPlaylist(audio);
            this.nestedPlaylistData.put(key,playlist);
            this.dataLastUpdated = UUID.randomUUID().toString();
            SaveNestedData();
            return true;
        }
        //If the key belongs to an imported playlist, add to it.
        playlist = this.importedPlaylistData.get(key);
        if(playlist != null) {
            if(playlist.ContainsAudio(audio)) {
                return false;
            }
            playlist.AddAudioToPlaylist(audio);
            this.importedPlaylistData.put(key,playlist);
            RefreshNestedPlaylist(null);
            this.dataLastUpdated = UUID.randomUUID().toString();
            SaveImportedData();
            return true;
        }
        return false;
    }

    /**
     * Removes a song from the specified playlist using the audio's name.
     * @param key Key of the playlist that is to be modified from the database.
     * @param audioName Name of the audio.
     * @return If the audio was successfully removed.
     */
    public boolean RemoveSongFromPlaylist(String key, String audioName) {
        if(audioName == null || audioName.isEmpty()) {
            return false;
        }
        //If the key belongs to a nested playlist, update it.
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            boolean success = playlist.RemoveAudio(audioName);
            if(success) {
                this.nestedPlaylistData.put(key, playlist);
                this.dataLastUpdated = UUID.randomUUID().toString();
                SaveNestedData();
            }
            return success;
        }
        //If the key belongs to an imported playlist, remove from it.
        playlist = this.importedPlaylistData.get(key);
        if(playlist != null) {
            boolean success = playlist.RemoveAudio(audioName);
            if(success) {
                this.importedPlaylistData.put(key, playlist);
                RefreshNestedPlaylist(null);
                this.dataLastUpdated = UUID.randomUUID().toString();
                SaveImportedData();
            }
            return success;
        }
        return false;
    }

    /**
     * Removes a song from ALL playlists in the Data.
     * @param audioName Name of the audio.
     */
    public void RemoveSongFromAll(String audioName) {
        if(audioName == null || audioName.isEmpty()) {
            return;
        }
        boolean hasChanged = false;

        //Removes the audio from imported playlists.
        HashMap<String, PlaylistInfo> copyOfImports = new HashMap<>();
        for(Map.Entry<String, PlaylistInfo> entry : this.importedPlaylistData.entrySet()) {
            PlaylistInfo value = entry.getValue();
            //Only updates imported playlist if successfully removed the audio.
            boolean removed = value.RemoveAudio(audioName);
            if(removed) {
                hasChanged = true;
                copyOfImports.put(entry.getKey(), value);
            }
        }
        this.importedPlaylistData.putAll(copyOfImports);

        //Removes the audio from nested playlists.
        HashMap<String, PlaylistInfo> copyOfNested = new HashMap<>();
        for(Map.Entry<String, PlaylistInfo> entry : this.nestedPlaylistData.entrySet()) {
            PlaylistInfo value = entry.getValue();
            //Only updates nested playlist if successfully removed the audio.
            boolean removed = value.RemoveAudio(audioName);
            if(removed) {
                hasChanged = true;
                copyOfNested.put(entry.getKey(), value);
            }
        }
        this.nestedPlaylistData.putAll(copyOfNested);

        //Saves the data.
        if(hasChanged) {
            RefreshNestedPlaylist(null);
            SaveImportedData();
            SaveNestedData();
        }
    }

    /**
     * Sets the loose ordering list of the specified playlist.
     * @param key Key of the playlist that is to be modified from the database.
     * @param order Order in which the items should be placed.
     */
    public void UpdateOrderOfItemsInPlaylist(String key, HashMap<String, Integer> order) {
        //If the key belongs to an nested playlist, update it.
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            playlist.SetItemsOrder(order);
            this.nestedPlaylistData.put(key, playlist);
            this.dataLastUpdated = UUID.randomUUID().toString();
            SaveNestedData();
            return;
        }
        //If the key belongs to an imported playlist, update it.
        playlist = this.importedPlaylistData.get(key);
        if(playlist != null) {
            playlist.SetItemsOrder(order);
            this.importedPlaylistData.put(key, playlist);
            this.dataLastUpdated = UUID.randomUUID().toString();
            SaveImportedData();
        }
    }

    /**
     * Adds an already existing imported playlist into a nested playlist.
     * @param nested Key of the nested playlist.
     * @param imported Key of the imported playlist.
     */
    public boolean AddImportPlaylistToNested(String nested, String imported) {
        PlaylistInfo nestedPlaylist = this.nestedPlaylistData.get(nested);
        PlaylistInfo importedPlaylist = this.importedPlaylistData.get(imported);
        if(nestedPlaylist != null && importedPlaylist != null) {
            if(nestedPlaylist.ContainsImportedKey(imported)) {
                return false;
            }
            nestedPlaylist.ImportPlaylist(imported, importedPlaylist);
            this.dataLastUpdated = UUID.randomUUID().toString();
            SaveNestedData();
            return true;
        }
        return false;
    }

    /**
     * Removes a playlist from the database.
     * @param key Key of the playlist that is to be removed from the database.
     * @param parentKey If the new playlist is imported and is being removed from within a nested playlist,
     *                  declare a value for the parentKey, which is the key of the nested playlist.
     */
    public boolean RemovePlaylist(String key, @Nullable String parentKey) {
        if(parentKey != null) {
            PlaylistInfo playlist = this.nestedPlaylistData.get(parentKey);
            if(playlist != null) {
                playlist.RemoveImportedPlaylist(key);
                this.nestedPlaylistData.put(parentKey,playlist);
                this.dataLastUpdated = UUID.randomUUID().toString();
                SaveNestedData();
                return true;
            }
        }
        else {
            PlaylistInfo removedPlaylist = this.nestedPlaylistData.remove(key);
            if(removedPlaylist != null) {
                this.dataLastUpdated = UUID.randomUUID().toString();
                SaveNestedData();
                return true;
            }
            removedPlaylist = this.importedPlaylistData.remove(key);
            if(removedPlaylist == null) {
                return false;
            }

            //Remove from all other nested playlists
            HashMap<String, PlaylistInfo> copyOfNested = new HashMap<>();
            for(Map.Entry<String, PlaylistInfo> entry : this.nestedPlaylistData.entrySet()) {
                PlaylistInfo value = entry.getValue();
                value.RemoveImportedPlaylist(key);
                copyOfNested.put(entry.getKey(), value);
            }
            this.nestedPlaylistData.putAll(copyOfNested);

            this.dataLastUpdated = UUID.randomUUID().toString();
            SaveImportedData();
            return true;
        }
        return false;
    }

    /**
     * Creates a new playlist and places it into the database.
     * @param playlist Playlist data of the new playlist.
     * @param isNested Indicates whether or not the new playlist is a nested playlist or an imported one.
     * @param parentKey If the new playlist is imported and is being added within a nested playlist, declare
     *                  a value for the parentKey, which is the key of the nested playlist.
     */
    public String CreateNewPlaylist(PlaylistInfo playlist, boolean isNested, @Nullable String parentKey) {
        String key = UUID.randomUUID().toString();
        if(isNested) {
            this.nestedPlaylistData.put(key, playlist);
        }
        else {
            if(parentKey != null) {
                PlaylistInfo parentPlaylist = this.nestedPlaylistData.get(parentKey);
                if(parentPlaylist != null) {
                    parentPlaylist.ImportPlaylist(key, playlist);
                }
                this.nestedPlaylistData.put(parentKey,parentPlaylist);
            }
            else {
                playlist.SetItemsOrder(playlist.getAllVideos());
            }
            this.importedPlaylistData.put(key, playlist);
            SaveImportedData();
        }
        SaveNestedData();
        this.dataLastUpdated = UUID.randomUUID().toString();
        return key;
    }

    /**
     * Fetches the most recent update identifier. A different value from the one fetched
     * previous indicates that the dataset has been updated/changed.
     * @return Last Update Identifier.
     */
    public String GetDataLastUpdate() {
        return this.dataLastUpdated;
    }

    /**
     * Downloads song with the given, valid URL into the MUSIC directory.
     * It is required to implement {@link DownloadListener} to obtain the
     * result of this call and to catch potential errors.
     * onError returns -1 in the attempt parameter if a critical failure occurs.
     * @param url Url of the Audio source.
     * @param downloadFromUrlListener Listener used to get the results/errors of this call.
     */
    public void DownloadSongToDirectoryFromUrl(String url, DownloadListener downloadFromUrlListener) {
        if(!this.downloaderInitialized) {
            downloadFromUrlListener.onError(-1,"Downloader failed to initialize on startup, thus it is in a failed state.");
            return;
        }
        Thread thread = new Thread(() -> {
            //Fetches audio information from the source.
            YoutubeDLRequest request = new YoutubeDLRequest(url);
            request.addOption("--no-playlist");
            request.addOption("--retries",10);
            request.addOption("--no-check-certificate");

            PlaybackAudioInfo audio = new PlaybackAudioInfo();
            int videoInfoFetchAttemptNumber = 1;
            boolean successInfo = false;
            boolean successDownload = false;
            while(videoInfoFetchAttemptNumber <= MAX_FETCH_AUDIO_INFO_RETRIES && !successInfo) {
                try {
                    VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(request);
                    audio.setTitle(ValidateFileName(streamInfo.getTitle()));
                    audio.setThumbnailSource(streamInfo.getThumbnail());
                    audio.setThumbnailType(PlaybackAudioInfo.PlaybackMediaType.STREAM);
                    audio.setAudioSource(this.appMusicDirectory.getAbsolutePath() + File.separator + audio.getTitle() + "." + this.settings.extension);
                    audio.setAudioType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);
                    audio.setOrigin(url);

                    successInfo = true;
                } catch (YoutubeDLException | InterruptedException e) {
                    Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
                    e.printStackTrace();
                    if(e.getMessage() != null) {
                        if(e.getMessage().contains("not a valid URL.")) {
                            downloadFromUrlListener.onError(-1,"Not a valid URL.");
                            return;
                        } else if(e.getMessage().contains("Video unavailable")) {
                            downloadFromUrlListener.onError(-1,"Video is unavailable.");
                            return;
                        } else if(e.getMessage().contains("Private video")) {
                            downloadFromUrlListener.onError(-1,"Video is Private.");
                        }
                    }
                    downloadFromUrlListener.onError(videoInfoFetchAttemptNumber,"Failed to Fetch Video Information.");

                    //We will retry Fetch
                    videoInfoFetchAttemptNumber++;
                }
            }

            if(successInfo) {
                File searchFile = DoesFileExistWithName(this.appMusicDirectory,audio.getTitle(),"audio");
                if(searchFile != null) {
                    //If the file already exists, then don't re-download it.
                    audio.setAudioSource(searchFile.getAbsolutePath());
                    successDownload = true;
                }
            }
            else {
                downloadFromUrlListener.onError(-1,"Failed to fetch audio information.");
                return;
            }

            //Performs a download operation.
            request = new YoutubeDLRequest(url);
            request.addOption("-x");
            request.addOption("--no-playlist");
            request.addOption("--retries",10);
            request.addOption("--no-check-certificate");
            request.addOption("--no-mtime");
            if(this.settings.embedThumbnail) {
                request.addOption("--embed-thumbnail");
            }
            request.addOption("--audio-format", this.settings.extension);
            request.addOption("-o", this.appMusicDirectory.getAbsolutePath() + File.separator + audio.getTitle() + ".%(ext)s");

            int downloadAttemptNumber = 1;
            while(downloadAttemptNumber <= MAX_AUDIO_DOWNLOAD_RETRIES && !successDownload) {
                try {
                    YoutubeDLResponse response = YoutubeDL.getInstance().execute(request, (float progress, long etaSeconds, String line) -> {
                        downloadFromUrlListener.onProgressUpdate(progress,etaSeconds);
                    });

                    successDownload = true;
                } catch (YoutubeDLException | InterruptedException e) {
                    Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
                    e.printStackTrace();
                    downloadFromUrlListener.onError(downloadAttemptNumber,"Failed to Download from the given URL. Retrying...");

                    //We will retry download
                    downloadAttemptNumber++;
                }
            }
            if(!successDownload) {
                downloadFromUrlListener.onError(-1,"Download Attempts exceeded threshold.");
                return;
            }


            //Updates the thumbnail source and type. Will also download the thumbnail.
            if((audio.getThumbnailSource() != null || !audio.getThumbnailSource().isEmpty()) &&
                    this.settings.downloadThumbnail) {
                File searchFile = DoesFileExistWithName(this.appImageDirectory,audio.getTitle(),null);
                if(searchFile == null) {
                    File newThumbnail = DownloadToLocalStorage(audio.getThumbnailSource(), this.appImageDirectory, audio.getTitle() + ".bin");
                    if (newThumbnail != null) {
                        audio.setThumbnailSource(newThumbnail.getAbsolutePath());
                        audio.setThumbnailType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);
                    }
                }
                else {
                    audio.setThumbnailSource(searchFile.getAbsolutePath());
                    audio.setThumbnailType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);
                }
            }

            this.dataLastUpdated = UUID.randomUUID().toString();
            downloadFromUrlListener.onComplete(audio);
        });

        thread.start();
    }

    /**
     * Downloads song with the given file source directory.
     * It is required to implement {@link DownloadListener} to obtain the
     * result of this call and to catch potential errors.
     * onError returns -1 in the attempt parameter if a critical failure occurs.
     * @param from File Uri of the source file.
     * @param downloadListener Listener used to get the results/errors of this call.
     */
    public void DownloadFileFromDirectoryToDirectory(Uri from, DownloadListener downloadListener) {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        //Gets file destination and checks to see if the file already exists.
        File fileDestination = new File(this.appMusicDirectory, GetFileNameFromUri(from));

        String destFileName = fileDestination.getName().split("\\.(?=[^\\.]+$)")[0];
        if(GetFileFromDirectory(this.appMusicDirectory,destFileName) != null) {
            downloadListener.onError(-1,"Song already exists.");
            return;
        }

        try{
            //Writes bytes from the input stream into the output stream.
            inputStream = this.context.getContentResolver().openInputStream(from);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                outputStream = Files.newOutputStream(fileDestination.toPath());
            }
            else {
                outputStream = new FileOutputStream(fileDestination);
            }

            byte[] byteArrayBuffer = new byte[1024];
            int length;
            while((length = inputStream.read(byteArrayBuffer)) > 0) {
                outputStream.write(byteArrayBuffer,0,length);
            }

            inputStream.close();
            outputStream.flush();
            outputStream.close();

            //Returns a PlaybackAudioInfo with the audio data.
            PlaybackAudioInfo audio = new PlaybackAudioInfo();
            audio.setOrigin(PlaybackAudioInfo.ORIGIN_UPLOAD);
            audio.setAudioSource(fileDestination.getAbsolutePath());
            audio.setAudioType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);
            audio.setTitle(fileDestination.getName().split("\\.(?=[^\\.]+$)")[0]);
            audio.setThumbnailType(PlaybackAudioInfo.PlaybackMediaType.UNKNOWN);

            this.dataLastUpdated = UUID.randomUUID().toString();
            downloadListener.onComplete(audio);
        } catch(Exception e) {
            Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
            downloadListener.onError(-1,"File failed to download.");

            //Tries to close input/output stream
            if(inputStream != null) {
                try{
                    inputStream.close();
                } catch (IOException ignored) {}
            }
            if(outputStream != null) {
                try{
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Exports the specified playlist into the musics directory.
     * @param playlistKey Key of the playlist.
     */
    public void ExportPlaylist(String playlistKey, ExportListener exportListener) {
        Thread thread = new Thread(() -> {
            PlaylistInfo playlist = GetPlaylistFromKey(playlistKey);
            if(playlist != null) {
                String playlistDirectoryName = ValidateFileName(playlist.getTitle());

                //Iterates through all of the audios in the playlist and exports each one.
                for(PlaybackAudioInfo audio : playlist.getAllVideos()) {
                    ExportSong(audio.getTitle(), playlistDirectoryName);
                    exportListener.onProgress("Exporting: " + audio.getTitle());
                }
                exportListener.onComplete("Successfully Exported Playlist.");
                return;
            }
            exportListener.onComplete("Failed to export playlist.");
        });
        thread.start();
    }

    /**
     * Exports the specified song into the musics directory.
     * @param audioTitle Title of the audio.
     */
    public boolean ExportSong(String audioTitle, String optionalDirectory) {
        File audioFile = DoesFileExistWithName(this.appMusicDirectory,audioTitle,"audio");
        if(audioFile == null) {
            return false;
        }

        InputStream audioInputStream = null;
        OutputStream audioOutputStream = null;

        try {
            //If optionalDirectory is defined, create a folder to place the contents in instead.
            File fileDestination;
            if(optionalDirectory != null) {
                File nextDirectory = new File(this.exportDirectory, optionalDirectory);
                if(!nextDirectory.exists()) {
                    nextDirectory.mkdirs();
                }
                fileDestination = new File(nextDirectory, audioFile.getName());
            }
            else {
                fileDestination = new File(this.exportDirectory, audioFile.getName());
            }

            //If the settings do not allow overrides and the file exists, then do not export.
            if(!this.settings.overrideExport && fileDestination.exists()) {
                return false;
            }

            //Writes bytes from the input stream into the output stream.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioInputStream = Files.newInputStream(audioFile.toPath());
                audioOutputStream = Files.newOutputStream(fileDestination.toPath());
            }
            else {
                audioInputStream = new FileInputStream(audioFile);
                audioOutputStream = new FileOutputStream(fileDestination);
            }


            byte[] byteArrayBuffer = new byte[1024];
            int length;
            while((length = audioInputStream.read(byteArrayBuffer)) > 0) {
                audioOutputStream.write(byteArrayBuffer,0,length);
            }

            audioInputStream.close();
            audioOutputStream.flush();
            audioOutputStream.close();

            //Scans media item.
            MediaScannerConnection.scanFile(this.context, new String[]{fileDestination.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String s, Uri uri) {
                    Log.d(LOG_TAG, "Scanned: " + s);
                    Log.d(LOG_TAG, "Uri: " + uri);
                }
            });
            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
            e.printStackTrace();

            //Tries to close input/output stream
            if(audioInputStream != null) {
                try{
                    audioInputStream.close();
                } catch (IOException ignored) {}
            }
            if(audioOutputStream != null) {
                try{
                    audioOutputStream.flush();
                    audioOutputStream.close();
                } catch (IOException ignored) {}
            }

            return false;
        }
    }

    /**
     * Permanently deletes an audio from all playlists and the local files.
     * @param audioTitle Name of the audio
     * @return If it was successfully deleted.
     */
    public boolean DeleteSong(String audioTitle) {
        File audioFile = DoesFileExistWithName(this.appMusicDirectory,audioTitle,"audio");
        if(audioFile == null) {
            return false;
        }
        File thumbFile = DoesFileExistWithName(this.appImageDirectory,audioTitle,null);

        //If the file exists, perform a deletion.
        if(audioFile.exists()) {
            RemoveSongFromAll(audioTitle);
            if(thumbFile != null && thumbFile.exists()) {
                thumbFile.delete();
            }
            boolean deleteSuccess = audioFile.delete();
            this.dataLastUpdated = UUID.randomUUID().toString();
            return deleteSuccess;
        }
        return false;
    }

    /**
     * Synchronously Downloads file with given url to local storage.
     * Must not be called from the Main Thread.
     * @param link Link to json on web.
     * @param directory Working directory.
     * @param fileName Name of the File + Extension ex: "accounts.json"
     * @return File path to the downloaded file.
     */
    public File DownloadToLocalStorage(String link, File directory, String fileName) {
        //Checks to see if it already exists.
        File checkFile = GetFileFromDirectory(directory, fileName);
        if(checkFile != null) {
            return checkFile;
        }

        //Downloads the file.
        final CountDownLatch latch = new CountDownLatch(1);
        AndroidNetworking.download(link, directory.getAbsolutePath(), fileName)
                .setPriority(Priority.MEDIUM)
                .build().startDownload(new com.androidnetworking.interfaces.DownloadListener() {
            @Override
            public void onDownloadComplete() {
                Log.d(LOG_TAG, "Download Complete");
                latch.countDown();
            }
            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() != 0) {
                    Log.e(LOG_TAG, "DownloadToLocalStorage errorCode : " + anError.getErrorCode());
                    Log.e(LOG_TAG, "DownloadToLocalStorage errorBody : " + anError.getErrorBody());
                    Log.e(LOG_TAG, "DownloadToLocalStorage errorDetail : " + anError.getErrorDetail());
                }
                else {
                    Log.e(LOG_TAG, "DownloadToLocalStorage errorDetail : " + anError.getErrorDetail());
                }
                latch.countDown();
            }
        });

        try{
            latch.await();
        } catch(InterruptedException e) {
            Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "Latch was interrupted.");
            e.printStackTrace();
        }

        return GetFileFromDirectory(directory, fileName);
    }

    /**
     * Synchronous Get Request using Android Fast Networking.
     * This function CANNOT be called from the main thread.
     * @param url Url of the request.
     * @param queryParams Map with the query requests as key-value pairs
     * @return JSON result
     */
    public static JSONObject MakeGetRequest(String url, Map queryParams) {
        ANRequest request = AndroidNetworking.get(url)
                .addQueryParameter(queryParams)
                .build();
        ANResponse<JSONObject> response = request.executeForJSONObject();
        if (response.isSuccess()) {
            return (JSONObject) response.getResult();
        } else {
            ANError error = response.getError();
            if (error.getErrorCode() != 0) {
                Log.e(LOG_TAG, "MakeGetRequest errorCode : " + error.getErrorCode());
                Log.e(LOG_TAG, "MakeGetRequest errorBody : " + error.getErrorBody());
                Log.e(LOG_TAG, "MakeGetRequest errorDetail : " + error.getErrorDetail());
            }
            else {
                Log.e(LOG_TAG, "MakeGetRequest errorDetail : " + error.getErrorDetail());
            }
            error.printStackTrace();
            return null;
        }
    }

    /**
     * Constructs a playlist based on the internal directory
     * @return PlaylistInfo
     */
    public PlaylistInfo ConstructPlaylistFromLocalFiles() {
        if(this.lastConstructedLocalPlaylist.equals(this.dataLastUpdated)) {
            return this.constructedLocalDataPlaylist;
        }

        //Gets a list of files from the directory.
        File[] files = this.appMusicDirectory.listFiles();
        if(files == null) {
            return null;
        }
        PlaylistInfo playlistInfo = new PlaylistInfo();
        playlistInfo.setTitle("Saved Songs");

        //Iterates through all of the files and constructs an PlaybackAudioInfo class based on the data.
        HashMap<String,File> directoryMap = GetMapOfFileDirectory(this.appImageDirectory,null);
        for(File file : files) {
            PlaybackAudioInfo audio = new PlaybackAudioInfo();
            String title = file.getName().split("\\.(?=[^\\.]+$)")[0];

            //Checks to see if a thumbnail image exists for the audio.
            if(directoryMap.containsKey(title)) {
                audio.setThumbnailSource(directoryMap.get(title).getAbsolutePath());
                audio.setThumbnailType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);
            }
            audio.setTitle(title);
            audio.setAudioSource(file.getAbsolutePath());
            audio.setAudioType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);
            audio.setOrigin(PlaybackAudioInfo.ORIGIN_UPLOAD);
            playlistInfo.AddAudioToPlaylist(audio);
        }

        this.lastConstructedLocalPlaylist = this.dataLastUpdated;
        this.constructedLocalDataPlaylist = playlistInfo;
        return playlistInfo;
    }

    /**
     * Constructs a map based on the directory to make it easier to search
     * for a specific title and fetch the file.
     * @param directory Source directory
     * @return Map(String, File)
     */
    public HashMap<String,File> GetMapOfFileDirectory(File directory, String defaultmime) {
        File[] files = directory.listFiles();
        if(files == null) {
            return null;
        }
        HashMap<String,File> directoryMap = new HashMap<>();
        for(File file : files) {
            if(defaultmime == null || GetMimeType(file,"").contains(defaultmime)) {
                directoryMap.put(file.getName().split("\\.(?=[^\\.]+$)")[0],file);
            }
        }
        return directoryMap;
    }

    /**
     * Checks the specified directory for a file of the given name.
     * @param directory File directory that will be searched.
     * @param fileName Name of the file.
     * @return File by that name.
     */
    public File DoesFileExistWithName(File directory, String fileName, String optionalMime) {
        File[] files = directory.listFiles();
        if(files == null) {
            return null;
        }
        for(File file : files) {
            if(fileName.equals(file.getName().split("\\.(?=[^\\.]+$)")[0])) {
                if(optionalMime != null && !GetMimeType(file,"").contains(optionalMime)) {
                    continue;
                }
                return file;
            }
        }
        return null;
    }

    /**
     * Gets the Music Directory on the application.
     * Assumes that permissions have been granted to read/write in the external storage.
     * @param context Context of the application.
     * @return File directory path.
     */
    private File GetLocalMusicDirectory(Context context) {
        File appMusicDirectory = context.getExternalFilesDir(DataManager.LOCAL_DIRECTORY_AUDIO_STORAGE);
        if (!appMusicDirectory.exists()) {
            appMusicDirectory.mkdir();
        }
        return appMusicDirectory;
    }

    /**
     * Gets the Image Directory on the application.
     * Assumes that permissions have been granted to read/write in the external storage.
     * @param context Context of the application.
     * @return File directory path.
     */
    private File GetLocalImageDirectory(Context context) {
        File appImageDirectory = context.getExternalFilesDir(DataManager.LOCAL_DIRECTORY_IMG_STORAGE);
        if (!appImageDirectory.exists()) {
            appImageDirectory.mkdir();
        }
        return appImageDirectory;
    }

    /**
     * Gets the Image Directory on the application.
     * Assumes that permissions have been granted to read/write in the external storage.
     * @param context Context of the application.
     * @return File directory path.
     */
    private File GetExportsDirectory(Context context) {
        File musicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        if(!musicDirectory.exists()) {
            musicDirectory.mkdir();
        }
        File exportDirectory = new File(musicDirectory, EXPORT_DIRECTORY_NAME);
        if (!exportDirectory.exists()) {
            exportDirectory.mkdirs();
        }
        return exportDirectory;
    }

    /**
     * Fetches a file object from the local directory with given file name.
     * @param fileName Name of the file.
     * @return File object.
     */
    public File GetFileFromDirectory(File directory, String fileName) {
        File searchFile = new File(directory,fileName);
        if(searchFile.exists()) {
            return searchFile;
        }
        else {
            return null;
        }
    }

    /**
     * Converts a hash map of playlists into a sorted arraylist.
     * @param map Hashmap of playlists.
     * @return Sorted Arraylist of playlists.
     */
    public ArrayList<Pair<String, PlaylistInfo>> PlaylistMapToArraylist(HashMap<String, PlaylistInfo> map) {
        ArrayList<Pair<String, PlaylistInfo>> pairArray = new ArrayList<>();

        for(Map.Entry<String, PlaylistInfo> entry : map.entrySet()) {
            String key = entry.getKey();
            PlaylistInfo value = entry.getValue();
            pairArray.add(new Pair<>(key, value));
        }
        //Sorts the playlists by last viewed.
        Collections.sort(pairArray, (left, right) -> {
            if(left.second.getLastViewed() > right.second.getLastViewed()) {
                return -1;
            }
            else if(left.second.getLastViewed() < right.second.getLastViewed()) {
                return 1;
            }
            return 0;
        });

        return pairArray;
    }

    /**
     * Gets the name of the file with a Uri source.
     * @param uri File uri source.
     * @return Name of the file.
     */
    public String GetFileNameFromUri(Uri uri) {
        if(uri == null) {
            return null;
        }
        Cursor cursor = this.context.getContentResolver().query(uri,null,null,null,null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String returnString = cursor.getString(nameIndex);
        cursor.close();
        return returnString;
    }

    /**
     * Gets the mime type of the file.
     * @param file File.
     * @return Mime Type.
     */
    public String GetMimeType(File file, String defaultMime) {
        String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1);

        if(MimeTypeMap.getSingleton().hasExtension(extension)) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return defaultMime;
    }

    /**
     * Gets the duration of the audio in milliseconds
     * @param audio Audio source.
     * @return milliseconds length. -1 if failed.
     */
    public int GetAudioDuration(PlaybackAudioInfo audio) {
        if(this.lengthCache.containsKey(audio.getTitle())) {
            Integer length = this.lengthCache.get(audio.getTitle());
            if(length == null) {
                return -1;
            }
            else {
                return length;
            }
        }
        //Retrieves the audio length.
        try{
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(audio.getAudioSource());
            int duration = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            this.lengthCache.put(audio.getTitle(), duration);
            retriever.close();
            return duration;
        } catch(Exception ignore) {}

        return -1;
    }

    /**
     * Gets the Bitmap of the thumbnail of the audio directly from the cache.
     * @param audio Audio source.
     * @return Bitmap, returns null if it doesn't exist.
     */
    public Bitmap GetThumbnailImageCache(PlaybackAudioInfo audio) {
        if(this.bitmapCache.containsKey(audio.getTitle())) {
            return this.bitmapCache.get(audio.getTitle());
        }
        return null;
    }

    /**
     * Gets the Bitmap of the thumbnail of the audio.
     * @param audio Audio source.
     * @return Bitmap, returns null if it doesn't exist.
     */
    public Bitmap GetThumbnailImage(PlaybackAudioInfo audio) {
        if(this.bitmapCache.containsKey(audio.getTitle())) {
            return this.bitmapCache.get(audio.getTitle());
        }
        Bitmap bitmap = null;

        if(audio.getThumbnailType() == PlaybackAudioInfo.PlaybackMediaType.LOCAL) {
            //Gets bitmap from external thumbnail file.
            try { bitmap = BitmapFactory.decodeFile(audio.getThumbnailSource()); } catch(Exception ignore) {}
            if(bitmap == null) {
                try {
                    //Gets bitmap from embedded thumbnail image.
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(audio.getAudioSource());
                    byte[] data = mmr.getEmbeddedPicture();
                    if (data != null) {
                        bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    }
                    mmr.close();
                } catch (Exception ignore) {}
            }
        }
        //If all other methods of getting the bitmap failed, get the default.
        if(bitmap == null) {
            bitmap = BitmapFactory.decodeResource(this.context.getResources(), R.drawable.med_res);
        }
        this.bitmapCache.put(audio.getTitle(), bitmap);

        return bitmap;
    }

    /**
     * Converts the given string into a string that is valid for a file name in android's
     * file system. Invalid Characters are listed here:
     * https://stackoverflow.com/a/35352640
     * @param fileName File name that will be converted.
     * @return Name of the file.
     */
    public static String ValidateFileName(String fileName) {
        String valid = fileName.trim(); //Remotes leading and ending spaces.
        //Limits character length to 250 characters.
        if(valid.length() > 250) {
            valid = valid.substring(0,250);
        }

        //Remove all control characters + DELETE(U+007F)
        valid = valid.replaceAll("[\\x{0000}-\\x{001F}\\x{007F}]","");

        //Replaces invalid characters with similar-looking Unicode characters.
        valid = valid.replace('"','\uFF02');
        valid = valid.replace('*','\uFF0A');
        valid = valid.replace('/','\uFF0F');
        valid = valid.replace(':','\uFF1A');
        valid = valid.replace('<','\uFF1C');
        valid = valid.replace('>','\uFF1E');
        valid = valid.replace('?','\uFF1F');
        valid = valid.replace('\\','\uFF3C');
        valid = valid.replace('|','\uFF5C');

        if(valid.charAt(valid.length()-1) == '.') { //Replaces period at the end of the file name.
            valid = valid.substring(0,valid.length()-1) + '\uFF0E';
        }

        return valid;
    }
}
