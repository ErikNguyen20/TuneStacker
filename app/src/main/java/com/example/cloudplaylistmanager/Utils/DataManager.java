package com.example.cloudplaylistmanager.Utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.ANResponse;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.androidnetworking.interfaces.StringRequestListener;
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
    void onError(String error);
}

public class DataManager {
    private static final String LOG_TAG = "DataManager";
    private static final String PUBLIC_DIRECTORY_NAME = "downloaded-songs";

    private static DataManager instance = null;
    public DownloadFromUrlListener downloadFromUrlListener;
    private boolean downloaderInitialized = false;
    private Context context;
    private YoutubeUtilities YtUtilities;

    private DataManager(Context context) {
        this.context = context;
        this.YtUtilities = new YoutubeUtilities(context);
        AndroidNetworking.initialize(context);
        try {
            YoutubeDL.getInstance().init(context);
            FFmpeg.getInstance().init(context);
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
     * Downloads song with the given, valid URL into the MUSIC directory.
     * It is required to implement {@link DownloadFromUrlListener} to obtain the
     * result of this call and to catch potential errors.
     * @param url Url of the Audio source.
     */
    public void DownloadSongToDirectoryFromUrl(String url) {
        if(!this.downloaderInitialized) {
            this.downloadFromUrlListener.onError("Downloader failed to initialize on startup, thus it is in a failed state.");
            return;
        }
        Thread thread = new Thread(() -> {
            File appPublicMusicDirectory = GetMusicDirectory();

            //Preforms a download operation.
            YoutubeDLRequest request = new YoutubeDLRequest(url);
            request.addOption("-x");
            request.addOption("--no-playlist");
            request.addOption("--retries",10);
            request.addOption("--convert-thumbnails","jpg");
            request.addOption("--embed-thumbnail");
            request.addOption("--audio-format", "opus");
            request.addOption("--audio-quality", 5);
            request.addOption("-o", appPublicMusicDirectory.getAbsolutePath() + "/%(title)s.%(ext)s");
            String responseString = null;
            try {
                YoutubeDLResponse response = YoutubeDL.getInstance().execute(request, (float progress, long etaSeconds, String line) -> {
                    this.downloadFromUrlListener.onProgressUpdate(progress,etaSeconds);
                });
                responseString = response.getOut();
            } catch (YoutubeDLException | InterruptedException e) {
                Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
                e.printStackTrace();
                this.downloadFromUrlListener.onError("Failed to Download from the given URL.");
                //We will retry download
                return;
            }

            //Fetches audio information from the source.
            PlaybackAudioInfo audio = new PlaybackAudioInfo();
            try {
                VideoInfo streamInfo = YoutubeDL.getInstance().getInfo(request);
                audio.setTitle(streamInfo.getTitle());
                audio.setAuthor(streamInfo.getUploader());
                audio.setThumbnailSource(streamInfo.getThumbnail());
                audio.setThumbnailType(PlaybackAudioInfo.PlaybackMediaType.STREAM);
                audio.setSource(appPublicMusicDirectory.getAbsolutePath() + '/' + audio.getTitle() + ".opus");
                audio.setType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);
            } catch (YoutubeDLException | InterruptedException e) {
                Log.e("Test",e.getMessage());
                e.printStackTrace();
                this.downloadFromUrlListener.onError("Failed to Fetch Video Information.");
            }

            if(audio.getTitle() == null || audio.getTitle().isEmpty()) {
                audio = new PlaybackAudioInfo();

                String sourcePath = responseString.substring(responseString.indexOf("[ExtractAudio]")+28,responseString.indexOf(".opus")+5);
                audio.setTitle(sourcePath.substring(appPublicMusicDirectory.getAbsolutePath().length(),sourcePath.length()-5));
            }

            audio.setSource(appPublicMusicDirectory.getAbsolutePath() + '/' + audio.getTitle() + ".opus");
            audio.setType(PlaybackAudioInfo.PlaybackMediaType.LOCAL);

            this.downloadFromUrlListener.onComplete(audio);
        });

        thread.start();
    }

    /**
     * Downloads file with given url to local storage.
     * @param link Link to json on web.
     * @param directory External Storage directory.
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
        this.YtUtilities.playlistListener = new FetchPlaylistListener() {
            @Override
            public void onComplete(YoutubeUtilities.YtPlaylistInfo fetchedPlaylist) {
                Log.d("DataManager","Playlist Length: " + fetchedPlaylist.getVideos().size());
            }

            @Override
            public void onError(String message) {
                Log.e("DataManager",message);
            }
        };

        this.YtUtilities.FetchPlaylistItems(url);
    }


    /**
     * Converts an audio title into a hash. Uses SHA-256
     * @param title String title of the audio.
     */
    public String AudioTitleToHash(String title) {
        try {
            String chunk = title.split("[.]")[0];
            MessageDigest digest = MessageDigest.getInstance("SHA-256");;
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
     * Assumes that
     */
    public File GetMusicDirectory() {
        File musicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File appPublicMusicDirectory = new File(musicDirectory, PUBLIC_DIRECTORY_NAME);
        if (!appPublicMusicDirectory.exists()) {
            appPublicMusicDirectory.mkdir();
        }
        return appPublicMusicDirectory;
    }

    /**
     * Checks to see if the user has granted read/write permissions to the app.
     */
    public boolean CheckPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        else {
            int write = ContextCompat.checkSelfPermission(this.context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(this.context, Manifest.permission.READ_EXTERNAL_STORAGE);

            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        }
    }
}
