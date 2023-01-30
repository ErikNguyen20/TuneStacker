package com.example.cloudplaylistmanager.ui.dashboard.fragments;


import android.app.ProgressDialog;
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

import android.widget.Toast;

import com.example.cloudplaylistmanager.MusicPlayer.MediaPlayerActivity;
import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewOptionsListener;
import com.example.cloudplaylistmanager.RecyclerAdapters.SongsOptionsRecyclerAdapter;
import com.example.cloudplaylistmanager.RecyclerAdapters.SongsRecyclerAdapter;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.ui.addNewPopupSingle.AddNewPopupSingleActivity;
import com.example.cloudplaylistmanager.ui.dashboard.DashboardViewModel;



/**
 * A simple {@link Fragment} subclass.
 */
public class SavedSongsFragment extends Fragment {

    private PlaylistInfo savedSongs;
    private SongsOptionsRecyclerAdapter songsAdapter;
    private DashboardViewModel viewModel;
    private ProgressDialog progressDialog;


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
        this.songsAdapter = new SongsOptionsRecyclerAdapter(getContext(), this.savedSongs, true, R.menu.single_song_item_option, new RecyclerViewOptionsListener() {
            @Override
            public void SelectMenuOption(int position, int itemId, String optional) {
                if(itemId == R.id.export_option) {
                    //Exports the audio to the music directory.
                    boolean success = DataManager.getInstance().ExportSong(optional,null);
                    if(success) {
                        Toast.makeText(getActivity(),"Song Successfully Exported.",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getActivity(),"Song Failed to Export.",Toast.LENGTH_SHORT).show();
                    }
                } else if(itemId == R.id.delete_option) {
                    //Permanently deletes the song from the device.
                    try {
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.setTitle("Deleting");
                            progressDialog.setMessage("Permanently Deleting Song...");
                            progressDialog.show();

                            //Calls DataManager to delete the song.
                            boolean success = DataManager.getInstance().DeleteSong(optional);
                            if (success) {
                                viewModel.updateData();
                                Toast.makeText(getActivity(), "Song Permanently Deleted.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(), "Song Failed to Delete.", Toast.LENGTH_SHORT).show();
                            }
                            progressDialog.dismiss();
                        });
                    } catch(Exception e) {
                        if(progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(getActivity(), "Song Failed to Delete.", Toast.LENGTH_SHORT).show();
                    }
                } else if(itemId == R.id.play_option) {
                    //Creates a new playlist item.
                    PlaybackAudioInfo audio = savedSongs.getAllVideos().get(position);
                    PlaylistInfo singlePlaylistItem = new PlaylistInfo();
                    singlePlaylistItem.setTitle(audio.getTitle());
                    singlePlaylistItem.AddAudioToPlaylist(audio);

                    //Launches Media Player
                    Intent intent = new Intent(getActivity(),MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,singlePlaylistItem);
                    intent.putExtra(MediaPlayerActivity.POSITION_TAG,position);
                    startActivity(intent);
                }
            }

            @Override
            public void ButtonClicked(int viewType, int position) {
                if(viewType != SongsRecyclerAdapter.ADD_ITEM_TOKEN) {
                    //Launches Media Player
                    Intent intent = new Intent(getActivity(),MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,savedSongs);
                    intent.putExtra(MediaPlayerActivity.POSITION_TAG,position);
                    startActivity(intent);
                }
                else {
                    //Launches Add New popup.
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

        //Sets up progress dialog.
        this.progressDialog = new ProgressDialog(getActivity());
        this.progressDialog.setCanceledOnTouchOutside(false);
        this.progressDialog.setCancelable(false);

        return view;
    }

}