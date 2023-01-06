package com.example.cloudplaylistmanager.ui.dashboard.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.PlaylistRecyclerAdapter;
import com.example.cloudplaylistmanager.RecyclerAdapters.SongsRecyclerAdapter;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.ui.dashboard.DashboardViewModel;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class SavedSongsFragment extends Fragment {

    private PlaylistInfo savedSongs;
    private SongsRecyclerAdapter songsAdapter;

    public SavedSongsFragment() {
        this.savedSongs = new PlaylistInfo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_songs, container, false);

        //Fetches data from the ViewModel.
        DashboardViewModel viewModel = new ViewModelProvider(requireParentFragment()).get(DashboardViewModel.class);
        viewModel.getLocalVideos().observe(getViewLifecycleOwner(), new Observer<PlaylistInfo>() {
            @Override
            public void onChanged(PlaylistInfo playlistInfo) {
                savedSongs = playlistInfo;
                songsAdapter.updateData(playlistInfo);
            }
        });

        //Instantiates new Recycler View and sets the adapter.
        RecyclerView songRecyclerView = view.findViewById(R.id.recyclerView_saved_songs);
        songRecyclerView.setHasFixedSize(true);
        songRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        this.songsAdapter = new SongsRecyclerAdapter(getContext(),this.savedSongs, true);
        songRecyclerView.setAdapter(this.songsAdapter);

        return view;
    }
}