package com.example.cloudplaylistmanager.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

interface FetchPlaylistListener{
    void onComplete(YoutubeUtilities.YtPlaylistInfo fetchedPlaylist);
    void onError(String message);
}

interface ExtractUrlListener{
    void onComplete(String url);
    void onError(String message);
}


public class YoutubeUtilities {
    private static final String LOG_TAG = "YoutubeUtilities";
    private static final String API_KEY = "AIzaSyDRtock8Du8PQSV4h0oVYMJHvwsI233TJg"; //Comes from "https://console.cloud.google.com/"
    private static final int MAX_RESULTS = 50;

    private static final String VID_EXTRACT_PATTERN = "/^.*((youtu.be\\/)|(v\\/)|(\\/u\\/\\w\\/)|(embed\\/)|(watch\\?))\\??v?=?([^#\\&\\?]*).*/";
    private static final String PLAYLIST_EXTRACT_PATTERN = "list=([a-zA-Z0-9-_]+)&?";
    public static int VIDEO_MP4_720_ITAG = 22;
    public static int AUDIO_M4A_128k_ITAG = 140;

    private Context context;
    public FetchPlaylistListener playlistListener;
    public ExtractUrlListener extractUrlListener;


    public YoutubeUtilities(Context context) {
        this.context = context;
    }

    /**
     * Extracts the download url from a Youtube Video Url.
     * It is required to implement {@link ExtractUrlListener} to obtain the
     * result of this call and to catch potential errors.
     * @param url Url of the Youtube Video.
     */
    @SuppressLint("StaticFieldLeak")
    public void ExtractUrlFromVideoUrl(String url, int itag) {
        new YouTubeExtractor(this.context) {
            @Override
            protected void onExtractionComplete(@Nullable SparseArray<YtFile> ytFiles, @Nullable VideoMeta videoMeta) {
                if(ytFiles != null) {
                    String downloadLink = ytFiles.get(itag).getUrl();
                    extractUrlListener.onComplete(downloadLink);
                }
                else {
                    extractUrlListener.onError("Failed to Download.");
                }
            }
        }.extract(url);
    }

    /**
     * Fetches all videos in a youtube playlist with a given playlist url.
     * It is required to implement {@link FetchPlaylistListener} to obtain the
     * result of this call and to catch potential errors.
     * @param url Url of the Youtube Playlist
     */
    public void FetchPlaylistItems(String url) {
        Thread thread = new Thread(() -> {
            String extractedPlaylistID = ExtractPlaylistIdFromUrl(url);
            if(extractedPlaylistID == null) {
                this.playlistListener.onError("Invalid Playlist URL.");
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
                this.playlistListener.onError("Initial Get Request from the API failed.");
                return;
            }
            YtPlaylistInfo playlistInfo = ParsePlaylistJsonResult(result);
            if(playlistInfo == null) {
                this.playlistListener.onError("An Error occurred when parsing the Initial Get Request");
                return;
            }

            //If the playlist is larger than the max results, fetch the next list using the next page token.
            int maxSongCount = playlistInfo.getTotalResults();
            int numSongs = MAX_RESULTS;
            while (numSongs < maxSongCount) {
                params.put("pageToken",playlistInfo.getNextPageToken());
                result = DataManager.MakeGetRequest("https://www.googleapis.com/youtube/v3/playlistItems",params);
                if(result == null) {
                    this.playlistListener.onError("Subsequent Get Requests from the API failed.");
                    return;
                }
                YtPlaylistInfo nextPlaylistInfo = ParsePlaylistJsonResult(result);
                if(nextPlaylistInfo == null) {
                    this.playlistListener.onError("An Error occurred when parsing the Subsequent Get Requests");
                    return;
                }
                playlistInfo.MergePlaylists(nextPlaylistInfo);
                numSongs += MAX_RESULTS;
            }

            this.playlistListener.onComplete(playlistInfo);
        });

        thread.start();
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
     * Parses the JSON result of the get request from Youtube's playlist list API.
     * @param result JSON Result of the fetch api that will be parsed.
     * @return YtPlaylistInfo object
     */
    private YtPlaylistInfo ParsePlaylistJsonResult(JSONObject result) {
        if(result == null) {
            return null;
        }
        try {
            YtPlaylistInfo playlistResult = new YtPlaylistInfo();
            if(result.has("nextPageToken")) {
                playlistResult.SetNextPageToken(result.getString("nextPageToken"));
            }
            playlistResult.SetTotalResults(result.getJSONObject("pageInfo").getInt("totalResults"));

            JSONArray items = result.getJSONArray("items");
            for (int index = 0; index < items.length(); index++) {
                YtVideoInfo video = new YtVideoInfo();

                JSONObject item = items.getJSONObject(index).getJSONObject("snippet");
                if(item.has("title")) {
                    video.title = item.getString("title");
                }
                if(item.has("description")) {
                    video.description = item.getString("description");
                }
                if(item.getJSONObject("thumbnails").has("default")) {
                    video.thumbnail = item.getJSONObject("thumbnails").getJSONObject("default").getString("url");
                }
                if(item.has("videoOwnerChannelTitle")) {
                    video.creator = item.getString("videoOwnerChannelTitle");
                }
                else {
                    video.isPrivate = true;
                }
                video.videoId = item.getJSONObject("resourceId").getString("videoId");

                playlistResult.AddVideoToPlaylist(video);
            }
            return playlistResult;
        } catch(Exception e) {
            Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
            return null;
        }
    }



    public class YtPlaylistInfo {
        private String nextPageToken;
        private int totalResults;
        private ArrayList<YtVideoInfo> videos;

        public YtPlaylistInfo() {
            this.videos = new ArrayList<>();
            this.nextPageToken = null;
        }

        public void SetNextPageToken(String nextPageToken) {
            this.nextPageToken = nextPageToken;
        }

        public void SetTotalResults(int totalResults) {
            this.totalResults = totalResults;
        }

        public String getNextPageToken() {
            return this.nextPageToken;
        }

        public int getTotalResults() {
            return this.totalResults;
        }

        public ArrayList<YtVideoInfo> getVideos() {
            return this.videos;
        }

        public void AddVideoToPlaylist(YtVideoInfo video) {
            this.videos.add(video);
        }

        public void MergePlaylists(YtPlaylistInfo other) {
            this.videos.addAll(other.videos);
            this.nextPageToken = other.nextPageToken;
        }
    }


    public class YtVideoInfo {
        public String title = "Unknown";
        public String creator = "Unknown";
        public String description = "";
        public String thumbnail = null;
        public String videoId = null;
        public boolean isPrivate = false;
    }

}
