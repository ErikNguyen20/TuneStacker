package com.example.cloudplaylistmanager.ui.playlistviewnested.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.cloudplaylistmanager.MusicPlayer.MediaPlayerActivity;
import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewOptionsListener;
import com.example.cloudplaylistmanager.RecyclerAdapters.SongsOptionsRecyclerAdapter;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.ui.addExistingPopupSingle.AddExistingPopupSingleActivity;
import com.example.cloudplaylistmanager.ui.playlistviewnested.PlaylistNestedViewModel;
import com.example.cloudplaylistmanager.ui.playlistviewnormal.PlaylistImportActivity;

import java.util.HashMap;

/**
 * A simple {@link Fragment} subclass.
 */
public class AllSongsFragment extends Fragment {

    private PlaylistNestedViewModel viewModel;

    private String parentUuid;
    private PlaylistInfo playlist;
    private SongsOptionsRecyclerAdapter adapter;
    RecyclerView recyclerView;

    public AllSongsFragment() {
        this.playlist = new PlaylistInfo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_songs, container, false);

        //Gets the Recycler View and sets the adapter.
        this.recyclerView = view.findViewById(R.id.recyclerView_allSongs);
        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        this.adapter = new SongsOptionsRecyclerAdapter(getContext(), this.playlist, true, R.menu.song_item_options, new RecyclerViewOptionsListener() {
            @Override
            public void SelectMenuOption(int position, int itemId, String optional) {
                if(itemId == R.id.export_option) {
                    //Exports the selected song.
                    boolean success = DataManager.getInstance().ExportSong(optional,null);
                    if(success) {
                        Toast.makeText(getActivity(),"Song Successfully Exported.",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getActivity(),"Song Failed to Export.",Toast.LENGTH_SHORT).show();
                    }
                } else if(itemId == R.id.delete_option) {
                    //Removes the selected song.
                    boolean remove = DataManager.getInstance().RemoveSongFromPlaylist(parentUuid, optional);
                    if(remove) {
                        viewModel.updateData(parentUuid);
                        Toast.makeText(getActivity(),"Song Removed.",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getActivity(),"Failed to Remove Song.",Toast.LENGTH_SHORT).show();
                    }
                } else if(itemId == R.id.play_option) {
                    //Creates a new playlist item.
                    PlaybackAudioInfo audio = playlist.getAllVideos().get(position);
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
                if(viewType != SongsOptionsRecyclerAdapter.ADD_ITEM_TOKEN) {
                    //Starts the media player.
                    Intent intent = new Intent(getActivity(), MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,playlist);
                    intent.putExtra(MediaPlayerActivity.POSITION_TAG,position);
                    startActivity(intent);
                }
                else {
                    //Starts the add existing popup.
                    Intent intent = new Intent(getActivity(), AddExistingPopupSingleActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(AddExistingPopupSingleActivity.IS_PLAYLIST_TAG,false);
                    intent.putExtra(AddExistingPopupSingleActivity.PARENT_UUID_TAG,parentUuid);
                    startActivity(intent);
                }
            }
        });
        this.recyclerView.setAdapter(this.adapter);


        //Fetches data from the ViewModel.
        this.viewModel = new ViewModelProvider(requireActivity()).get(PlaylistNestedViewModel.class);
        this.viewModel.getPlaylistData().observe(getViewLifecycleOwner(), new Observer<Pair<String, PlaylistInfo>>() {
            @Override
            public void onChanged(Pair<String, PlaylistInfo> stringPlaylistInfoPair) {
                if(stringPlaylistInfoPair == null) {
                    return;
                }
                parentUuid = stringPlaylistInfoPair.first;
                playlist = stringPlaylistInfoPair.second;
                adapter.updateData(stringPlaylistInfoPair.second);
            }
        });

        //Sets the callback for the item touch helper.
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(this.simpleCallback);
        itemTouchHelper.attachToRecyclerView(this.recyclerView);

        return view;
    }


    //Handles dragging of items in the recycler view.
    private ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            if(viewHolder.getItemViewType() == SongsOptionsRecyclerAdapter.ADD_ITEM_TOKEN || target.getItemViewType() == SongsOptionsRecyclerAdapter.ADD_ITEM_TOKEN) {
                return false;
            }
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            //Swaps positions of the items in the adapter.
            adapter.swapDataPoints(fromPosition, toPosition);

            return true;
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);

            //Updates the positioning of the audios in DataManager.
            HashMap<String, Integer> order = playlist.SetItemsOrder(adapter.getAudios());
            DataManager.getInstance().UpdateOrderOfItemsInPlaylist(parentUuid,order);
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if(viewHolder.getItemViewType() == SongsOptionsRecyclerAdapter.ADD_ITEM_TOKEN) {
                return 0;
            }
            else {
                return super.getMovementFlags(recyclerView, viewHolder);
            }
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
    };
}