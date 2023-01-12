package com.example.cloudplaylistmanager.RecyclerAdapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;


import java.util.ArrayList;

public class PlaylistRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int ADD_ITEM_TOKEN = -1; //Token that uniquely identifies the add button
    @StringRes
    private int addButtonResId;
    private Context context;
    private ArrayList<PlaylistInfo> playlist;
    RecyclerViewItemClickedListener itemClickedListener;

    public PlaylistRecyclerAdapter(Context context, ArrayList<PlaylistInfo> playlist, @StringRes int addButtonResId,
                                   RecyclerViewItemClickedListener itemClickedListener) {
        this.context = context;
        if(playlist == null) {
            this.playlist = new ArrayList<>();
        }
        else {
            this.playlist = playlist;
        }
        this.addButtonResId = addButtonResId;
        this.itemClickedListener = itemClickedListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        if(viewType == ADD_ITEM_TOKEN) {
            View view = inflater.inflate(R.layout.single_line_item_add, parent, false);
            return new PlaylistRecyclerAdapter.ViewHolderAdd(view);
        }
        else {
            View view = inflater.inflate(R.layout.double_line_item, parent, false);
            return new PlaylistRecyclerAdapter.ViewHolderItem(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder.getItemViewType() == ADD_ITEM_TOKEN) {
            PlaylistRecyclerAdapter.ViewHolderAdd viewHolder = (PlaylistRecyclerAdapter.ViewHolderAdd) holder;
            viewHolder.title.setText(this.addButtonResId);
        }
        else {
            if(this.playlist.isEmpty()) {
                return;
            }

            PlaylistRecyclerAdapter.ViewHolderItem viewHolder = (PlaylistRecyclerAdapter.ViewHolderItem) holder;
            PlaylistInfo currentPlaylist = this.playlist.get(position - 1);
            viewHolder.title.setText(currentPlaylist.getTitle());
            viewHolder.other.setText(new String(" " + currentPlaylist.getAllVideos().size() + " songs"));

            //Sets the icon of the playlist.
            if(!currentPlaylist.getAllVideos().iterator().hasNext()) {
                viewHolder.icon.setImageResource(R.drawable.med_res);
                return;
            }
            PlaybackAudioInfo sourceAudio = currentPlaylist.getAllVideos().iterator().next();
            if(sourceAudio.getThumbnailType() == PlaybackAudioInfo.PlaybackMediaType.LOCAL) {
                Bitmap bitmap = BitmapFactory.decodeFile(sourceAudio.getThumbnailSource());
                if (bitmap != null) {
                    viewHolder.icon.setImageBitmap(bitmap);
                    return;
                }
            }
            viewHolder.icon.setImageResource(R.drawable.med_res);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0) {
            return ADD_ITEM_TOKEN;
        }
        else {
            return super.getItemViewType(position - 1);
        }
    }

    public void ViewHolderClicked(int viewType, int position) {
        this.itemClickedListener.onClicked(viewType,position - 1);
    }

    public void updateData(ArrayList<PlaylistInfo> playlist) {
        this.playlist.clear();
        this.playlist.addAll(playlist);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return this.playlist.size() + 1;
    }

    public class ViewHolderItem extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView title;
        public TextView other;

        public ViewHolderItem(@NonNull View itemView) {
            super(itemView);

            this.icon = itemView.findViewById(R.id.imageView_item);
            this.title = itemView.findViewById(R.id.textView_item_title);
            this.other = itemView.findViewById(R.id.textView_item_other);

            //Make it clickable.
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ViewHolderClicked(getItemViewType(), getLayoutPosition());
                }
            });
        }
    }

    public class ViewHolderAdd extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView title;

        public ViewHolderAdd(@NonNull View itemView) {
            super(itemView);

            this.icon = itemView.findViewById(R.id.imageView_item);
            this.title = itemView.findViewById(R.id.textView_item_title);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ViewHolderClicked(getItemViewType(), getLayoutPosition());
                }
            });
        }
    }
}
