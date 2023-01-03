package com.example.cloudplaylistmanager.Utils;

import android.content.Context;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.ANResponse;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.example.cloudplaylistmanager.Platforms.YoutubeUtilities;
import com.google.firebase.storage.StorageReference;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;
import com.yausername.youtubedl_android.mapper.VideoInfo;


import org.json.JSONObject;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

interface DownloadFromUrlListener{
    void onComplete(PlaybackAudioInfo audio);
    void onProgressUpdate(float progress, long etaSeconds);
    void onError(int attempt, String error);
}

public class DataManager {
    private static final String LOG_TAG = "DataManager";
    private static final String LOCAL_DIRECTORY_AUDIO_STORAGE = "downloaded-songs";
    private static final String LOCAL_DIRECTORY_IMG_STORAGE = "thumbnails";
    private static final String LOCAL_DIRECTORY_CACHE = "cache";
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

    //TODO - Make sure to finish this function (Add listener)
    public void UploadAudioToCloud(PlaybackAudioInfo audio) {
        Thread thread = new Thread(() -> {
            FirebaseManager firebase = FirebaseManager.getInstance();

            boolean exists = firebase.CheckIfSongExistsInDatabase(audio.getTitle(),audio.getOrigin());
            if(exists) {
                return;
            }

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

            String thumbnailUrl;
            if(thumbnailFile != null) {
                final CountDownLatch latch = new CountDownLatch(1);
                StringBuilder thumbnailUrlBuilder = new StringBuilder();
                //Get File size (bytes) thumbnail.length(); UUID.randomUUID().toString()
                StorageReference reference = firebase.GetStorageReferenceToThumbnails().child(audio.getTitle());
                firebase.UploadToFirebase(thumbnailFile, reference, new UploadListener() {
                    @Override
                    public void onComplete(String downloadUrl) {
                        thumbnailUrlBuilder.append(downloadUrl);
                        latch.countDown();
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        Log.e("TEST_ERROR", message);
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "Latch was interrupted.");
                    e.printStackTrace();
                }
                if (thumbnailUrlBuilder.toString().isEmpty()) {
                    thumbnailUrl = FirebaseManager.DEFAULT_THUMBNAIL_MED;
                }
            }


            File audioFile = null;
            switch(audio.getAudioType()) {
                case STREAM:
                    audioFile = DownloadToLocalStorage(audio.getAudioSource(), this.appImageDirectory, audio.getTitle());
                    break;
                case LOCAL:
                    audioFile = new File(audio.getAudioSource());
                    if(!audioFile.exists())
                    {
                        audioFile = null;
                    }
            }

            if(audioFile != null) {
                final CountDownLatch latch = new CountDownLatch(1);
                StringBuilder audioUrlBuilder = new StringBuilder();
                //Get File size (bytes) thumbnail.length(); UUID.randomUUID().toString()
                StorageReference reference = firebase.GetStorageReferenceToAudio().child(audio.getTitle());
                firebase.UploadToFirebase(thumbnailFile, reference, new UploadListener() {
                    @Override
                    public void onComplete(String downloadUrl) {
                        audioUrlBuilder.append(downloadUrl);
                        latch.countDown();
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        Log.e("TEST_ERROR", message);
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "Latch was interrupted.");
                    e.printStackTrace();
                }
                if (audioUrlBuilder.toString().isEmpty()) {
                    return;
                }
            }
            else {
                return;
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
                    audio.setTitle(streamInfo.getTitle());
                    audio.setAuthor(streamInfo.getUploader());
                    audio.setThumbnailSource(streamInfo.getThumbnail());
                    audio.setThumbnailType(PlaybackAudioInfo.PlaybackMediaType.STREAM);

                    success = true;
                } catch (YoutubeDLException | InterruptedException e) {
                    Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
                    e.printStackTrace();
                    downloadFromUrlListener.onError(videoInfoFetchAttemptNumber,"Failed to Fetch Video Information.");
                    audio.setTitle(null);

                    //We will retry Fetch
                    videoInfoFetchAttemptNumber++;
                }
            }
            //Sets default audio parameters.
            if(!success) {
                audio = new PlaybackAudioInfo();
                int startIndex = responseString.indexOf("[ExtractAudio]");
                int endIndex = responseString.indexOf(".opus");
                if(startIndex != -1 && endIndex != -1) {
                    String sourcePath = responseString.substring(startIndex + 28, endIndex + 5);
                    audio.setTitle(sourcePath.substring(this.appMusicDirectory.getAbsolutePath().length(),sourcePath.length()-5));
                }
            }

            audio.setAudioSource(this.appMusicDirectory.getAbsolutePath() + '/' + audio.getTitle() + ".opus");
            audio.setAudioType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);

            downloadFromUrlListener.onComplete(audio);
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
                Log.d("DataManager","Playlist Length: " + fetchedPlaylist.getVideos().size());
            }

            @Override
            public void onError(String message) {
                Log.e("DataManager",message);
            }
        });
    }


    /**
     * Checks to see if the song already exists on the device by title.
     * @param title String title of the audio.
     * @return If it exists.
     */
    public boolean CheckIfSongExists(String title) {
        File[] files = this.appMusicDirectory.listFiles();
        if(files == null) {
            return false;
        }
        for(File file : files) {
            if(file.isFile() && file.exists()) {
                if(title.equalsIgnoreCase(file.getName().split("\\.(?=[^\\.]+$)")[0])) {
                    return true;
                }
            }
        }
        return false;
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
}
