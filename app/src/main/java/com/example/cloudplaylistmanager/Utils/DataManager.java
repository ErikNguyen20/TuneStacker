package com.example.cloudplaylistmanager.Utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.ANResponse;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.androidnetworking.interfaces.StringRequestListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;
import com.yausername.youtubedl_android.mapper.VideoInfo;


import org.json.JSONObject;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

interface DownloadFromUrlListener{
    void onComplete(PlaybackAudioInfo audio);
    void onProgressUpdate(float progress, long etaSeconds);
    void onError(int attempt, String error);
}
interface UploadSongToFirebaseListener{
    void onComplete();
    void onError();
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


    private DataManager(Context context) {
        this.YtUtilities = new YoutubeUtilities(context);
        AndroidNetworking.initialize(context);
        try {
            YoutubeDL.getInstance().init(context);
            FFmpeg.getInstance().init(context);
            this.appMusicDirectory = GetLocalMusicDirectory(context);
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

    public void UploadAudioToFirebase(PlaybackAudioInfo audio, StorageReference fileReference) {
        //FirebaseStorage.getInstance().getReference().child("audio").child("TBUploaded File name here");
        if(audio.getAudioType() == PlaybackAudioInfo.PlaybackMediaType.LOCAL) {
            //Download it, then: audio.setAudioSource(filepath);
        }
        Uri audioUri = Uri.parse(audio.getAudioSource());
        Uri imgUri = Uri.parse(audio.getThumbnailSource());

        if(audio.getThumbnailType() == PlaybackAudioInfo.PlaybackMediaType.STREAM) {
            //Simply store it in the metadata.
        }
        else {

        }

        //String filename = uri.getLastPathSegment();
        //fileReference.putFile(uri);
    }

    /**
     * Downloads song with the given, valid URL into the MUSIC directory.
     * It is required to implement {@link DownloadFromUrlListener} to obtain the
     * result of this call and to catch potential errors.
     * onError returns -1 in the attempt parameter if a critical failure occurs.
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
                    if(responseString.contains("ERROR:") && response.getErr() != null && !response.getErr().isEmpty()) {
                        throw new YoutubeDLException(responseString);
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

                    if(streamInfo.getTitle().contains("ERROR:")) {
                        throw new YoutubeDLException(streamInfo.getTitle());
                    }
                    success = true;
                } catch (YoutubeDLException | InterruptedException e) {
                    Log.e("Test",e.getMessage());
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
                if(startIndex == -1 || endIndex == -1) {
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
     * Downloads file with given url to local storage.
     * @param link Link to json on web.
     * @param directory Working directory.
     * @param filename Name of the File + Extension ex: "accounts.json"
     */
    public void DownloadToLocalStorageAsync(String link, String directory, String filename) {
        AndroidNetworking.download(link, directory, filename)
                .setPriority(Priority.MEDIUM)
                .build().startDownload(new DownloadListener() {
            @Override
            public void onDownloadComplete() {
                Log.d(LOG_TAG, "Download Complete");
            }
            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() != 0) {
                    Log.e(LOG_TAG, "DownloadToLocalStorageAsync errorCode : " + anError.getErrorCode());
                    Log.e(LOG_TAG, "DownloadToLocalStorageAsync errorBody : " + anError.getErrorBody());
                    Log.e(LOG_TAG, "DownloadToLocalStorageAsync errorDetail : " + anError.getErrorDetail());
                }
                else {
                    Log.e(LOG_TAG, "DownloadToLocalStorageAsync errorDetail : " + anError.getErrorDetail());
                }
            }
        });
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

    public void testSearch() {
        AndroidNetworking.get("https://www.youtube.com/results?search_query=orange+mint").build().getAsString(new StringRequestListener() {
            @Override
            public void onResponse(String response) {
                Log.e("Test",response);
            }

            @Override
            public void onError(ANError anError) {
                if (anError.getErrorCode() != 0) {
                    Log.e(LOG_TAG, "MakeGetRequest errorCode : " + anError.getErrorCode());
                    Log.e(LOG_TAG, "MakeGetRequest errorBody : " + anError.getErrorBody());
                    Log.e(LOG_TAG, "MakeGetRequest errorDetail : " + anError.getErrorDetail());
                }
                else {
                    Log.e(LOG_TAG, "MakeGetRequest errorDetail : " + anError.getErrorDetail());
                }
                anError.printStackTrace();
            }
        });
    }

    public void SyncPlaylist(String url) {
        this.YtUtilities.FetchPlaylistItems(url, new FetchPlaylistListener() {
            @Override
            public void onComplete(YoutubeUtilities.YtPlaylistInfo fetchedPlaylist) {
                Log.d("DataManager","Playlist Length: " + fetchedPlaylist.getVideos().size());
            }

            @Override
            public void onError(String message) {
                Log.e("DataManager",message);
            }
        });
    }


    /**
     * Converts an audio title into a hash. Uses SHA-256
     * @param title String title of the audio.
     * @return Hashed string.
     */
    public static String AudioTitleToHash(String title) {
        try {
            String chunk = title.split("[.]")[0];
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(chunk.getBytes(StandardCharsets.UTF_8));
            BigInteger bi = new BigInteger(1, hash);
            return String.format("%0" + (hash.length << 1) + "x", bi);
        } catch (NoSuchAlgorithmException e) {
            Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
            return null;
        }
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
}
