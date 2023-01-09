package com.example.cloudplaylistmanager.RecyclerAdapters;

public interface RecyclerViewOptionsListener {
    void SelectMenuOption(int position, final int itemId);
    void ButtonClicked(int viewType, int position);
}
