package com.example.cloudplaylistmanager.ui.dashboard;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.cloudplaylistmanager.ui.dashboard.fragments.ImportedFragment;
import com.example.cloudplaylistmanager.ui.dashboard.fragments.MyPlaylistsFragment;
import com.example.cloudplaylistmanager.ui.dashboard.fragments.SavedSongsFragment;
import com.example.cloudplaylistmanager.ui.home.HomeFragment;

public class DashboardViewPagerAdapter extends FragmentStateAdapter {

    public DashboardViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch(position) {
            case 0: return new MyPlaylistsFragment();
            case 1: return new ImportedFragment();
            case 2: return new SavedSongsFragment();
        }
        return new MyPlaylistsFragment();
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
