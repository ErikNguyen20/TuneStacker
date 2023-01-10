package com.example.cloudplaylistmanager.ui.dashboard.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.cloudplaylistmanager.MusicPlayer.MediaPlayerActivity;
import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.PlaylistRecyclerAdapter;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewItemClickedListener;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.ui.dashboard.DashboardViewModel;
import com.example.cloudplaylistmanager.ui.playlistviewnested.PlaylistNestedActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 */
public class MyPlaylistsFragment extends Fragment {

    private ArrayList<Pair<String,PlaylistInfo>> myPlaylists;
    private PlaylistRecyclerAdapter playlistAdapter;


    public MyPlaylistsFragment() {
        this.myPlaylists = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_playlists, container, false);

        //Instantiates new Recycler View and sets the adapter.
        RecyclerView playlistRecyclerView = view.findViewById(R.id.recyclerView_my_playlists);
        playlistRecyclerView.setHasFixedSize(true);
        playlistRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        this.playlistAdapter = new PlaylistRecyclerAdapter(getContext(), null, R.string.create_playlist_display, new RecyclerViewItemClickedListener() {
            @Override
            public void onClicked(int viewType, int position) {
                if(viewType != PlaylistRecyclerAdapter.ADD_ITEM_TOKEN) {
                    Log.e("MyPlaylistFragment","Clicked Item");
                    Intent intent = new Intent(getActivity(), PlaylistNestedActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(PlaylistNestedActivity.SERIALIZE_TAG,myPlaylists.get(position).second);
                    intent.putExtra(PlaylistNestedActivity.UUID_KEY_TAG,myPlaylists.get(position).first);
                    startActivity(intent);
                }
            }
        });
        playlistRecyclerView.setAdapter(this.playlistAdapter);

        //Fetches data from the ViewModel.
        DashboardViewModel viewModel = new ViewModelProvider(requireParentFragment()).get(DashboardViewModel.class);
        viewModel.getMyPlaylists().observe(getViewLifecycleOwner(), new Observer<ArrayList<Pair<String, PlaylistInfo>>>() {
            @Override
            public void onChanged(ArrayList<Pair<String, PlaylistInfo>> pairs) {
                if(pairs == null) {
                    return;
                }
                myPlaylists = pairs;

                ArrayList<PlaylistInfo> data = new ArrayList<>();
                for(Pair<String, PlaylistInfo> value : pairs) {
                    data.add(value.second);
                }
                playlistAdapter.updateData(data);
            }
        });

                Log.e("MyPlaylists", "OnCreateView");
        return view;
    }
}