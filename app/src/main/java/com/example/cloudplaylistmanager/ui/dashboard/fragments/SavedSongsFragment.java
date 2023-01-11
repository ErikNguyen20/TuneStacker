package com.example.cloudplaylistmanager.ui.dashboard.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cloudplaylistmanager.MainActivity;
import com.example.cloudplaylistmanager.MusicPlayer.MediaPlayerActivity;
import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewItemClickedListener;
import com.example.cloudplaylistmanager.RecyclerAdapters.SongsRecyclerAdapter;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.DownloadListener;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.ui.addNewPopupSingle.AddNewPopupSingleActivity;
import com.example.cloudplaylistmanager.ui.dashboard.DashboardViewModel;

import java.io.File;

/**
 * A simple {@link Fragment} subclass.
 */
public class SavedSongsFragment extends Fragment {

    private PlaylistInfo savedSongs;
    private SongsRecyclerAdapter songsAdapter;
    private DashboardViewModel viewModel;


    public SavedSongsFragment() {
        this.savedSongs = new PlaylistInfo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_songs, container, false);

        //Instantiates new Recycler View and sets the adapter.
        RecyclerView songRecyclerView = view.findViewById(R.id.recyclerView_saved_songs);
        songRecyclerView.setHasFixedSize(true);
        songRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        //Implement a listener to capture clicks on the recycler view items.
        this.songsAdapter = new SongsRecyclerAdapter(getContext(), this.savedSongs, true, new RecyclerViewItemClickedListener() {
            @Override
            public void onClicked(int viewType, int position) {
                if(viewType != SongsRecyclerAdapter.ADD_ITEM_TOKEN) {
                    Intent intent = new Intent(getActivity(),MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,savedSongs);
                    intent.putExtra(MediaPlayerActivity.POSITION_TAG,position);
                    Log.e("ClickedPosition",String.valueOf(position));
                    startActivity(intent);
                }
                else {
                    Intent intent = new Intent(getActivity(), AddNewPopupSingleActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            }
        });
        songRecyclerView.setAdapter(this.songsAdapter);

        //Fetches data from the ViewModel.
        this.viewModel = new ViewModelProvider(requireParentFragment()).get(DashboardViewModel.class);
        this.viewModel.getLocalVideos().observe(getViewLifecycleOwner(), new Observer<PlaylistInfo>() {
            @Override
            public void onChanged(PlaylistInfo playlistInfo) {
                if(playlistInfo == null) {
                    return;
                }
                savedSongs = playlistInfo;
                songsAdapter.updateData(playlistInfo);
            }
        });
        return view;
    }

}