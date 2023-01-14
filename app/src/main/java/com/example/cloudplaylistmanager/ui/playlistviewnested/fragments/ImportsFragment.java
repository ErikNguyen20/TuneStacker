package com.example.cloudplaylistmanager.ui.playlistviewnested.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.RecyclerAdapters.PlaylistOptionsRecyclerAdapter;
import com.example.cloudplaylistmanager.RecyclerAdapters.RecyclerViewOptionsListener;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.ExportListener;
import com.example.cloudplaylistmanager.Utils.PlatformCompatUtility;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.example.cloudplaylistmanager.Utils.SerializablePair;
import com.example.cloudplaylistmanager.Utils.SyncPlaylistListener;
import com.example.cloudplaylistmanager.ui.addExistingPopupSingle.AddExistingPopupSingleActivity;
import com.example.cloudplaylistmanager.ui.playlistviewnested.PlaylistNestedActivity;
import com.example.cloudplaylistmanager.ui.playlistviewnested.PlaylistNestedViewModel;
import com.example.cloudplaylistmanager.ui.playlistviewnormal.PlaylistImportActivity;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImportsFragment extends Fragment {
    private static final String TOAST_KEY = "toast_key";
    private static final String PROGRESS_DIALOG_SHOW_KEY = "show_dialog_key";
    private static final String PROGRESS_DIALOG_HIDE_KEY = "hide_dialog_key";
    private static final String PROGRESS_DIALOG_UPDATE_KEY = "update_dialog_key";

    //Handler that handles toast and progress dialog messages.
    private ProgressDialog progressDialog;
    private final Handler toastHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.getData().getString(TOAST_KEY) != null && !msg.getData().getString(TOAST_KEY).isEmpty()) {
                Toast.makeText(getActivity(),msg.getData().getString(TOAST_KEY),Toast.LENGTH_LONG).show();
            }
            if(msg.getData().getString(PROGRESS_DIALOG_UPDATE_KEY) != null && !msg.getData().getString(PROGRESS_DIALOG_UPDATE_KEY).isEmpty()) {
                progressDialog.setMessage(msg.getData().getString(PROGRESS_DIALOG_UPDATE_KEY));
            }
            if(msg.getData().getBoolean(PROGRESS_DIALOG_SHOW_KEY)) {
                progressDialog.show();
            }
            if(msg.getData().getBoolean(PROGRESS_DIALOG_HIDE_KEY)) {
                progressDialog.hide();
            }
        }
    };


    private PlaylistNestedViewModel viewModel;

    private ArrayList<SerializablePair<String, PlaylistInfo>> playlists;
    private String parentUuidKey;
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
            public void SelectMenuOption(int position, int itemId, String optional) {
                if(itemId == R.id.export_option) {
                    StartProgressDialog("Exporting Playlist...");
                    DataManager.getInstance().ExportPlaylist(playlists.get(position).first, new ExportListener() {
                        @Override
                        public void onComplete(String message) {
                            SendToast(message);
                            HideProgressDialog();
                        }

                        @Override
                        public void onProgress(String message) {
                            UpdateProgressDialog(message);
                        }
                    });
                } else if(itemId == R.id.delete_option) {
                    boolean success = DataManager.getInstance().RemovePlaylist(playlists.get(position).first, parentUuidKey);
                    if(success) {
                        viewModel.updateData(parentUuidKey);
                        Toast.makeText(getActivity(),"Playlist Removed.",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getActivity(),"Failed to Remove Playlist.",Toast.LENGTH_SHORT).show();
                    }
                } else if(itemId == R.id.sync_option) {
                    StartProgressDialog("Syncing Playlist...");
                    PlatformCompatUtility.SyncPlaylist(playlists.get(position).first, new SyncPlaylistListener() {
                        @Override
                        public void onComplete() {
                            viewModel.updateData(playlists.get(position).first);
                            SendToast("Successfully Synced Playlist.");
                            HideProgressDialog();
                        }

                        @Override
                        public void onProgress(String message) {
                            UpdateProgressDialog(message);
                        }

                        @Override
                        public void onError(int code, String message) {
                            if(code == -1) {
                                SendToast(message);
                                HideProgressDialog();
                            }
                            else {
                                UpdateProgressDialog(message);
                            }
                        }
                    });
                }
            }

            @Override
            public void ButtonClicked(int viewType, int position) {
                if(viewType != PlaylistOptionsRecyclerAdapter.ADD_ITEM_TOKEN) {
                    Intent intent = new Intent(getActivity(), PlaylistImportActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(PlaylistNestedActivity.SERIALIZE_TAG,playlists.get(position).second);
                    intent.putExtra(PlaylistNestedActivity.UUID_KEY_TAG,playlists.get(position).first);
                    startActivity(intent);
                }
                else {
                    Intent intent = new Intent(getActivity(), AddExistingPopupSingleActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(AddExistingPopupSingleActivity.IS_PLAYLIST_TAG,true);
                    intent.putExtra(AddExistingPopupSingleActivity.PARENT_UUID_TAG,parentUuidKey);
                    startActivity(intent);
                }
            }
        });
        recyclerView.setAdapter(this.adapter);

        //Fetches data from the ViewModel.
        this.viewModel = new ViewModelProvider(requireActivity()).get(PlaylistNestedViewModel.class);
        this.viewModel.getPlaylistData().observe(getViewLifecycleOwner(), new Observer<Pair<String, PlaylistInfo>>() {
            @Override
            public void onChanged(Pair<String, PlaylistInfo> stringPlaylistInfoPair) {
                if(stringPlaylistInfoPair == null) {
                    return;
                }
                playlists = stringPlaylistInfoPair.second.GetImportedPlaylists();
                parentUuidKey = stringPlaylistInfoPair.first;

                ArrayList<PlaylistInfo> data = new ArrayList<>();
                for(SerializablePair<String, PlaylistInfo> value : stringPlaylistInfoPair.second.GetImportedPlaylists()) {
                    data.add(value.second);
                }
                adapter.updateData(data);
            }
        });


        this.progressDialog = new ProgressDialog(getActivity());
        this.progressDialog.setTitle("Syncing Playlist");
        this.progressDialog.setCanceledOnTouchOutside(false);

        return view;
    }

    @Override
    public void onDestroy() {
        if(this.progressDialog != null) {
            this.progressDialog.dismiss();
        }
        super.onDestroy();
    }

    public void SendToast(String message) {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(TOAST_KEY,message);
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }

    public void StartProgressDialog(String message) {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putBoolean(PROGRESS_DIALOG_SHOW_KEY,true);
        bundle.putString(PROGRESS_DIALOG_UPDATE_KEY,message);
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }

    public void UpdateProgressDialog(String message) {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(PROGRESS_DIALOG_UPDATE_KEY,message);
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }

    public void HideProgressDialog() {
        Message msg = this.toastHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putBoolean(PROGRESS_DIALOG_HIDE_KEY,true);
        msg.setData(bundle);
        this.toastHandler.sendMessage(msg);
    }
}