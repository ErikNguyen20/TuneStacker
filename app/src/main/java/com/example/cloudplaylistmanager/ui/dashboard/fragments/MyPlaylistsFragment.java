package com.example.cloudplaylistmanager.ui.dashboard.fragments;

import android.app.Dialog;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cloudplaylistmanager.MusicPlayer.MediaPlayerActivity;
import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.PlaylistRecyclerAdapter;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewItemClickedListener;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.ui.dashboard.DashboardViewModel;
import com.example.cloudplaylistmanager.ui.playlistviewnested.PlaylistNestedActivity;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class MyPlaylistsFragment extends Fragment {

    private ArrayList<Pair<String,PlaylistInfo>> myPlaylists;
    private PlaylistRecyclerAdapter playlistAdapter;
    DashboardViewModel viewModel;

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
                    Intent intent = new Intent(getActivity(), PlaylistNestedActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(PlaylistNestedActivity.SERIALIZE_TAG,myPlaylists.get(position).second);
                    intent.putExtra(PlaylistNestedActivity.UUID_KEY_TAG,myPlaylists.get(position).first);
                    startActivity(intent);
                }
                else {
                    ShowPlaylistDialog();
                }
            }
        });
        playlistRecyclerView.setAdapter(this.playlistAdapter);

        //Fetches data from the ViewModel.
        this.viewModel = new ViewModelProvider(requireParentFragment()).get(DashboardViewModel.class);
        this.viewModel.getMyPlaylists().observe(getViewLifecycleOwner(), new Observer<ArrayList<Pair<String, PlaylistInfo>>>() {
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

        return view;
    }

    public void ShowPlaylistDialog() {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.popup_text_input);

        TextView title = dialog.findViewById(R.id.popup_title);
        EditText name = dialog.findViewById(R.id.edit_text_name);
        TextView confirm = dialog.findViewById(R.id.textView_confirm_button);

        title.setText(R.string.create_playlist_display);
        name.setHint(R.string.popup_create_playlist_hint);

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputName = name.getText().toString().trim();
                if(!inputName.isEmpty()) {
                    PlaylistInfo newPlaylist = new PlaylistInfo();
                    newPlaylist.setTitle(inputName);

                    //Creates and updates the data.
                    String key = DataManager.getInstance().CreateNewPlaylist(newPlaylist, true, null);
                    viewModel.updateData();
                    dialog.dismiss();
                    Toast.makeText(getContext(),"New Playlist Created.",Toast.LENGTH_SHORT).show();

                    //Launches nested playlist view.
                    Intent intent = new Intent(getActivity(), PlaylistNestedActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(PlaylistNestedActivity.SERIALIZE_TAG,newPlaylist);
                    intent.putExtra(PlaylistNestedActivity.UUID_KEY_TAG,key);
                    startActivity(intent);
                }
            }
        });

        dialog.show();
    }
}