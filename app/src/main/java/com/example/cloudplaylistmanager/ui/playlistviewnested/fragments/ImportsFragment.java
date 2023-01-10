package com.example.cloudplaylistmanager.ui.playlistviewnested.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.PlaylistOptionsRecyclerAdapter;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewOptionsListener;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.ui.playlistviewnested.PlaylistNestedViewModel;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImportsFragment extends Fragment {

    private ArrayList<Pair<String, PlaylistInfo>> playlists;
    private PlaylistOptionsRecyclerAdapter adapter;

    public ImportsFragment() {
        this.playlists = new ArrayList<>();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_imports, container, false);

        //Instantiates new Recycler View and sets the adapter.
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView_imports);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        this.adapter = new PlaylistOptionsRecyclerAdapter(getContext(), null, new RecyclerViewOptionsListener() {
            @Override
            public void SelectMenuOption(int position, int itemId) {

            }

            @Override
            public void ButtonClicked(int viewType, int position) {
                if(viewType != PlaylistOptionsRecyclerAdapter.ADD_ITEM_TOKEN) {

                }
            }
        });
        recyclerView.setAdapter(this.adapter);

        //Fetches data from the ViewModel.
        PlaylistNestedViewModel viewModel = new ViewModelProvider(requireActivity()).get(PlaylistNestedViewModel.class);
        viewModel.getPlaylistData().observe(getViewLifecycleOwner(), new Observer<PlaylistInfo>() {
            @Override
            public void onChanged(PlaylistInfo playlistInfo) {
                if(playlistInfo == null) {
                    return;
                }
                playlists = playlistInfo.GetImportedPlaylists();

                ArrayList<PlaylistInfo> data = new ArrayList<>();
                for(Pair<String, PlaylistInfo> value : playlistInfo.GetImportedPlaylists()) {
                    data.add(value.second);
                }
                adapter.updateData(data);
            }
        });


        return view;
    }
}