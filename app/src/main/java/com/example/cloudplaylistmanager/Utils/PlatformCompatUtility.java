package com.example.cloudplaylistmanager.Utils;

import android.util.Log;

import com.example.cloudplaylistmanager.Platforms.YoutubeUtilities;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class PlatformCompatUtility {
    private static final String LOG_TAG = "PlatformCompatUtility";
    public enum Platform{
        LOCAL,
        UNKNOWN,
        OTHER,
        YOUTUBE,
        SPOTIFY,
        SOUNDCLOUD
    }

    public static final String PLATFORM_YOUTUBE = "Youtube";
    public static final String PLATFORM_SPOTIFY = "Spotify";


    public static void DownloadPlaylist(String url, DownloadPlaylistListener downloadListener) {
        Thread thread = new Thread(() -> {
            PlaylistInfo fetchedPlaylist = FetchPlaylistFromUrl(url);

            if(fetchedPlaylist != null) {
                PlaylistInfo updatedPlaylist = new PlaylistInfo();
                updatedPlaylist.setTitle(fetchedPlaylist.getTitle());
                updatedPlaylist.setLinkSource(fetchedPlaylist.getLinkSource());

                ArrayList<PlaybackAudioInfo> audios = fetchedPlaylist.getInsertedVideos();

                for(int index = 0; index < audios.size(); index++) {

                    String songOrigin = audios.get(index).getOrigin();
                    final CountDownLatch latch = new CountDownLatch(1);
                    final PlaybackAudioInfo[] fetchedAudio = new PlaybackAudioInfo[1];
                    final boolean[] fetchedAudioModified = new boolean[1];

                    DataManager.getInstance().DownloadSongToDirectoryFromUrl(songOrigin, new DownloadListener() {
                        @Override
                        public void onComplete(PlaybackAudioInfo audio) {
                            fetchedAudio[0] = audio;
                            fetchedAudioModified[0] = true;
                            latch.countDown();
                        }

                        @Override
                        public void onProgressUpdate(float progress, long etaSeconds) {}

                        @Override
                        public void onError(int attempt, String error) {
                            if(attempt == -1) {
                                downloadListener.onError(0,songOrigin + " failed to download. Moving onto next song.");
                                latch.countDown();
                            }
                            else {
                                downloadListener.onError(attempt,error);
                            }
                        }
                    });
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
                        e.printStackTrace();
                        downloadListener.onError(0,songOrigin + " download was interrupted.");
                    }

                    if(fetchedAudioModified[0]) {
                        updatedPlaylist.AddVideoToPlaylist(fetchedAudio[0]);
                        downloadListener.onProgressUpdate(index + 1,audios.size());
                    }
                    else {
                        downloadListener.onError(0,songOrigin + " download failed.");
                    }
                }

                downloadListener.onComplete(updatedPlaylist);
            } else {
                downloadListener.onError(-1,"Failed to fetch Playlist Items from API.");
            }
        });
        thread.start();
    }


    public static PlaylistInfo FetchPlaylistFromUrl(String url) {
        //Determines which playlist source the url is from.
        Platform playlistSource = PlaylistUrlSource(url);

        //Fetches the playlist items using the various platform utilities.
        final CountDownLatch latch = new CountDownLatch(1);
        final PlaylistInfo[] playlist = new PlaylistInfo[1];
        final boolean[] playlistModified = new boolean[1];

        switch(playlistSource) {
            case YOUTUBE:
                YoutubeUtilities.FetchPlaylistItems(url, new FetchPlaylistListener() {
                    @Override
                    public void onComplete(PlaylistInfo fetchedPlaylist) {
                        playlist[0] = fetchedPlaylist;
                        playlistModified[0] = true;
                        latch.countDown();
                    }

                    @Override
                    public void onError(String message) {
                        latch.countDown();
                    }
                });
                break;
            default:
                latch.countDown();
                break;
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
        }

        return playlistModified[0] ? playlist[0] : null;
    }


    public static Platform PlaylistUrlSource(String url) {
        if(YoutubeUtilities.ExtractPlaylistIdFromUrl(url) != null) {
            return Platform.YOUTUBE;
        }
        else {
            return Platform.UNKNOWN;
        }
    }
}
