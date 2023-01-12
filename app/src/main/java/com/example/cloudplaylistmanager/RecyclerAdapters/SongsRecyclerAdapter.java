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
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

import java.util.ArrayList;

public class SongsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int ADD_ITEM_TOKEN = -1; //Token that uniquely identifies the add button

    private Context context;
    ArrayList<PlaybackAudioInfo> audios;
    boolean addButtonIncluded;
    RecyclerViewItemClickedListener itemClickedListener;

    public SongsRecyclerAdapter(Context context, PlaylistInfo playlist, boolean addButtonIncluded,
                                RecyclerViewItemClickedListener itemClickedListener) {
        this.context = context;
        this.audios = new ArrayList<>();
        this.audios.addAll(playlist.getAllVideos());
        this.addButtonIncluded = addButtonIncluded;
        this.itemClickedListener = itemClickedListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        if(viewType == ADD_ITEM_TOKEN) {
            View view = inflater.inflate(R.layout.single_line_item_add, parent, false);
            return new SongsRecyclerAdapter.ViewHolderAdd(view);
        }
        else {
            View view = inflater.inflate(R.layout.single_line_item, parent, false);
            return new SongsRecyclerAdapter.ViewHolderItem(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder.getItemViewType() == ADD_ITEM_TOKEN) {
            SongsRecyclerAdapter.ViewHolderAdd viewHolder = (SongsRecyclerAdapter.ViewHolderAdd) holder;
            viewHolder.title.setText(R.string.add_song_display);
        }
        else {
            if(this.audios.isEmpty()) {
                return;
            }
            SongsRecyclerAdapter.ViewHolderItem viewHolder = (SongsRecyclerAdapter.ViewHolderItem) holder;

            //Populate the view with information on the UI.
            PlaybackAudioInfo audio = this.audios.get(this.addButtonIncluded ? position - 1 : position);

            viewHolder.title.setText(audio.getTitle());
            if(audio.getThumbnailType() == PlaybackAudioInfo.PlaybackMediaType.LOCAL) {
                Bitmap bitmap = BitmapFactory.decodeFile(audio.getThumbnailSource());
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
        if(this.addButtonIncluded && position == 0) {
            return ADD_ITEM_TOKEN;
        }
        else {
            return super.getItemViewType(position);
        }
    }

    public void ViewHolderClicked(int viewType, int position) {
        if(this.itemClickedListener != null) {
            this.itemClickedListener.onClicked(viewType,this.addButtonIncluded ? position - 1 : position);
        }
    }

    public ArrayList<PlaybackAudioInfo> getData() {
        return this.audios;
    }

    public void updateData(PlaylistInfo playlist) {
        this.audios.clear();
        this.audios.addAll(playlist.getAllVideos());
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return this.addButtonIncluded ? this.audios.size() + 1 : this.audios.size();
    }

    public class ViewHolderItem extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView title;

        public ViewHolderItem(@NonNull View itemView) {
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

    public class ViewHolderAdd extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView title;

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
