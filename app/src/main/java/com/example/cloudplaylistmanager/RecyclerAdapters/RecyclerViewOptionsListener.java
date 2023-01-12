package com.example.cloudplaylistmanager.RecyclerAdapters;

import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;

public interface RecyclerViewOptionsListener {
    void SelectMenuOption(int position, final int itemId, String optional);
    void ButtonClicked(int viewType, int position);
}
