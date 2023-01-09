package com.example.cloudplaylistmanager.ui.playlistviewnested;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.cloudplaylistmanager.ui.playlistviewnested.fragments.AllSongsFragment;
import com.example.cloudplaylistmanager.ui.playlistviewnested.fragments.ImportsFragment;

public class PlaylistNestedViewPagerAdapter extends FragmentStateAdapter {

    public PlaylistNestedViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == 0) {
            return new AllSongsFragment();
        }
        else {
            return new ImportsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
