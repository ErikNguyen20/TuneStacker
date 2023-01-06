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
    private static final int ADD_ITEM_TOKEN = -1; //Token that uniquely identifies the add button
    @StringRes
    private int addButtonResId;
    private Context context;
    private ArrayList<PlaylistInfo> playlist;

    public PlaylistRecyclerAdapter(Context context, ArrayList<PlaylistInfo> playlist, @StringRes int addButtonResId) {
        this.context = context;
        this.playlist = playlist;
        this.addButtonResId = addButtonResId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        if(viewType == ADD_ITEM_TOKEN) {
            View view = inflater.inflate(R.layout.single_line_item, parent, false);
            return new ViewHolderAdd(view);
        }
        else {
            View view = inflater.inflate(R.layout.double_line_item, parent, false);
            return new ViewHolderItem(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder.getItemViewType() == ADD_ITEM_TOKEN) {
            ViewHolderAdd viewHolder = (ViewHolderAdd) holder;
            viewHolder.title.setText(this.addButtonResId);
            viewHolder.icon.setImageResource(R.drawable.add_new_image);
        }
        else {
            if(this.playlist.isEmpty()) {
                return;
            }

            ViewHolderItem viewHolder = (ViewHolderItem) holder;
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
            return super.getItemViewType(position);
        }
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
                    Log.e("MyPlaylistItem","Clicked");
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
                    Log.e("MyPlaylistItem","Add Button Clicked");
                }
            });
        }
    }
}
