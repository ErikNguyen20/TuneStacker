package com.example.cloudplaylistmanager.RecyclerAdapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

import java.util.ArrayList;

public class SongsOptionsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    public static final int ADD_ITEM_TOKEN = -1; //Token that uniquely identifies the add button

    private Context context;
    private ArrayList<PlaybackAudioInfo> audios;
    RecyclerViewOptionsListener recyclerViewSongsOptionsListener;

    public SongsOptionsRecyclerAdapter(Context context, PlaylistInfo playlist,
                                       RecyclerViewOptionsListener recyclerViewSongsOptionsListener) {
        this.context = context;
        this.audios = new ArrayList<>();
        this.audios.addAll(playlist.getAllVideos());
        this.recyclerViewSongsOptionsListener = recyclerViewSongsOptionsListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        if(viewType == ADD_ITEM_TOKEN) {
            View view = inflater.inflate(R.layout.single_line_item_add, parent, false);
            return new SongsOptionsRecyclerAdapter.ViewHolderAdd(view);
        }
        else {
            View view = inflater.inflate(R.layout.single_line_item_with_options, parent, false);
            return new SongsOptionsRecyclerAdapter.ViewHolderItem(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder.getItemViewType() == ADD_ITEM_TOKEN) {
            SongsOptionsRecyclerAdapter.ViewHolderAdd viewHolder = (SongsOptionsRecyclerAdapter.ViewHolderAdd) holder;
            viewHolder.title.setText(R.string.add_song_display);
        }
        else {
            if(this.audios.isEmpty()) {
                return;
            }
            SongsOptionsRecyclerAdapter.ViewHolderItem viewHolder = (SongsOptionsRecyclerAdapter.ViewHolderItem) holder;

            //Populate the view with information on the UI.
            PlaybackAudioInfo audio = this.audios.get(position);

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
    public int getItemCount() {
        return this.audios.size() + 1;
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

    public void updateData(PlaylistInfo playlist) {
        this.audios.clear();
        this.audios.addAll(playlist.getAllVideos());
        notifyDataSetChanged();
    }

    public void SelectPopupMenuOption(int position, final int optionId) {
        this.recyclerViewSongsOptionsListener.SelectMenuOption(position, optionId);
    }

    public void ButtonClicked(int viewType, int position) {
        this.recyclerViewSongsOptionsListener.ButtonClicked(viewType, position - 1);
    }

    public class ViewHolderItem extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView title;
        public ImageView options;

        public ViewHolderItem(@NonNull View itemView) {
            super(itemView);

            this.icon = itemView.findViewById(R.id.imageView_item);
            this.title = itemView.findViewById(R.id.textView_item_title);
            this.options = itemView.findViewById(R.id.imageView_item_options);

            this.options.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenu = new PopupMenu(context, options);
                    popupMenu.inflate(R.menu.song_item_options);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            SelectPopupMenuOption(getLayoutPosition(), menuItem.getItemId());
                            return true;
                        }
                    });
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

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
                    ButtonClicked(getItemViewType(), getLayoutPosition());
                }
            });
        }
    }
}
