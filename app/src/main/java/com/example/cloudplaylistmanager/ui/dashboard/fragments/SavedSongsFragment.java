package com.example.cloudplaylistmanager.ui.dashboard.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.example.cloudplaylistmanager.MusicPlayer.MediaPlayerActivity;
import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewItemClickedListener;
import com.example.cloudplaylistmanager.RecyclerAdapters.SongsRecyclerAdapter;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.ui.dashboard.DashboardViewModel;

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
                    startActivity(intent);
                }
                else {
                    LaunchDialogPopup();
                }
            }
        });
        songRecyclerView.setAdapter(this.songsAdapter);

        //Fetches data from the ViewModel.
        DashboardViewModel viewModel = new ViewModelProvider(requireParentFragment()).get(DashboardViewModel.class);
        viewModel.getLocalVideos().observe(getViewLifecycleOwner(), new Observer<PlaylistInfo>() {
            @Override
            public void onChanged(PlaylistInfo playlistInfo) {
                savedSongs = playlistInfo;
                songsAdapter.updateData(playlistInfo);
            }
        });
        return view;
    }


    public void LaunchDialogPopup() {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.create_new_song_popup);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView downloadButton = dialog.findViewById(R.id.textView_download_button);
        TextView chooseButton = dialog.findViewById(R.id.textView_choose_file);
        TextView fileName = dialog.findViewById(R.id.textView_file_name);
        EditText urlField = dialog.findViewById(R.id.edit_text_url);

        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //fileName.setText();
            }
        });
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}