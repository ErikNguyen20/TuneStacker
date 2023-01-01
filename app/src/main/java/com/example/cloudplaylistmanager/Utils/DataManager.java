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

import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;


public class DataManager {
    private static final String LOG_TAG = "DataManager";

    private static DataManager instance = null;
    private Context context;
    private YoutubeUtilities YtUtilities;

    private DataManager(Context context) {
        this.context = context;
        this.YtUtilities = new YoutubeUtilities(context);
        AndroidNetworking.initialize(context);
    }
    public static void Initialize(Context context) {
        if(instance == null) {
            instance = new DataManager(context);
        }
    }
    public static DataManager getInstance() {
        return instance;
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
                Log.d("LocalStorage", "Download Complete");
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

    public void getaudiourl(String url) {
        this.YtUtilities.extractUrlListener = new ExtractUrlListener() {
            @Override
            public void onComplete(String url) {
                Log.d("AudioResult",url);
            }

            @Override
            public void onError(String message) {
                Log.e("AudioResult",message);
            }
        };
        this.YtUtilities.ExtractUrlFromVideoUrl(url,YoutubeUtilities.AUDIO_M4A_128k_ITAG);
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
            Log.e(this.getClass().getName(),(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
            return null;
        }
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
