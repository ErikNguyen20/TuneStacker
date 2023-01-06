package com.example.cloudplaylistmanager.Utils;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.ANResponse;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.example.cloudplaylistmanager.Platforms.YoutubeUtilities;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.StorageReference;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;
import com.yausername.youtubedl_android.mapper.VideoInfo;


import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;


public class DataManager {
    private static final String LOG_TAG = "DataManager";
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


    private DataManager(Context context) {
        this.YtUtilities = new YoutubeUtilities(context);
        AndroidNetworking.initialize(context);
        try {
            YoutubeDL.getInstance().init(context);
            FFmpeg.getInstance().init(context);
            this.appMusicDirectory = GetLocalMusicDirectory(context);
            this.appImageDirectory = GetLocalImageDirectory(context);
            this.appCacheDirectory = GetLocalCacheDirectory(context);
            this.downloaderInitialized = true;
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
     * Uploads song with the given audio information
     * It is required to implement {@link UploadToCloudListener} to obtain the
     * result of this call and to catch potential errors.
     * @param audio Audio information that will be uploaded.
     * @param uploadToCloudListener Listener used to get the results/errors of this call.
     */
    public void UploadAudioToCloud(PlaybackAudioInfo audio, UploadToCloudListener uploadToCloudListener) {
        Thread thread = new Thread(() -> {
            FirebaseManager firebase = FirebaseManager.getInstance();
            String uniqueID = UUID.randomUUID().toString();
            //Checks to see if the song already exists as metadata in the database.
            Pair<DocumentReference,Boolean> existPath = firebase.FindExistingSongInDatabase(audio.getTitle(),audio.getOrigin());
            if(existPath != null && existPath.second) {
                uploadToCloudListener.onError("Audio already exists on the cloud.");
                return;
            }

            //Fetches/Downloads and uploads the thumbnail.
            File thumbnailFile = null;
            switch(audio.getThumbnailType()) {
                case STREAM:
                    thumbnailFile = DownloadToLocalStorage(audio.getThumbnailSource(), this.appImageDirectory, audio.getTitle());
                    break;
                case LOCAL:
                    thumbnailFile = new File(audio.getThumbnailSource());
                    if(!thumbnailFile.exists())
                    {
                        thumbnailFile = null;
                    }
            }
            //Uploads the thumbnail to the cloud.
            String thumbnailUrl;
            if(thumbnailFile != null) {
                final CountDownLatch latch = new CountDownLatch(1);
                StringBuilder thumbnailUrlBuilder = new StringBuilder();
                StorageReference reference = firebase.GetStorageReferenceToThumbnails().child(audio.getTitle() + '_' + uniqueID);
                firebase.UploadToFirebase(thumbnailFile, reference, new UploadListener() {
                    @Override
                    public void onComplete(String downloadUrl) {
                        thumbnailUrlBuilder.append(downloadUrl);
                        latch.countDown();
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "Latch was interrupted.");
                    e.printStackTrace();
                }
                if (!thumbnailUrlBuilder.toString().isEmpty()) {
                    thumbnailUrl = thumbnailUrlBuilder.toString();
                }
                else {
                    thumbnailUrl = FirebaseManager.DEFAULT_THUMBNAIL_SOURCE;
                }
                audio.setThumbnailSource(thumbnailUrl);
            }
            else {
                audio.setThumbnailSource(FirebaseManager.DEFAULT_THUMBNAIL_SOURCE);
            }

            //Fetches/Downloads and uploads the audio.
            File audioFile = null;
            switch(audio.getAudioType()) {
                case STREAM:
                    audioFile = DownloadToLocalStorage(audio.getAudioSource(), this.appImageDirectory, audio.getTitle()+ '_' + uniqueID);
                    break;
                case LOCAL:
                    audioFile = new File(audio.getAudioSource());
                    if(!audioFile.exists())
                    {
                        audioFile = null;
                    }
                    break;
                default:
                    uploadToCloudListener.onError("Audio Source is Invalid.");
                    return;
            }
            //Uploads the audio to the cloud.
            if(audioFile != null) {
                //Uploads to Firebase cloud storage. Can specify another location if wanted.
                final CountDownLatch latch = new CountDownLatch(1);
                StringBuilder audioUrlBuilder = new StringBuilder();
                StorageReference reference = firebase.GetStorageReferenceToAudio().child(audio.getTitle() + '_' + UUID.randomUUID().toString());
                firebase.UploadToFirebase(audioFile, reference, new UploadListener() {
                    @Override
                    public void onComplete(String downloadUrl) {
                        audioUrlBuilder.append(downloadUrl);
                        latch.countDown();
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        uploadToCloudListener.onError(message);
                        Log.e(LOG_TAG, message);
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "Latch was interrupted.");
                    e.printStackTrace();
                }
                if (!audioUrlBuilder.toString().isEmpty()) {
                    audio.setAudioSource(audioUrlBuilder.toString());
                }
                else {
                    audio.setAudioSource(null);
                }
            }
            else {
                uploadToCloudListener.onError("Audio File not Found / Failed to download.");
                Log.e(LOG_TAG, "Audio File not Found / Failed to download.");
            }

            //Updates or Adds the new song's metadata.
            if(existPath == null) {
                //External parameter is null because the cloud source is Firebase. We will specify
                //a parameter if it is stored in some external cloud storage.
                DocumentReference songMetadata = firebase.CreateNewSongMetadata(audio,null);
                uploadToCloudListener.onComplete(songMetadata.getPath());
                Log.d(LOG_TAG, "Created new Song Metadata.");
            }
            else {
                //Metadata should also be updated to include the "external" field if the source is on
                //on external cloud storage.
                firebase.UpdateExistingSongAudioUrl(existPath.first.getPath(),audio.getAudioSource());
                firebase.UpdateExistingSongThumbnailUrl(existPath.first.getPath(),audio.getThumbnailSource());
                uploadToCloudListener.onComplete(existPath.first.getPath());
                Log.d(LOG_TAG, "Updated Song Metadata.");
            }
        });

        thread.start();
    }

    /**
     * Downloads song with the given, valid URL into the MUSIC directory.
     * It is required to implement {@link DownloadFromUrlListener} to obtain the
     * result of this call and to catch potential errors.
     * onError returns -1 in the attempt parameter if a critical failure occurs.
     * onError returns 0 if the file already exists.
     * @param url Url of the Audio source.
     * @param downloadFromUrlListener Listener used to get the results/errors of this call.
     */
    public void DownloadSongToDirectoryFromUrl(String url, DownloadFromUrlListener downloadFromUrlListener) {
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
                    }

                    success = true;
                } catch (YoutubeDLException | InterruptedException e) {
                    Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
                    e.printStackTrace();
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

            downloadFromUrlListener.onComplete(audio);
        });

        thread.start();
    }

    /**
     * Adds a playlist on the database. The playlist will not be
     * uploaded and just exists as a placeholder.
     * @param playlistInfo Link to json on web.
     * @param platform Platform that the playlist was imported from (Youtube, Spotify, etc).
     */
    public void ImportPlaylist(PlaylistInfo playlistInfo, String platform) {
        Thread thread = new Thread(() -> {
            FirebaseManager firebase = FirebaseManager.getInstance();
            if(firebase.FindExistingPlaylistInDatabase(playlistInfo.getLinkSource()) == null) {
                return;
            }

            DocumentReference playlistReference = firebase.CreateNewPlaylist(playlistInfo.getTitle(),false,platform,playlistInfo.getLinkSource());
            ArrayList<DocumentReference> metadataList = new ArrayList<>();
            for(PlaybackAudioInfo audioInfo : playlistInfo.getAllVideos()) {
                Pair<DocumentReference,Boolean> existPath = firebase.FindExistingSongInDatabase(audioInfo.getTitle(),audioInfo.getOrigin());
                if(existPath == null) {
                    DocumentReference metadataReference = firebase.CreateNewSongMetadata(audioInfo,null);
                    metadataList.add(metadataReference);
                }
                else {
                    metadataList.add(existPath.first);
                }
            }

            firebase.SetSongsInPlaylist(playlistReference,metadataList);
        });

        thread.start();
    }

    /**
     * Adds a song to the playlist on the database. The song will not be
     * uploaded and just exists as a placeholder.
     * @param audioInfo Link to json on web.
     * @param playlistPath Working directory.
     */
    public void AddSongToPlaylist(PlaybackAudioInfo audioInfo, String playlistPath) {
        Thread thread = new Thread(() -> {
            FirebaseManager firebase = FirebaseManager.getInstance();
            Pair<DocumentReference, Boolean> existPath = firebase.FindExistingSongInDatabase(audioInfo.getTitle(), audioInfo.getOrigin());
            if (existPath != null) {
                firebase.AddAudioToPlaylist(playlistPath, existPath.first.getPath());
            } else {
                audioInfo.setAudioSource(null);
                audioInfo.setThumbnailSource(null);
                DocumentReference newMetadata = firebase.CreateNewSongMetadata(audioInfo, null);
                firebase.AddAudioToPlaylist(playlistPath, newMetadata.getPath());
            }
        });

        thread.start();
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
                .build().startDownload(new DownloadListener() {
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

    public void SyncPlaylist(String url) {
        this.YtUtilities.FetchPlaylistItems(url, new FetchPlaylistListener() {
            @Override
            public void onComplete(PlaylistInfo fetchedPlaylist) {
                Log.d("DataManager","Playlist Length: " + fetchedPlaylist.getAllVideos().size());
            }

            @Override
            public void onError(String message) {
                Log.e("DataManager",message);
            }
        });
    }


    public PlaylistInfo ConstructPlaylistFromLocalFiles() {
        File[] files = this.appMusicDirectory.listFiles();
        if(files == null) {
            return null;
        }
        PlaylistInfo playlistInfo = new PlaylistInfo();
        playlistInfo.setTitle("From Saved Songs");

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
     * @return Map
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
    public File GetLocalMusicDirectory(Context context) {
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
    public File GetLocalImageDirectory(Context context) {
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
    public File GetLocalCacheDirectory(Context context) {
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
     * Gets the default image when a thumbnail isn't valid.
     * @return File object.
     */
    public File GetDefaultImage() {
        File newThumbnail = GetFileFromDirectory(this.appImageDirectory, DataManager.DEFAULT_THUMBNAIL);
        if(newThumbnail == null) {
            newThumbnail = DownloadToLocalStorage(FirebaseManager.DEFAULT_THUMBNAIL_SOURCE, this.appImageDirectory, DataManager.DEFAULT_THUMBNAIL);
        }
        return newThumbnail;
    }
}
