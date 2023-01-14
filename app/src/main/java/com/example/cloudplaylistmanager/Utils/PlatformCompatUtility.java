package com.example.cloudplaylistmanager.Utils;

import android.util.Log;

import com.example.cloudplaylistmanager.Platforms.YoutubeUtilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

public class PlatformCompatUtility {
    private static final String LOG_TAG = "PlatformCompatUtility";
    public enum Platform{
        LOCAL,
        UNKNOWN,
        YOUTUBE,
        SPOTIFY,
        SOUNDCLOUD
    }


    public static void DownloadSong(String url, DownloadListener downloadListener) {
        Thread thread = new Thread(() -> {
            String processedUrl = ProcessUrl(url);

            if(processedUrl != null) {
                DataManager.getInstance().DownloadSongToDirectoryFromUrl(processedUrl, new DownloadListener() {
                    @Override
                    public void onComplete(PlaybackAudioInfo audio) {
                        downloadListener.onComplete(audio);
                    }

                    @Override
                    public void onProgressUpdate(float progress, long etaSeconds) {
                        downloadListener.onProgressUpdate(progress, etaSeconds);
                    }

                    @Override
                    public void onError(int attempt, String error) {
                        downloadListener.onError(attempt, error);
                    }
                });
            }
            else {
                downloadListener.onError(-1,"Failed to Fetch URL.");
            }
        });
        thread.start();
    }

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
                        downloadListener.onProgressUpdate("[" + (index + 1) + "/" + audios.size() +
                                "] Downloaded: " + fetchedAudio[0].getTitle());
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


    public static void SyncPlaylist(String importedKey, SyncPlaylistListener playlistListener) {
        Thread thread = new Thread(() -> {

            PlaylistInfo localPlaylist = DataManager.getInstance().GetPlaylistFromKey(importedKey);
            if(localPlaylist != null && localPlaylist.getLinkSource() != null && !localPlaylist.getLinkSource().isEmpty()) {
                String playlistUrlSource = localPlaylist.getLinkSource();

                playlistListener.onProgress("Fetching song sources from the Playlist.");
                PlaylistInfo updatedPlaylist = FetchPlaylistFromUrl(playlistUrlSource);
                if(updatedPlaylist == null) {
                    playlistListener.onError(-1,"Failed to fetch Playlist Items.");
                    return;
                }


                ArrayList<PlaybackAudioInfo> queue = null;
                //Platform compats can be added here.
                Platform playlistSource = PlaylistUrlSource(playlistUrlSource);
                switch (playlistSource) {
                    case YOUTUBE:
                        queue = YoutubeUtilities.Sync(updatedPlaylist, localPlaylist);
                        break;
                    case UNKNOWN:
                        break;
                }


                if(queue != null && !queue.isEmpty()) {
                    for(int index = 0; index < queue.size(); index++) {
                        PlaybackAudioInfo audio = queue.get(index);
                        playlistListener.onProgress("[" + (index+1) + "/" + queue.size() + "] Downloading " + audio.getTitle());

                        final CountDownLatch latch = new CountDownLatch(1);
                        DataManager.getInstance().DownloadSongToDirectoryFromUrl(audio.getOrigin(), new DownloadListener() {
                            @Override
                            public void onComplete(PlaybackAudioInfo audio) {
                                DataManager.getInstance().AddSongToPlaylist(importedKey,audio);
                                latch.countDown();
                            }

                            @Override
                            public void onProgressUpdate(float progress, long etaSeconds) {}

                            @Override
                            public void onError(int attempt, String error) {
                                playlistListener.onError(0, error);
                                if(attempt == -1) {
                                    latch.countDown();
                                }
                            }
                        });
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
                            e.printStackTrace();
                            playlistListener.onError(0,audio.getTitle() + " download was interrupted.");
                        }
                    }
                }
                playlistListener.onComplete();
            }
            else {
                playlistListener.onError(-1, "Failed to Validate Original Playlist URL.");
            }
        });
        thread.start();
    }

    public static void SyncPlaylistsMultiple(ArrayList<String> importedKeys, SyncPlaylistListener playlistListener) {
        if(importedKeys == null || importedKeys.isEmpty()) {
            playlistListener.onComplete();
            return;
        }
        Thread thread = new Thread(() -> {
            HashSet<String> completedKeys = new HashSet<>();

            for(String key : importedKeys) {
                if(completedKeys.contains(key)) {
                    continue;
                }

                final CountDownLatch latch = new CountDownLatch(1);
                SyncPlaylist(key, new SyncPlaylistListener() {
                    @Override
                    public void onComplete() {
                        playlistListener.onProgress("A Playlist was Synced.");
                        latch.countDown();
                    }

                    @Override
                    public void onProgress(String message) {
                        playlistListener.onProgress(message);
                    }

                    @Override
                    public void onError(int code, String message) {
                        playlistListener.onError(0,message);
                        if(code == -1) {
                            latch.countDown();
                        }
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
                    e.printStackTrace();
                    playlistListener.onError(0,"Playlist Sync was Interrupted.");
                }
                completedKeys.add(key);
            }
            playlistListener.onComplete();
        });
        thread.start();
    }


    public static String ProcessUrl(String url) {
        Platform urlSource = AudioUrlSource(url);

        final CountDownLatch latch = new CountDownLatch(1);
        StringBuilder result = new StringBuilder();

        switch(urlSource) {
            case YOUTUBE:
            case UNKNOWN:
                result.append(url);
                latch.countDown();
                break;
            case SPOTIFY:
                //Implement Compat for Spotify.
                latch.countDown();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
        }

        return result.toString().isEmpty() ? null : result.toString();
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
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
        }

        return playlistModified[0] ? playlist[0] : null;
    }


    public static Platform AudioUrlSource(String url) {
        if(YoutubeUtilities.ExtractVideoIdFromUrl(url) != null) {
            return Platform.YOUTUBE;
        }
        else {
            return Platform.UNKNOWN;
        }
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
