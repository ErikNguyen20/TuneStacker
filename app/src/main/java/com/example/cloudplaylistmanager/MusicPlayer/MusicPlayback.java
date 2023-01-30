package com.example.cloudplaylistmanager.MusicPlayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

public class MusicPlayback implements
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener {
    private static final String LOG_TAG = "MusicPlayback";
    public static final int NEXT_SONG_IGNORED = -1;
    public static final int NEXT_SONG_PREV = -2;
    private static final float VOLUME_DEFAULT = 1.0f;
    private static final float VOLUME_DUCK = 0.5f;

    private Context context;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private OnUpdatePlayerListener onUpdatePlayerListener;

    private ArrayList<PlaybackAudioInfo> playlist;
    private ArrayList<Integer> shuffledPositions;
    private HashSet<Integer> errorPreparingPositions;

    private int currentPlayPosition, songCounter;
    private boolean isPaused, isShuffle, isRepeat;
    //Reduced when AudioManager invokes audio focus loss transient can duck
    private boolean isInitialized, isStopped, isReduced, isNoisyRegistered;


    /**
     * Constructs a new MusicPlayback object.
     * @param context Context of the player.
     * @param playlist List of audio objects
     * @param onUpdatePlayerListener Listener for callbacks.
     */
    public MusicPlayback(Context context, ArrayList<PlaybackAudioInfo> playlist,
                         @Nullable OnUpdatePlayerListener onUpdatePlayerListener) {
        this.currentPlayPosition = 0;
        this.songCounter = 0;
        this.errorPreparingPositions = new HashSet<>();

        this.context = context;
        this.playlist = playlist;
        this.onUpdatePlayerListener = onUpdatePlayerListener;
        this.audioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        NewMediaPlayer();
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
     * Links the listener for media callbacks.
     * @param onUpdatePlayerListener Listener for callbacks.
     */
    public void LinkListener(OnUpdatePlayerListener onUpdatePlayerListener) {
        this.onUpdatePlayerListener = onUpdatePlayerListener;
    }

    /**
     * Releases the media player and calls the onEnd callback.
     */
    public void Destroy() {
        if(this.onUpdatePlayerListener != null) {
            this.onUpdatePlayerListener.onEnd();
        }
        if(this.mediaPlayer != null) {
            try {
                if (this.isInitialized && this.mediaPlayer.isPlaying()) {
                    this.mediaPlayer.stop();
                }
            } catch(Exception ignore) {}

            try{ this.mediaPlayer.reset(); } catch(Exception ignore) {}
            this.mediaPlayer.release();
        }
        UnRegisterAudioNoisyReceiver();
        Log.d(LOG_TAG,"Destructor called.");
    }

    /**
     * Starts/Resumes the Music Player.
     */
    public void Play() {
        if(!this.isInitialized) {
            return;
        }
        //If the media player is stopped, prepare it.
        if(this.isStopped) {
            Prepare(null);
            return;
        }
        if(this.isPaused || !this.mediaPlayer.isPlaying()) {
            RequestAudioFocus();
            RegisterAudioNoisyReceiver();
            this.mediaPlayer.start();
            this.isStopped = false;
            this.isPaused = false;
        }

        if(this.onUpdatePlayerListener != null) {
            this.onUpdatePlayerListener.onPauseUpdate(false);
        }
    }

    /**
     * Pauses the Music Player.
     */
    public void Pause() {
        if(!this.isInitialized || this.isPaused || !this.mediaPlayer.isPlaying()) {
            return;
        }

        this.mediaPlayer.pause();
        this.isPaused = true;
        StopAudioFocus();
        UnRegisterAudioNoisyReceiver();

        if(this.onUpdatePlayerListener != null) {
            this.onUpdatePlayerListener.onPauseUpdate(true);
        }
    }

    /**
     * Stops the Music Player.
     */
    public void Stop() {
        if(this.isInitialized) {
            this.mediaPlayer.stop();
            this.isStopped = true;
            this.isPaused = false;
            StopAudioFocus();
            UnRegisterAudioNoisyReceiver();
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
        //Clamps position value within the duration bounds.
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
     * Retrieves Playing state of the Music Player.
     */
    public boolean IsPlaying() {
        if(!this.isInitialized) {
            return false;
        }
        return this.mediaPlayer.isPlaying();
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
     * Sets the volume of the media player
     * @param volume Volume of the player (0.0f-1.0f)
     */
    public void SetVolume(float volume) {
        if(this.mediaPlayer != null) {
            this.mediaPlayer.setVolume(volume, volume);
        }
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
     * Loads and Prepares the next song in the queue from the playlist.
     * @param overridePosition Next position of the song.
     */
    private void NextSong(int overridePosition) {
        if(this.playlist.size() <= 0) {
            Destroy();
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
                    Destroy();
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

        Prepare(audio);
    }

    /**
     * Prepares the media player with a given audio source.
     * If the audio source is null, it assumes that the data source
     * has already been set.
     * @param audio Audio source that will be played.
     */
    private void Prepare(@Nullable PlaybackAudioInfo audio) {
        this.isPaused = false;
        this.isStopped = false;
        this.isInitialized = false;

        try{
            if(audio != null) {
                this.mediaPlayer.reset();
                switch(audio.getAudioType()) {
                    case STREAM:
                    case LOCAL:
                        this.mediaPlayer.setDataSource(audio.getAudioSource());
                        break;
                    case UNKNOWN:
                        throw new Exception("Unknown Media Source");
                }
            }
            mediaPlayer.prepareAsync();
        } catch(Exception e) {
            Log.e(LOG_TAG,(e.getMessage() != null) ?  e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
            //If there is an error preparing this media, skip it to the next one.
            this.errorPreparingPositions.add(this.currentPlayPosition);
            if(this.errorPreparingPositions.size() >= this.playlist.size()) {
                Destroy();
            }
            else {
                NextSong(NEXT_SONG_IGNORED);
            }
        }
    }

    /**
     * Called when the song is finished playing.
     */
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if(mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        this.isInitialized = false;
        NextSong(NEXT_SONG_IGNORED);
    }

    /**
     * Called when the media player encounters an error.
     */
    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
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
     * Called when the media player is prepared.
     */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        this.isInitialized = true;
        this.isPaused = false;

        if(this.onUpdatePlayerListener != null) {
            this.onUpdatePlayerListener.onPrepared(GetDuration());
        }
        Play();

        this.errorPreparingPositions.remove(this.currentPlayPosition);
    }

    /**
     * Request audio focus.
     */
    private boolean RequestAudioFocus() {
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
            AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(this)
                .setAudioAttributes(attrs)
                .build();
            return this.audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }

        return this.audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    /**
     * Stops audio focus.
     */
    private void StopAudioFocus() {
        this.audioManager.abandonAudioFocus(this);
    }

    /**
     * Handles Audio Focus Change.
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch(focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                try{ Stop(); } catch(Exception ignore) {}
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if(!this.isReduced) {
                    SetVolume(VOLUME_DUCK);
                    this.isReduced = true;
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                Play();
                if(this.isReduced) {
                    SetVolume(VOLUME_DEFAULT);
                    this.isReduced = false;
                }
                break;
        }
    }

    //Broadcast receiver used to detect change in audio output source.
    private final BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Pause();
            }
        }
    };

    /**
     * Registers the audio noisy receiver.
     */
    private void RegisterAudioNoisyReceiver() {
        if (!this.isNoisyRegistered) {
            this.context.registerReceiver(this.audioNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            this.isNoisyRegistered = true;
        }
    }

    /**
     * Unregisters the audio noisy receiver
     */
    private void UnRegisterAudioNoisyReceiver() {
        if (this.isNoisyRegistered) {
            this.context.unregisterReceiver(this.audioNoisyReceiver);
            this.isNoisyRegistered = false;
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
        this.mediaPlayer.setWakeMode(this.context, PowerManager.PARTIAL_WAKE_LOCK);

        this.mediaPlayer.setOnCompletionListener(this);
        this.mediaPlayer.setOnPreparedListener(this);
        this.mediaPlayer.setOnErrorListener(this);
    }

    /**
     * Shuffles an array with indices that references the main playlist ArrayList.
     */
    private void GenerateShuffledList() {
        if(this.playlist.size() <= 0) {
            return;
        }

        if(this.shuffledPositions == null) {
            this.shuffledPositions = new ArrayList<>();
            for(int index = 0; index < this.playlist.size(); index++) {
                this.shuffledPositions.add(index);
            }
        }
        Random ran = new Random();
        Collections.shuffle(this.shuffledPositions,ran);
    }
}
