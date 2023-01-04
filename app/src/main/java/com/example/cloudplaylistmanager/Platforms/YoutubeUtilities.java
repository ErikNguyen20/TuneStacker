package com.example.cloudplaylistmanager.Platforms;

import android.content.Context;
import android.util.Log;


import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.FetchPlaylistListener;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class YoutubeUtilities {
    private static final String LOG_TAG = "YoutubeUtilities";
    private static final String API_KEY = "AIzaSyDRtock8Du8PQSV4h0oVYMJHvwsI233TJg"; //Comes from "https://console.cloud.google.com/"
    private static final int MAX_RESULTS = 50;

    private static final String VID_EXTRACT_PATTERN = "/^.*((youtu.be\\/)|(v\\/)|(\\/u\\/\\w\\/)|(embed\\/)|(watch\\?))\\??v?=?([^#\\&\\?]*).*/";
    private static final String PLAYLIST_EXTRACT_PATTERN = "list=([a-zA-Z0-9-_]+)&?";
    private static final String BASE_VIDEO_URL = "https://www.youtube.com/watch?v=";

    private Context context;


    public YoutubeUtilities(Context context) {
        this.context = context;
    }

    /**
     * Fetches all videos in a youtube playlist with a given playlist url.
     * It is required to implement {@link FetchPlaylistListener} to obtain the
     * result of this call and to catch potential errors.
     * @param url Url of the Youtube Playlist
     * @param playlistListener Listener used to get the results/errors of this call.
     */
    public void FetchPlaylistItems(String url, FetchPlaylistListener playlistListener) {
        Thread thread = new Thread(() -> {
            String extractedPlaylistID = ExtractPlaylistIdFromUrl(url);
            if(extractedPlaylistID == null) {
                playlistListener.onError("Invalid Playlist URL.");
            }


            //Initializes Query Parameters for the HTTP Get Request.
            HashMap<String,String> params = new HashMap<>();
            params.put("part", "snippet");
            params.put("maxResults", String.valueOf(MAX_RESULTS));
            params.put("playlistId", extractedPlaylistID);
            params.put("key",API_KEY);

            //Makes the Get Request and parses results into a YtPlaylistInfo object.
            JSONObject result = DataManager.MakeGetRequest("https://www.googleapis.com/youtube/v3/playlistItems",params);
            if(result == null) {
                playlistListener.onError("Initial Get Request from the API failed.");
                return;
            }
            YoutubePlaylistInfo playlistInfo = ParsePlaylistJsonResult(result);
            if(playlistInfo == null) {
                playlistListener.onError("An Error occurred when parsing the Initial Get Request");
                return;
            }

            //If the playlist is larger than the max results, fetch the next list using the next page token.
            int maxSongCount = playlistInfo.GetTotalResults();
            int numSongs = MAX_RESULTS;
            while (numSongs < maxSongCount) {
                params.put("pageToken",playlistInfo.GetNextPageToken());
                result = DataManager.MakeGetRequest("https://www.googleapis.com/youtube/v3/playlistItems",params);
                if(result == null) {
                    playlistListener.onError("Subsequent Get Requests from the API failed.");
                    return;
                }
                YoutubePlaylistInfo nextPlaylistInfo = ParsePlaylistJsonResult(result);
                if(nextPlaylistInfo == null) {
                    playlistListener.onError("An Error occurred when parsing the Subsequent Get Requests");
                    return;
                }
                playlistInfo.MergePlaylists(nextPlaylistInfo);
                numSongs += MAX_RESULTS;
            }

            YoutubePlaylistInfo playlistMetadata = GetPlaylistInfo(extractedPlaylistID);
            if(playlistMetadata != null) {
                playlistInfo.setTitle(playlistMetadata.getTitle());
            }
            else {
                playlistInfo.setTitle(extractedPlaylistID);
            }

            playlistInfo.setLinkSource(url);
            playlistListener.onComplete(playlistInfo);
        });

        thread.start();
    }

    /**
     * Extracts the Playlist Metadata from the Youtube Playlist ID.
     * @param id ID of the playlist on Youtube.
     */
    public YoutubePlaylistInfo GetPlaylistInfo(String id) {
        //Initializes Query Parameters for the HTTP Get Request.
        HashMap<String,String> params = new HashMap<>();
        params.put("part", "snippet");
        params.put("fields", "items(id,snippet(title,channelId,channelTitle))");
        params.put("id", id);
        params.put("key",API_KEY);

        //Makes the Get Request and parses results into a YtPlaylistInfo object.
        JSONObject result = DataManager.MakeGetRequest("https://www.googleapis.com/youtube/v3/playlists",params);
        if(result != null) {
            //Parse result
            try {
                YoutubePlaylistInfo playlistInfo = new YoutubePlaylistInfo();

                JSONArray items = result.getJSONArray("items");
                for(int index = 0; index < items.length(); index++) {
                    JSONObject item = items.getJSONObject(index).getJSONObject("snippet");
                    if(item.has("title")) {
                        playlistInfo.setTitle(item.getString("title"));
                    }
                }
                return playlistInfo;
            } catch (JSONException e) {
                Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Extracts the Video ID from the Youtube URL.
     * @param url Youtube Video URL.
     */
    public static String ExtractVideoIdFromUrl(String url) {
        Matcher match = Pattern.compile(VID_EXTRACT_PATTERN).matcher(url);
        if(match.find() && match.group(7) != null && match.group(7).length() == 11) {
            return match.group(7);
        }
        else {
            return null;
        }
    }

    /**
     * Extracts the Playlist ID from the Youtube Playlist URL.
     * @param url Youtube Playlist URL.
     */
    public static String ExtractPlaylistIdFromUrl(String url) {
        Matcher match = Pattern.compile(PLAYLIST_EXTRACT_PATTERN).matcher(url);
        if(match.find()) {
            return match.group(1);
        }
        else {
            return null;
        }
    }

    /**
     * Simply concatenates the base Youtube URL with the id.
     * @param id Youtube video ID.
     */
    public static String GetVideoWithYoutubeID(String id) {
        return BASE_VIDEO_URL + id;
    }

    /**
     * Parses the JSON result of the get request from Youtube's playlist list API.
     * @param result JSON Result of the fetch api that will be parsed.
     * @return YtPlaylistInfo object
     */
    private YoutubePlaylistInfo ParsePlaylistJsonResult(JSONObject result) {
        if(result == null) {
            return null;
        }
        try {
            YoutubePlaylistInfo playlistResult = new YoutubePlaylistInfo();
            if(result.has("nextPageToken")) {
                playlistResult.SetNextPageToken(result.getString("nextPageToken"));
            }
            playlistResult.SetTotalResults(result.getJSONObject("pageInfo").getInt("totalResults"));

            JSONArray items = result.getJSONArray("items");
            for (int index = 0; index < items.length(); index++) {
                PlaybackAudioInfo video = new PlaybackAudioInfo();

                JSONObject item = items.getJSONObject(index).getJSONObject("snippet");
                if(item.has("title")) {
                    video.setTitle(item.getString("title"));
                }
                if(item.getJSONObject("thumbnails").has("medium")) {
                    video.setThumbnailSource(item.getJSONObject("thumbnails").getJSONObject("medium").getString("url"));
                    video.setThumbnailType(PlaybackAudioInfo.PlaybackMediaType.STREAM);
                }
                if(item.has("videoOwnerChannelTitle")) {
                    video.setAuthor(item.getString("videoOwnerChannelTitle"));
                }
                else {
                    video.setIsPrivate(true);
                }

                video.setOrigin(BASE_VIDEO_URL + item.getJSONObject("resourceId").getString("videoId"));
                video.setAudioType(PlaybackAudioInfo.PlaybackMediaType.UNKNOWN);

                playlistResult.AddVideoToPlaylist(video);
            }
            return playlistResult;
        } catch(Exception e) {
            Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
            return null;
        }
    }



    public class YoutubePlaylistInfo extends PlaylistInfo {
        private String nextPageToken;
        private int totalResults;

        public YoutubePlaylistInfo() {
            super();
            this.nextPageToken = null;
        }

        public void SetNextPageToken(String nextPageToken) {
            this.nextPageToken = nextPageToken;
        }

        public void SetTotalResults(int totalResults) {
            this.totalResults = totalResults;
        }

        public String GetNextPageToken() {
            return this.nextPageToken;
        }

        public int GetTotalResults() {
            return this.totalResults;
        }

        public void MergePlaylists(YoutubePlaylistInfo other) {
            super.MergePlaylists(other);
            this.nextPageToken = other.nextPageToken;
        }
    }
}
