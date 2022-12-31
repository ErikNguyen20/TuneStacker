package com.example.cloudplaylistmanager.Utils;

import android.content.Context;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.ANResponse;
import com.androidnetworking.error.ANError;

import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;


public class DataManager {
    private Context context;
    private YoutubeUtilities YtUtilities;

    public DataManager(Context context) {
        this.context = context;
        this.YtUtilities = new YoutubeUtilities();
        AndroidNetworking.initialize(context);
    }

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
                Log.e("MakeGetRequest", "errorCode : " + error.getErrorCode());
                Log.e("MakeGetRequest", "errorBody : " + error.getErrorBody());
                Log.e("MakeGetRequest", "errorDetail : " + error.getErrorDetail());
            }
            else {
                Log.e("MakeGetRequest", "errorDetail : " + error.getErrorDetail());
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
}
