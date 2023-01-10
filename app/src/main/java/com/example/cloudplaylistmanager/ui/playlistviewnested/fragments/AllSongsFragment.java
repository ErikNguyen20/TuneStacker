package com.example.cloudplaylistmanager.ui.playlistviewnested.fragments;

import android.content.Intent;
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

import com.example.cloudplaylistmanager.MusicPlayer.MediaPlayerActivity;
import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewOptionsListener;
import com.example.cloudplaylistmanager.RecyclerAdapters.SongsOptionsRecyclerAdapter;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.ui.playlistviewnested.PlaylistNestedViewModel;

/**
 * A simple {@link Fragment} subclass.
 */
public class AllSongsFragment extends Fragment {

    private PlaylistInfo playlist;
    private SongsOptionsRecyclerAdapter adapter;

    public AllSongsFragment() {
        this.playlist = new PlaylistInfo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_songs, container, false);

        //Instantiates new Recycler View and sets the adapter.
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView_allSongs);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        this.adapter = new SongsOptionsRecyclerAdapter(getContext(), this.playlist, true, new RecyclerViewOptionsListener() {
            @Override
            public void SelectMenuOption(int position, int itemId) {

            }

            @Override
            public void ButtonClicked(int viewType, int position) {
                if(viewType != SongsOptionsRecyclerAdapter.ADD_ITEM_TOKEN) {
                    Intent intent = new Intent(getActivity(), MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,playlist);
                    intent.putExtra(MediaPlayerActivity.POSITION_TAG,position);
                    startActivity(intent);
                }
            }
        });
        recyclerView.setAdapter(this.adapter);


        //Fetches data from the ViewModel.
        PlaylistNestedViewModel viewModel = new ViewModelProvider(requireActivity()).get(PlaylistNestedViewModel.class);
        viewModel.getPlaylistData().observe(getViewLifecycleOwner(), new Observer<Pair<String, PlaylistInfo>>() {
            @Override
            public void onChanged(Pair<String, PlaylistInfo> stringPlaylistInfoPair) {
                if(stringPlaylistInfoPair == null) {
                    return;
                }
                playlist = stringPlaylistInfoPair.second;
                adapter.updateData(stringPlaylistInfoPair.second);
            }
        });

        return view;
    }
}