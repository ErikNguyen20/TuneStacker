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

        //Fetches data from the ViewModel.
        DashboardViewModel viewModel = new ViewModelProvider(requireParentFragment()).get(DashboardViewModel.class);
        viewModel.getImportedPlaylists().observe(getViewLifecycleOwner(), new Observer<ArrayList<PlaylistInfo>>() {
            @Override
            public void onChanged(ArrayList<PlaylistInfo> playlistInfo) {
                importedPlaylists = playlistInfo;
                playlistAdapter.updateData(playlistInfo);
            }
        });

        //Instantiates new Recycler View and sets the adapter.
        RecyclerView playlistRecyclerView = view.findViewById(R.id.recyclerView_imported);
        playlistRecyclerView.setHasFixedSize(true);
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        this.playlistAdapter = new PlaylistRecyclerAdapter(getContext(),this.importedPlaylists, R.string.import_playlist_display);
        playlistRecyclerView.setAdapter(this.playlistAdapter);

        Log.e("ImportedFragment","OnCreateView");
        return view;
    }
}