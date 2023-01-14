package com.example.cloudplaylistmanager.RecyclerAdapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

import java.util.ArrayList;

public class SelectItemsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<String> listOfUUids;
    private Context context;
    private boolean isPlaylist;
    private RecyclerViewSelectItemsListener recyclerViewSelectItemsListener;

    public SelectItemsRecyclerAdapter(Context context, boolean isPlaylist, ArrayList<String> listOfUUids,
                                      RecyclerViewSelectItemsListener recyclerViewSelectItemsListener) {
        this.context = context;
        this.isPlaylist = isPlaylist;
        this.listOfUUids = listOfUUids;
        if(this.listOfUUids == null) {
            this.listOfUUids = new ArrayList<>();
        }
        this.recyclerViewSelectItemsListener = recyclerViewSelectItemsListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        if(this.isPlaylist) {
            View view = inflater.inflate(R.layout.double_line_item_with_radio_button, parent, false);
            return new SelectItemsRecyclerAdapter.ViewHolderPlaylist(view);
        }
        else {
            View view = inflater.inflate(R.layout.single_line_item_with_radio_button, parent, false);
            return new SelectItemsRecyclerAdapter.ViewHolderAudio(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(this.isPlaylist) {
            SelectItemsRecyclerAdapter.ViewHolderPlaylist viewHolder = (SelectItemsRecyclerAdapter.ViewHolderPlaylist) holder;

            String uuidLink = this.listOfUUids.get(position);
            viewHolder.uuidLink = uuidLink;

            Pair<PlaylistInfo, Boolean> playlistInfo = this.recyclerViewSelectItemsListener.FetchPlaylistInformation(uuidLink);
            if(playlistInfo == null) {
                return;
            }
            viewHolder.checkBox.setChecked(playlistInfo.second);
            viewHolder.title.setText(playlistInfo.first.getTitle());
            viewHolder.other.setText(new String(" " + playlistInfo.first.getAllVideos().size() + " songs"));

            //Sets the icon of the playlist.
            if(!playlistInfo.first.getAllVideos().iterator().hasNext()) {
                viewHolder.icon.setImageResource(R.drawable.med_res);
                return;
            }
            PlaybackAudioInfo sourceAudio = playlistInfo.first.getAllVideos().iterator().next();
            Bitmap bitmap = DataManager.GetThumbnailImage(sourceAudio);
            if(bitmap != null) {
                viewHolder.icon.setImageBitmap(bitmap);
            }
            else {
                viewHolder.icon.setImageResource(R.drawable.med_res);
            }
        }
        else {
            SelectItemsRecyclerAdapter.ViewHolderAudio viewHolder = (SelectItemsRecyclerAdapter.ViewHolderAudio) holder;

            String uuidLink = this.listOfUUids.get(position);
            viewHolder.uuidLink = uuidLink;

            Pair<PlaybackAudioInfo, Boolean> audioInfo = this.recyclerViewSelectItemsListener.FetchAudioInformation(uuidLink);
            if(audioInfo == null) {
                return;
            }
            viewHolder.checkBox.setChecked(audioInfo.second);
            viewHolder.title.setText(audioInfo.first.getTitle());

            if(audioInfo.first.getThumbnailType() == PlaybackAudioInfo.PlaybackMediaType.LOCAL) {
                Bitmap bitmap = BitmapFactory.decodeFile(audioInfo.first.getThumbnailSource());
                if (bitmap != null) {
                    viewHolder.icon.setImageBitmap(bitmap);
                    return;
                }
            }
            viewHolder.icon.setImageResource(R.drawable.med_res);
        }
    }

    public void FilterResults(ArrayList<String> listOfUUidsNew) {
        this.listOfUUids.clear();
        this.listOfUUids.addAll(listOfUUidsNew);
        notifyDataSetChanged();
    }

    public void ItemSelected(String uuid) {
        this.recyclerViewSelectItemsListener.ButtonClicked(uuid);
    }

    @Override
    public int getItemCount() {
        return this.listOfUUids.size();
    }

    public class ViewHolderAudio extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView title;
        public CheckBox checkBox;
        public String uuidLink;

        public ViewHolderAudio(@NonNull View itemView) {
            super(itemView);

            this.icon = itemView.findViewById(R.id.imageView_item);
            this.title = itemView.findViewById(R.id.textView_item_title);
            this.checkBox = itemView.findViewById(R.id.checkBox);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ItemSelected(uuidLink);
                }
            });
        }
    }

    public class ViewHolderPlaylist extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView title;
        public TextView other;
        public CheckBox checkBox;
        public String uuidLink;

        public ViewHolderPlaylist(@NonNull View itemView) {
            super(itemView);

            this.icon = itemView.findViewById(R.id.imageView_item);
            this.title = itemView.findViewById(R.id.textView_item_title);
            this.other = itemView.findViewById(R.id.textView_item_other);
            this.checkBox = itemView.findViewById(R.id.checkBox);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ItemSelected(uuidLink);
                }
            });
        }
    }
}
