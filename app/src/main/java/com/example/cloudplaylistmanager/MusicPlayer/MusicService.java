package com.example.cloudplaylistmanager.MusicPlayer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;


/**
 * Music Service. Implements {@link MediaPlayer} as a service, allowing
 * for background play. This service is required to be bound by a service connection
 * to use the various methods.
 */
public class MusicService extends Service implements
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener {
    private static final String LOG_TAG = "MusicService";
    public static final int NEXT_SONG_IGNORED = -1;
    public static final int NEXT_SONG_PREV = -2;

    private MusicServiceBinder musicBinder = new MusicServiceBinder();
    private OnUpdatePlayerListener onUpdatePlayerListener = null;
    private MediaPlayer mediaPlayer = null;

    private ArrayList<PlaybackAudioInfo> playlist = null;
    private ArrayList<Integer> shuffledPositions;
    private HashSet<Integer> errorPreparingPositions;
    private int currentPlayPosition, songCounter;
    private boolean isShuffle, isRepeat = false;
    private boolean isPaused, isReduced = false; //Reduced when AudioManager invokes audio focus loss transient can duck
    private boolean isInitialized;
    private int bufferingProgressPercent = 0;

    /**
     * Initializes the Music Service. This method must be called before using the service.
     * @param playlist Desired audio contents that will be played by the service.
     */
    public void InitializePlayer(ArrayList<PlaybackAudioInfo> playlist, OnUpdatePlayerListener onUpdatePlayerListener) {
        if(this.mediaPlayer == null) {
            NewMediaPlayer();
        }
        this.isInitialized = false;
        this.mediaPlayer.reset();
        this.currentPlayPosition = 0;
        this.songCounter = 0;
        this.playlist = playlist;
        this.onUpdatePlayerListener = onUpdatePlayerListener;
        this.errorPreparingPositions = new HashSet<>();
        GenerateShuffledList();
    }

    /**
     * Method that begins playing audio from the music service.
     * @param startPos Starting audio position in the list.
     * @param shuffle True = Music Player will select songs randomly.
     * @param repeat True = Music Player will repeat the playlist after it is finished.
     */
    public void BeginPlaying(int startPos, boolean shuffle, boolean repeat) {
        this.isShuffle = shuffle;
        this.isRepeat = repeat;

        if(startPos == NEXT_SONG_IGNORED) {
            this.songCounter = -1;
        }

        NextSong(startPos);
    }



    /**
     * Resumes the Music Player.
     */
    public void Resume() {
        if(!this.isInitialized || !this.isPaused) {
            return;
        }
        this.mediaPlayer.start();
        this.isPaused = false;

        if(this.onUpdatePlayerListener != null) {
            this.onUpdatePlayerListener.onPauseUpdate(false);
        }
    }

    /**
     * Stops the Music Player.
     */
    public void Stop() {
        if(!this.isInitialized) {
            return;
        }
        this.mediaPlayer.stop();
        this.isPaused = false;
    }

    /**
     * Pauses the Music Player.
     */
    public void Pause() {
        if(!this.isInitialized || this.isPaused) {
            return;
        }
        this.mediaPlayer.pause();
        this.isPaused = true;

        if(this.onUpdatePlayerListener != null) {
            this.onUpdatePlayerListener.onPauseUpdate(true);
        }
    }

    /**
     * Moves media to time position.
     * @param pos Time position in milliseconds.
     */
    public void SeekTo(int pos) {
        if(!this.isInitialized) {
            return;
        }
        if(pos > this.mediaPlayer.getDuration()) {
            pos = this.mediaPlayer.getDuration();
        }
        else if(pos < 0) {
            pos = 0;
        }
        this.mediaPlayer.seekTo(pos);
    }

    /**
     * Forcefully switches current playing song to another.
     * @param pos New song's position in the given playlist.
     */
    public void SwitchSong(int pos) {
        Stop();
        NextSong(pos);
    }

    /**
     * Retrieves the total duration of the current audio.
     */
    public int GetDuration() {
        if(!this.isInitialized) {
            return 0;
        }
        return this.mediaPlayer.getDuration();
    }

    /**
     * Retrieves the current time position of the Music Player.
     */
    public int GetCurrentPosition() {
        if(!this.isInitialized) {
            return 0;
        }
        return this.mediaPlayer.getCurrentPosition();
    }

    /**
     * Retrieves the current buffering progress of the Music Player (0-100).
     */
    public int GetBufferingProgress() {
        if(!this.isInitialized) {
            return 0;
        }
        return this.bufferingProgressPercent;
    }

    /**
     * Retrieves the current audio's information.
     */
    public PlaybackAudioInfo GetAudioInfo() {
        return this.playlist.get(this.currentPlayPosition);
    }

    /**
     * Retrieves the current audio's index opsition.
     */
    public int GetCurrentSongIndex() {
        return this.currentPlayPosition;
    }

    /**
     * Retrieves Paused state of the Music Player.
     */
    public boolean IsPaused() {
        return this.isPaused;
    }

    /**
     * Sets the shuffle mode of the Music Player
     * @param shuffle True = Shuffle mode on.
     */
    public void SetShuffle(boolean shuffle) {
        this.isShuffle = shuffle;
        if(this.onUpdatePlayerListener != null) {
            this.onUpdatePlayerListener.onShuffleUpdate(this.isShuffle);
        }
    }

    /**
     * Retrieves Shuffled state of the Music Player.
     */
    public boolean IsShuffle() {
        return this.isShuffle;
    }

    /**
     * Sets the repeat mode of the Music Player
     * @param repeat True = Repeat mode on.
     */
    public void SetRepeat(boolean repeat) {
        this.isRepeat = repeat;
        if(this.onUpdatePlayerListener != null) {
            this.onUpdatePlayerListener.onRepeatUpdate(this.isRepeat);
        }
    }

    /**
     * Retrieves Repeated state of the Music Player.
     */
    public boolean IsRepeat() {
        return this.isRepeat;
    }

    /**
     * Loads and Prepares the next song in the queue from the playlist.
     */
    private void NextSong(int overridePosition) {
        if(this.playlist.size() <= 0) {
            if(this.onUpdatePlayerListener != null) {
                this.onUpdatePlayerListener.onEnd();
            }
            stopSelf();
            return;
        }

        if(overridePosition == NEXT_SONG_PREV) {
            //Sets the currentPlayPosition to the previous song.
            this.songCounter--;
            if(this.songCounter < 0) {
                GenerateShuffledList();
                this.songCounter = this.playlist.size() - 1;
            }
            if(this.isShuffle) {
                this.currentPlayPosition = this.shuffledPositions.get(this.songCounter);
            }
            else {
                this.currentPlayPosition = this.songCounter;
            }
        }
        else if(overridePosition == NEXT_SONG_IGNORED) {
            //Sets the currentPlayPosition to the next song.
            this.songCounter++;
            if(this.songCounter >= this.playlist.size()) {
                if(!this.isRepeat) {
                    if(this.onUpdatePlayerListener != null) {
                        this.onUpdatePlayerListener.onEnd();
                    }
                    stopSelf();
                    return;
                }
                //If the song counter reaches the end of the list, re-generate a shuffled list.
                GenerateShuffledList();
                this.songCounter = 0;
            }
            if(this.isShuffle) {
                this.currentPlayPosition = this.shuffledPositions.get(this.songCounter);
            }
            else {
                this.currentPlayPosition = this.songCounter;
            }
        }
        else {
            //Force select song
            this.songCounter = overridePosition % this.playlist.size();
            this.currentPlayPosition = this.songCounter;
        }


        //Selects the audio from the playlist.
        PlaybackAudioInfo audio = this.playlist.get(this.currentPlayPosition);
        if(this.onUpdatePlayerListener != null) {
            this.onUpdatePlayerListener.onSongChange(audio, this.currentPlayPosition);
            this.onUpdatePlayerListener.onPauseUpdate(this.isPaused);
            this.onUpdatePlayerListener.onShuffleUpdate(this.isShuffle);
            this.onUpdatePlayerListener.onRepeatUpdate(this.isRepeat);
        }
        this.isPaused = false;
        this.isInitialized = false;
        this.mediaPlayer.reset();
        this.bufferingProgressPercent = 0;

        Log.d("MusicPlayer","Selected Song - \"" + audio.getAudioSource() + "\"");
        //Sets the datasource and prepares the audio based on the type.
        try{
            switch(audio.getAudioType()) {
                case STREAM:
                case LOCAL:
                    this.mediaPlayer.setDataSource(audio.getAudioSource());
                    break;
                case UNKNOWN:
                    throw new Exception("Unknown Media Source");
            }
            mediaPlayer.prepareAsync();
        } catch(Exception e) {
            Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
            //If there is an error preparing this media, skip it to the next one.
            this.errorPreparingPositions.add(this.currentPlayPosition);
            if(this.errorPreparingPositions.size() >= this.playlist.size()) {
                if(this.onUpdatePlayerListener != null) {
                    this.onUpdatePlayerListener.onEnd();
                }
                stopSelf();
            }
            else {
                NextSong(NEXT_SONG_IGNORED);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.errorPreparingPositions = new HashSet<>();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(this.mediaPlayer != null) {
            if(this.mediaPlayer.isPlaying()) {
                this.mediaPlayer.stop();
            }
            this.mediaPlayer.reset();
            this.mediaPlayer.release();
        }
        Log.d("MusicPlayer","Destructor called.");
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mP, int i) {
        this.bufferingProgressPercent = i;
    }

    @Override
    public void onCompletion(MediaPlayer mP) {
        if(mP.isPlaying()) {
            mP.stop();
        }
        NextSong(NEXT_SONG_IGNORED);
    }

    @Override
    public void onPrepared(MediaPlayer mP) {
        this.isInitialized = true;
        if(this.onUpdatePlayerListener != null) {
            this.onUpdatePlayerListener.onPrepared(GetDuration());
        }
        if(!mP.isPlaying()) {
            Log.d("MusicPlayer","Playing.");
            mP.start();
        }
        this.errorPreparingPositions.remove(this.currentPlayPosition);
    }

    @Override
    public boolean onError(MediaPlayer mP, int what, int extra) {
        if(what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            Log.e("MusicPlayer","Media Error - Server Died.");
            NewMediaPlayer();
        }
        switch(extra) {
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                Log.e(LOG_TAG,"Media Error - Timed out.");
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                Log.e(LOG_TAG,"Media Error - IO.");
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                Log.e(LOG_TAG,"Media Error - Malformed.");
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                Log.e(LOG_TAG,"Media Error - Unsupported.");
                break;
            default:
                Log.e(LOG_TAG, "Media Error - Unknown");
        }
        return false;
    }

    /**
     * Handles Audio Focus Change.
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch(focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if(!this.isReduced) {
                    mediaPlayer.setVolume(0.5f,0.5f);
                    this.isReduced = true;
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                Resume();
                if(this.isReduced) {
                    mediaPlayer.setVolume(1f,1f);
                    this.isReduced = false;
                }
                break;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.musicBinder;
    }
    public class MusicServiceBinder extends Binder {
        public MusicService getBinder() {
            return MusicService.this;
        }
    }

    /**
     * Instantiates a new Media Player Object.
     */
    private void NewMediaPlayer() {
        if(this.mediaPlayer != null) {
            if(this.mediaPlayer.isPlaying()) {
                this.mediaPlayer.stop();
            }
            this.mediaPlayer.release();
        }
        this.isPaused = false;
        this.mediaPlayer = new MediaPlayer();
        this.mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

        this.mediaPlayer.setOnCompletionListener(this);
        this.mediaPlayer.setOnPreparedListener(this);
        this.mediaPlayer.setOnBufferingUpdateListener(this);
        this.mediaPlayer.setOnErrorListener(this);
    }

    /**
     * Shuffles an array with indices that references the main playlist ArrayList.
     */
    private void GenerateShuffledList() {
        if(this.playlist.size() <= 0) {
            return;
        }

        Random ran = new Random();
        if(this.shuffledPositions == null) {
            this.shuffledPositions = new ArrayList<>();
            for(int index = 0; index < this.playlist.size(); index++) {
                this.shuffledPositions.add(index);
            }
        }
        Collections.shuffle(this.shuffledPositions,ran);
    }
}
