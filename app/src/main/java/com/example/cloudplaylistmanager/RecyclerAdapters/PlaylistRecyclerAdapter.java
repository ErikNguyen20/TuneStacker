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
import androidx.recyclerview.widget.RecyclerView;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

import java.util.ArrayList;

public class PlaylistRecyclerAdapter extends RecyclerView.Adapter<PlaylistRecyclerAdapter.ViewHolder> {
    private Context context;
    private ArrayList<PlaylistInfo> playlist;

    public PlaylistRecyclerAdapter(Context context, ArrayList<PlaylistInfo> playlist) {
        this.context = context;
        this.playlist = playlist;
    }

    @NonNull
    @Override
    public PlaylistRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(this.context).inflate(R.layout.playlist_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistRecyclerAdapter.ViewHolder holder, int position) {
        if(this.playlist.isEmpty()) {
            return;
        }
        PlaylistInfo currentPlaylist = this.playlist.get(position);

        holder.title.setText(currentPlaylist.getTitle());

        //Sets the icon of the playlist.
        if(currentPlaylist.getVideos().isEmpty()) {
            holder.icon.setImageResource(R.drawable.med_res);
            return;
        }
        PlaybackAudioInfo sourceAudio = currentPlaylist.getVideos().get(0);
        if(sourceAudio.getThumbnailType() == PlaybackAudioInfo.PlaybackMediaType.LOCAL) {
            Bitmap bitmap = BitmapFactory.decodeFile(sourceAudio.getThumbnailSource());
            if (bitmap != null) {
                holder.icon.setImageBitmap(bitmap);
                return;
            }
        }
        holder.icon.setImageResource(R.drawable.med_res);

    }

    public void updateData(ArrayList<PlaylistInfo> playlist) {
        this.playlist.clear();
        this.playlist.addAll(playlist);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return this.playlist.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView title;
        TextView other;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.icon = itemView.findViewById(R.id.imageView_playlist_item);
            this.title = itemView.findViewById(R.id.textView_playlist_item_title);
            this.other = itemView.findViewById(R.id.textView_playlist_item_other);

            //Make it clickable.
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.e("MyPlaylistItem","Clicked");
                }
            });
        }
    }
}
