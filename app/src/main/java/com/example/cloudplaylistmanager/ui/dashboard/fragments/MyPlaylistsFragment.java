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
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.ui.dashboard.DashboardViewModel;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class MyPlaylistsFragment extends Fragment {

    private ArrayList<PlaylistInfo> myPlaylists;
    private PlaylistRecyclerAdapter playlistAdapter;


    public MyPlaylistsFragment() {
        this.myPlaylists = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_playlists, container, false);

        //Fetches data from the ViewModel.
        DashboardViewModel viewModel = new ViewModelProvider(requireParentFragment()).get(DashboardViewModel.class);
        viewModel.getMyPlaylists().observe(getViewLifecycleOwner(), new Observer<ArrayList<PlaylistInfo>>() {
            @Override
            public void onChanged(ArrayList<PlaylistInfo> playlistInfo) {
                myPlaylists = playlistInfo;
                playlistAdapter.updateData(playlistInfo);
            }
        });

        //Instantiates new Recycler View and sets the adapter.
        RecyclerView playlistRecyclerView = view.findViewById(R.id.recyclerView_my_playlists);
        playlistRecyclerView.setHasFixedSize(true);
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        this.playlistAdapter = new PlaylistRecyclerAdapter(getContext(),this.myPlaylists);
        playlistRecyclerView.setAdapter(this.playlistAdapter);

        Log.e("MyPlaylists","OnCreateView");

        return view;
    }
}