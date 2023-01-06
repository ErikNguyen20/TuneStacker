package com.example.cloudplaylistmanager.Utils;

public interface OnUpdatePlayerListener {
    void onPauseUpdate(boolean isPaused);
    void onShuffleUpdate(boolean isShuffled);
    void onRepeatUpdate(boolean isRepeat);
    void onSongChange(PlaybackAudioInfo audio);
    void onPrepared(int duration);
}
