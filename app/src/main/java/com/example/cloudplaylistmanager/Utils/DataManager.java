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
    private static final int MAX_FETCH_AUDIO_INFO_RETRIES = 4;


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
        this.YtUtilities = new YoutubeUtilities(context);
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


    public void SaveData() {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().serializeNulls().create();
        String jsonNestedResult = gson.toJson(this.nestedPlaylistData);
        String jsonImportResult = gson.toJson(this.importedPlaylistData);

        editor.putString(SAVED_PREFERENCES_NESTED_TAG,jsonNestedResult);
        editor.putString(SAVED_PREFERENCES_IMPORT_TAG,jsonImportResult);
        editor.apply();
    }

    private void LoadNestedPlaylistsData() {
        //Imports playlist data must be fetched first.
        Gson gson = new Gson();
        String json = this.sharedPreferences.getString(SAVED_PREFERENCES_NESTED_TAG,"");
        Type nestedPlaylistsType = new TypeToken<HashMap<String, PlaylistInfo>>(){}.getType();

        HashMap<String, PlaylistInfo> nestedPlaylists = gson.fromJson(json,nestedPlaylistsType);
        if(nestedPlaylists == null) {
            nestedPlaylists = new HashMap<>();
        }

        //Populate the nested playlist with the imported values.
        for(PlaylistInfo value : nestedPlaylists.values()) {
            for(String key : value.GetImportedPlaylistKeys()) {
                if(this.importedPlaylistData.containsKey(key)) {
                    value.ImportPlaylist(key, this.importedPlaylistData.get(key));
                }
            }
        }

        this.nestedPlaylistData = nestedPlaylists;
        this.dataLastUpdated = UUID.randomUUID().toString();
    }

    private void LoadImportedPlaylistsData() {
        //Gets imported playlist through gson and shared preferences.
        Gson gson = new Gson();
        String json = this.sharedPreferences.getString(SAVED_PREFERENCES_IMPORT_TAG,"");
        Type importPlaylistsType = new TypeToken<HashMap<String, PlaylistInfo>>(){}.getType();

        HashMap<String, PlaylistInfo> imports = gson.fromJson(json, importPlaylistsType);
        if(imports == null) {
            imports = new HashMap<>();
        }

        this.importedPlaylistData = imports;
        this.dataLastUpdated = UUID.randomUUID().toString();
    }

    public ArrayList<Pair<String, PlaylistInfo>> GetNestedPlaylists() {
        if(this.nestedPlaylistData == null) {
            LoadNestedPlaylistsData();
        }
        return PlaylistMapToArraylist(this.nestedPlaylistData);
    }

    public ArrayList<Pair<String, PlaylistInfo>> GetImportedPlaylists() {
        if(this.importedPlaylistData == null) {
            LoadImportedPlaylistsData();
        }
        return PlaylistMapToArraylist(this.importedPlaylistData);
    }

    public void RemovePlaylist(String key) {
        this.nestedPlaylistData.remove(key);
        this.importedPlaylistData.remove(key);
        this.dataLastUpdated = UUID.randomUUID().toString();
    }

    public PlaylistInfo GetPlaylistFromKey(String key) {
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            return playlist;
        }
        playlist = this.importedPlaylistData.get(key);
        return playlist;
    }

    public void RenamePlaylist(String key, String newName) {
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            playlist.setTitle(newName);
            this.dataLastUpdated = UUID.randomUUID().toString();
            return;
        }
        playlist = this.importedPlaylistData.get(key);
        if(playlist != null) {
            playlist.setTitle(newName);
            this.dataLastUpdated = UUID.randomUUID().toString();
        }
    }

    public void AddSongToPlaylist(String key, PlaybackAudioInfo audio) {
        PlaylistInfo playlist = this.nestedPlaylistData.get(key);
        if(playlist != null) {
            playlist.AddVideoToPlaylist(audio);
            this.dataLastUpdated = UUID.randomUUID().toString();
            return;
        }
        playlist = this.importedPlaylistData.get(key);
        if(playlist != null) {
            playlist.AddVideoToPlaylist(audio);
            this.dataLastUpdated = UUID.randomUUID().toString();
        }
    }

    public void CreateNewPlaylist(PlaylistInfo playlist, boolean isNested) {
        String key = UUID.randomUUID().toString();
        if(isNested) {
            this.nestedPlaylistData.put(key, playlist);
        }
        else {
            this.importedPlaylistData.put(key, playlist);
        }
        this.dataLastUpdated = UUID.randomUUID().toString();
    }

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
            //Preforms a download operation.
            YoutubeDLRequest request = null;
            String responseString = null;

            int downloadAttemptNumber = 1;
            boolean success = false;
            Log.d(LOG_TAG,"Downloading Song to Directory.");
            Log.d(LOG_TAG,this.appMusicDirectory.getAbsolutePath());
            while(downloadAttemptNumber <= MAX_AUDIO_DOWNLOAD_RETRIES && !success) {
                request = new YoutubeDLRequest(url);
                request.addOption("-x");
                request.addOption("--no-playlist");
                request.addOption("--retries",10);
                request.addOption("--no-check-certificate");
                request.addOption("--no-mtime");
                request.addOption("--audio-format", "opus");
                request.addOption("-o", this.appMusicDirectory.getAbsolutePath() + "/%(title)s.%(ext)s");
                try {
                    YoutubeDLResponse response = YoutubeDL.getInstance().execute(request, (float progress, long etaSeconds, String line) -> {
                        downloadFromUrlListener.onProgressUpdate(progress,etaSeconds);
                    });
                    responseString = response.getOut();

                    if(responseString.contains("has already been downloaded")) {
                        downloadFromUrlListener.onError(0,"File already exists.");
                        return;
                    }

                    success = true;
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
                    downloadFromUrlListener.onError(downloadAttemptNumber,"Failed to Download from the given URL.");

                    //We will retry download
                    downloadAttemptNumber++;
                }
            }
            if(!success) {
                downloadFromUrlListener.onError(-1,"Download Attempts exceeded threshold.");
                return;
            }

            //Fetches audio information from the source.
            PlaybackAudioInfo audio = new PlaybackAudioInfo();
            int videoInfoFetchAttemptNumber = 1;
            success = false;
            while(videoInfoFetchAttemptNumber <= MAX_FETCH_AUDIO_INFO_RETRIES && !success) {
                try {
                    VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(request);
                    audio.setTitle(streamInfo.getTitle().trim());
                    audio.setAuthor(streamInfo.getUploader().trim());
                    audio.setThumbnailSource(streamInfo.getThumbnail());
                    audio.setThumbnailType(PlaybackAudioInfo.PlaybackMediaType.STREAM);

                    success = true;
                } catch (YoutubeDLException | InterruptedException e) {
                    Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
                    e.printStackTrace();
                    downloadFromUrlListener.onError(videoInfoFetchAttemptNumber,"Failed to Fetch Video Information.");

                    //We will retry Fetch
                    videoInfoFetchAttemptNumber++;
                }
            }
            //Sets fallback audio parameters.
            if(!success) {
                audio = new PlaybackAudioInfo();
                int startIndex = responseString.indexOf("[ExtractAudio]");
                int endIndex = responseString.indexOf(".opus");
                if(startIndex != -1 && endIndex != -1) {
                    String sourcePath = responseString.substring(startIndex + 28, endIndex + 5);
                    audio.setTitle(sourcePath.substring(this.appMusicDirectory.getAbsolutePath().length()+1,sourcePath.length()-5).trim());
                }
                else {
                    downloadFromUrlListener.onError(-1,"Could not find reference to Downloaded File.");
                    return;
                }
            }
            audio.setAudioSource(this.appMusicDirectory.getAbsolutePath() + '/' + audio.getTitle() + ".opus");
            audio.setAudioType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);
            audio.setOrigin(url);

            //Updates the thumbnail source and type.
            if(audio.getThumbnailSource() != null) {
                File newThumbnail = DownloadToLocalStorage(audio.getThumbnailSource(), this.appImageDirectory, audio.getTitle());
                if(newThumbnail != null) {
                    audio.setThumbnailSource(newThumbnail.getAbsolutePath());
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
}
