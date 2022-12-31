package com.example.cloudplaylistmanager.Utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface FetchPlaylistListener{
    void onComplete(YoutubeUtilities.YtPlaylistInfo fetchedPlaylist);
    void onError(String message);
}

public class YoutubeUtilities {
    private static final String API_KEY = "AIzaSyDRtock8Du8PQSV4h0oVYMJHvwsI233TJg"; //Comes from "https://console.cloud.google.com/"

    private static final String VID_EXTRACT_PATTERN = "/^.*((youtu.be\\/)|(v\\/)|(\\/u\\/\\w\\/)|(embed\\/)|(watch\\?))\\??v?=?([^#\\&\\?]*).*/";
    private static final String PLAYLIST_EXTRACT_PATTERN = "list=([a-zA-Z0-9-_]+)&?";

    public FetchPlaylistListener playlistListener;


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

            HashMap<String,String> params = new HashMap<>();
            params.put("part", "snippet");
            params.put("maxResults", "50");
            params.put("playlistId", extractedPlaylistID);
            params.put("key",API_KEY);

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

            int maxSongCount = playlistInfo.getTotalResults();
            int numSongs = 50;
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
                numSongs += 50;
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
        if(match.find() && match.group(7).length() == 11) {
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
                video.title = item.getString("title");
                video.description = item.getString("description");
                if(item.getJSONObject("thumbnails").has("default")) {
                    video.thumbnail = item.getJSONObject("thumbnails").getJSONObject("default").getString("url");
                } else {
                    video.isPrivate = true;
                }
                if(item.has("videoOwnerChannelTitle")) {
                    video.creator = item.getString("videoOwnerChannelTitle");
                }
                video.videoId = item.getJSONObject("resourceId").getString("videoId");

                playlistResult.AddVideoToPlaylist(video);
            }
            return playlistResult;
        } catch(Exception e) {
            Log.e(this.getClass().getName(),(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
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
