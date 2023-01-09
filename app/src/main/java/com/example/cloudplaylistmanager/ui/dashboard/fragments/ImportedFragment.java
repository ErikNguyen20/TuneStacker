package com.example.cloudplaylistmanager.ui.dashboard.fragments;

import android.content.Intent;
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

import com.example.cloudplaylistmanager.MusicPlayer.MediaPlayerActivity;
import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.PlaylistRecyclerAdapter;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewItemClickedListener;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.ui.dashboard.DashboardViewModel;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImportedFragment extends Fragment {

    private ArrayList<PlaylistInfo> importedPlaylists;
    private PlaylistRecyclerAdapter playlistAdapter;

    public ImportedFragment() {
        this.importedPlaylists = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_imported, container, false);

        //Instantiates new Recycler View and sets the adapter.
        RecyclerView playlistRecyclerView = view.findViewById(R.id.recyclerView_imported);
        playlistRecyclerView.setHasFixedSize(true);
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        this.playlistAdapter = new PlaylistRecyclerAdapter(getContext(), this.importedPlaylists, R.string.import_playlist_display, new RecyclerViewItemClickedListener() {
            @Override
            public void onClicked(int viewType, int position) {
                if(viewType != PlaylistRecyclerAdapter.ADD_ITEM_TOKEN) {

                }
            }
        });
        playlistRecyclerView.setAdapter(this.playlistAdapter);


        //Fetches data from the ViewModel.
        DashboardViewModel viewModel = new ViewModelProvider(requireParentFragment()).get(DashboardViewModel.class);
        viewModel.getImportedPlaylists().observe(getViewLifecycleOwner(), new Observer<ArrayList<PlaylistInfo>>() {
            @Override
            public void onChanged(ArrayList<PlaylistInfo> playlistInfo) {
                importedPlaylists = playlistInfo;
                playlistAdapter.updateData(playlistInfo);
            }
        });

        Log.e("ImportedFragment","OnCreateView");
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        //We can induce a refresh of the recycler if needed.
    }
}