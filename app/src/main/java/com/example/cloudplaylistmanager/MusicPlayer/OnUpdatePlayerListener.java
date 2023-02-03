package com.example.cloudplaylistmanager.MusicPlayer;

import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;

public interface OnUpdatePlayerListener {
    void onPauseUpdate(boolean isPaused);

    void onSeekChange(int pos);
    void onShuffleUpdate(boolean isShuffled);
    void onRepeatUpdate(boolean isRepeat);
    void onSongChange(PlaybackAudioInfo audio, int position);
    void onPrepared(int duration);
    void onEnd();
}
