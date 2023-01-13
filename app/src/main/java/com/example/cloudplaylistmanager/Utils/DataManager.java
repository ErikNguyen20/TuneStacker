package com.example.cloudplaylistmanager.Utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Pair;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.ANResponse;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.example.cloudplaylistmanager.Platforms.YoutubeUtilities;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nullable;


public class DataManager {
    private static final String LOG_TAG = "DataManager";
    private static final String SAVED_PREFERENCES_NESTED_TAG = "nested";
    private static final String SAVED_PREFERENCES_IMPORT_TAG = "import";
    private static final String LOCAL_DIRECTORY_AUDIO_STORAGE = "downloaded-songs";
    private static final String LOCAL_DIRECTORY_IMG_STORAGE = "thumbnails";
    private static final String LOCAL_DIRECTORY_CACHE = "cache";
    private static final String DEFAULT_THUMBNAIL_LOW = "default_thumbnail_low";
    private static final String DEFAULT_THUMBNAIL_MED = "default_thumbnail_med";
    private static final String DEFAULT_THUMBNAIL_HIGH = "default_thumbnail_high";
    public static final String DEFAULT_THUMBNAIL = DEFAULT_THUMBNAIL_MED;

    private static final int MAX_AUDIO_DOWNLOAD_RETRIES = 8;
    private static final int MAX_FETCH_AUDIO_INFO_RETRIES = 6;


    private static DataManager instance = null;
    private boolean downloaderInitialized;
    private YoutubeUtilities YtUtilities;
    private File appMusicDirectory;
    private File appCacheDirectory;
    private File appImageDirectory;
    private SharedPreferences sharedPreferences;
    private ContentResolver contentResolver;

    private String dataLastUpdated;
    private HashMap<String, PlaylistInfo> nestedPlaylistData;  //key is UUID
    private HashMap<String, PlaylistInfo> importedPlaylistData;//key is UUID


    private DataManager(Context context) {
        this.YtUtilities = new YoutubeUtilities();
        AndroidNetworking.initialize(context);
        try {
            this.appMusicDirectory = GetLocalMusicDirectory(context);
            this.appImageDirectory = GetLocalImageDirectory(context);
            this.appCacheDirectory = GetLocalCacheDirectory(context);
            this.dataLastUpdated = UUID.randomUUID().toString();
            this.sharedPreferences = context.getSharedPreferences(LOG_TAG,Context.MODE_PRIVATE);
            this.contentResolver = context.getContentResolver();

            YoutubeDL.getInstance().init(context);
            FFmpeg.getInstance().init(context);
            this.downloaderInitialized = true;

            LoadImportedPlaylistsData();
            LoadNestedPlaylistsData();
        } catch(YoutubeDLException e) {
            Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
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
        String jsonImportResult = gson.toJson(this.importedPlaylistData);

        editor.putString(SAVED_PREFERENCES_NESTED_TAG,jsonNestedResult);
        editor.putString(SAVED_PREFERENCES_IMPORT_TAG,jsonImportResult);
        editor.apply();
    }

    /**
     * Saves all imported playlists data to {@link SharedPreferences}.
     */
    public void SaveImportedData() {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().serializeNulls().create();
        String jsonImportResult = gson.toJson(this.importedPlaylistData);

        editor.putString(SAVED_PREFERENCES_IMPORT_TAG,jsonImportResult);
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

        this.importedPlaylistData = new HashMap<>();
        for(Map.Entry<String, PlaylistInfo> entry : imports.entrySet()) {
            PlaylistInfo playlist = entry.getValue();
            playlist.UpdateAllVideos();
            this.importedPlaylistData.put(entry.getKey(),playlist);
        }

        this.dataLastUpdated = UUID.randomUUID().toString();
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

                ArrayList<String> playlistKeys = playlist.GetImportedPlaylistKeys();
                for(int index = 0; index < playlistKeys.size(); index++) {
                    if(this.importedPlaylistData.containsKey(playlistKeys.get(index))) {
                        playlist.ImportPlaylistWithoutUpdatingKeys(playlistKeys.get(index), this.importedPlaylistData.get(playlistKeys.get(index)));
                    }
                }
                this.nestedPlaylistData.put(key,playlist);
            }
        }
        else {//Update all playlists
            HashMap<String, PlaylistInfo> otherPlaylistData = new HashMap<>();
            for(Map.Entry<String, PlaylistInfo> entry : this.nestedPlaylistData.entrySet()) {
                PlaylistInfo playlist = entry.getValue();
                playlist.ClearImportedPlaylists();

                ArrayList<String> playlistKeys = playlist.GetImportedPlaylistKeys();
                for(int index = 0; index < playlistKeys.size(); index++) {
                    if(this.importedPlaylistData.containsKey(playlistKeys.get(index))) {
                        playlist.ImportPlaylistWithoutUpdatingKeys(playlistKeys.get(index), this.importedPlaylistData.get(playlistKeys.get(index)));
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
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            return playlist;
        }
        playlist = this.importedPlaylistData.get(key);
        return playlist;
    }

    /**
     * Renames a playlist from the database.
     * @param key Key of the playlist that is to be renamed from the database.
     * @param newName New name of the playlist.
     */
    public void RenamePlaylist(String key, String newName) {
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
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            playlist.updateLastViewed();
            this.nestedPlaylistData.put(key,playlist);
            this.dataLastUpdated = UUID.randomUUID().toString();
            return;
        }
        playlist = this.importedPlaylistData.get(key);
        if(playlist != null) {
            playlist.updateLastViewed();
            this.importedPlaylistData.put(key,playlist);
            this.dataLastUpdated = UUID.randomUUID().toString();
        }
    }

    /**
     * Adds a new audio source to a given playlist from the database.
     * @param key Key of the playlist that is to be modified from the database.
     * @param audio Newly added audio source.
     */
    public boolean AddSongToPlaylist(String key, PlaybackAudioInfo audio) {
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            if(playlist.ContainsAudio(audio)) {
                return false;
            }
            playlist.AddVideoToPlaylist(audio);
            this.nestedPlaylistData.put(key,playlist);
            this.dataLastUpdated = UUID.randomUUID().toString();
            SaveNestedData();
            return true;
        }
        playlist = this.importedPlaylistData.get(key);
        if(playlist != null) {
            if(playlist.ContainsAudio(audio)) {
                return false;
            }
            playlist.AddVideoToPlaylist(audio);
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
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            boolean success = playlist.RemoveVideo(audioName);
            if(success) {
                this.nestedPlaylistData.put(key, playlist);
                this.dataLastUpdated = UUID.randomUUID().toString();
                SaveNestedData();
            }
            return success;
        }
        playlist = this.importedPlaylistData.get(key);
        if(playlist != null) {
            boolean success = playlist.RemoveVideo(audioName);
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
            this.importedPlaylistData.put(key, playlist);
            if(parentKey != null) {
                PlaylistInfo parentPlaylist = this.nestedPlaylistData.get(parentKey);
                if(parentPlaylist != null) {
                    parentPlaylist.ImportPlaylist(key, playlist);
                }
                this.nestedPlaylistData.put(parentKey,parentPlaylist);
            }
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
     * onError returns 0 if the file already exists.
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
                    audio.setAuthor(streamInfo.getUploader());
                    audio.setThumbnailSource(streamInfo.getThumbnail());
                    audio.setThumbnailType(PlaybackAudioInfo.PlaybackMediaType.STREAM);

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
                        }
                    }
                    downloadFromUrlListener.onError(videoInfoFetchAttemptNumber,"Failed to Fetch Video Information.");

                    //We will retry Fetch
                    videoInfoFetchAttemptNumber++;
                }
            }


            //Preforms a download operation.
            request = new YoutubeDLRequest(url);
            request.addOption("-x");
            request.addOption("--no-playlist");
            request.addOption("--retries",10);
            request.addOption("--no-check-certificate");
            request.addOption("--no-mtime");
            request.addOption("--audio-format", "opus");

            if(successInfo) {
                File searchFile = DoesFileExistWithName(this.appMusicDirectory,audio.getTitle());
                if(searchFile != null) {
                    //If the file already exists, then don't re-download it.
                    successDownload = true;
                }
                request.addOption("-o", this.appMusicDirectory.getAbsolutePath() + File.separator + audio.getTitle() + ".%(ext)s");
            }
            else {
                request.addOption("-o", this.appMusicDirectory.getAbsolutePath() + File.separator + "%(title)s.%(ext)s");
            }

            String responseString = null;
            int downloadAttemptNumber = 1;
            while(downloadAttemptNumber <= MAX_AUDIO_DOWNLOAD_RETRIES && !successDownload) {
                try {
                    YoutubeDLResponse response = YoutubeDL.getInstance().execute(request, (float progress, long etaSeconds, String line) -> {
                        downloadFromUrlListener.onProgressUpdate(progress,etaSeconds);
                    });
                    responseString = response.getOut();

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


            //Sets fallback audio parameters.
            if(!successInfo) {
                audio = new PlaybackAudioInfo();
                int startIndex = responseString.indexOf("[ExtractAudio]");
                int endIndex = responseString.indexOf(".opus");
                if(startIndex != -1 && endIndex != -1) {
                    String sourcePath = responseString.substring(startIndex + 28, endIndex + 5);
                    audio.setTitle(ValidateFileName(sourcePath.substring(this.appMusicDirectory.getAbsolutePath().length()+1,sourcePath.length()-5)));
                }
                else {
                    downloadFromUrlListener.onError(-1,"Could not find reference to Downloaded File.");
                    return;
                }
            }
            audio.setAudioSource(this.appMusicDirectory.getAbsolutePath() + File.separator + audio.getTitle() + ".opus");
            audio.setAudioType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);
            audio.setOrigin(url);


            //Updates the thumbnail source and type.
            if(audio.getThumbnailSource() != null) {
                File searchFile = DoesFileExistWithName(this.appImageDirectory,audio.getTitle());
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

        File fileDestination = new File(this.appMusicDirectory, GetFileNameFromUri(from));

        String destFileName = fileDestination.getName().split("\\.(?=[^\\.]+$)")[0];
        if(GetFileFromDirectory(this.appMusicDirectory,destFileName) != null) {
            downloadListener.onError(-1,"Song already exists.");
            return;
        }

        try{
            inputStream = this.contentResolver.openInputStream(from);
            outputStream = new FileOutputStream(fileDestination);

            byte[] byteArrayBuffer = new byte[1024];
            int length;
            while((length = inputStream.read(byteArrayBuffer)) > 0) {
                outputStream.write(byteArrayBuffer,0,length);
            }

            inputStream.close();
            outputStream.close();

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
        }
    }

    public void VerifyData() {
        //This will check every song within the playlists to see if the
        //song exists within the local file directory.
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
        File[] files = this.appMusicDirectory.listFiles();
        if(files == null) {
            return null;
        }
        PlaylistInfo playlistInfo = new PlaylistInfo();

        HashMap<String,File> directoryMap = GetMapOfFileDirectory(this.appImageDirectory);
        for(File file : files) {
            PlaybackAudioInfo audio = new PlaybackAudioInfo();
            String title = file.getName().split("\\.(?=[^\\.]+$)")[0];

            if(directoryMap.containsKey(title)) {
                audio.setThumbnailSource(directoryMap.get(title).getAbsolutePath());
                audio.setThumbnailType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);
            }
            audio.setTitle(title);
            audio.setAudioSource(file.getAbsolutePath());
            audio.setAudioType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);
            audio.setOrigin(PlaybackAudioInfo.ORIGIN_UPLOAD);
            playlistInfo.AddVideoToPlaylist(audio);
        }

        return playlistInfo;
    }

    /**
     * Constructs a map based on the directory to make it easier to search
     * for a specific title and fetch the file.
     * @param directory Source directory
     * @return Map(String, File)
     */
    public HashMap<String,File> GetMapOfFileDirectory(File directory) {
        File[] files = directory.listFiles();
        if(files == null) {
            return null;
        }
        HashMap<String,File> directoryMap = new HashMap<>();
        for(File file : files) {
            directoryMap.put(file.getName().split("\\.(?=[^\\.]+$)")[0],file);
        }
        return directoryMap;
    }

    /**
     * Checks the specified directory for a file of the given name.
     * @param directory File directory that will be searched.
     * @param fileName Name of the file.
     * @return File by that name.
     */
    public File DoesFileExistWithName(File directory, String fileName) {
        File[] files = directory.listFiles();
        if(files == null) {
            return null;
        }
        for(File file : files) {
            if(fileName.equals(file.getName().split("\\.(?=[^\\.]+$)")[0])) {
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
     * Gets the Cache Directory on the application.
     * Assumes that permissions have been granted to read/write in the external storage.
     * @param context Context of the application.
     * @return File directory path.
     */
    private File GetLocalCacheDirectory(Context context) {
        File appCacheDirectory = context.getExternalFilesDir(DataManager.LOCAL_DIRECTORY_CACHE);
        if (!appCacheDirectory.exists()) {
            appCacheDirectory.mkdir();
        }
        return appCacheDirectory;
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
        Collections.sort(pairArray, (left, right) -> {
            if(left.second.getLastViewed() < right.second.getLastViewed()) {
                return -1;
            }
            else if(left.second.getLastViewed() > right.second.getLastViewed()) {
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
        Cursor cursor = this.contentResolver.query(uri,null,null,null,null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        String returnString = cursor.getString(nameIndex);
        cursor.close();
        return returnString;
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
        if(valid.isEmpty()) {
            return "Unnamed";
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
