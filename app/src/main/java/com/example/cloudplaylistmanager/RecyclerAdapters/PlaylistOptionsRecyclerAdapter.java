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

public class PlaylistOptionsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int ADD_ITEM_TOKEN = -1; //Token that uniquely identifies the add button

    private Context context;
    private ArrayList<PlaylistInfo> playlists;
    RecyclerViewOptionsListener recyclerViewPlaylistOptionsListener;

    public PlaylistOptionsRecyclerAdapter(Context context, ArrayList<PlaylistInfo> playlist,
                                          RecyclerViewOptionsListener recyclerViewPlaylistOptionsListener) {
        this.context = context;
        if(playlist == null) {
            this.playlists = new ArrayList<>();
        }
        else {
            this.playlists = playlist;
        }
        this.recyclerViewPlaylistOptionsListener = recyclerViewPlaylistOptionsListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        if(viewType == ADD_ITEM_TOKEN) {
            View view = inflater.inflate(R.layout.single_line_item_add, parent, false);
            return new PlaylistOptionsRecyclerAdapter.ViewHolderAdd(view);
        }
        else {
            View view = inflater.inflate(R.layout.double_line_item_with_options, parent, false);
            return new PlaylistOptionsRecyclerAdapter.ViewHolderItem(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder.getItemViewType() == ADD_ITEM_TOKEN) {
            PlaylistOptionsRecyclerAdapter.ViewHolderAdd viewHolder = (PlaylistOptionsRecyclerAdapter.ViewHolderAdd) holder;
            viewHolder.title.setText(R.string.import_playlist_display);
        }
        else {
            if(this.playlists.isEmpty()) {
                return;
            }

            PlaylistOptionsRecyclerAdapter.ViewHolderItem viewHolder = (PlaylistOptionsRecyclerAdapter.ViewHolderItem) holder;
            PlaylistInfo currentPlaylist = this.playlists.get(position - 1);
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

    @Override
    public int getItemCount() {
        return this.playlists.size() + 1;
    }

    public void updateData(ArrayList<PlaylistInfo> playlist) {
        this.playlists.clear();
        this.playlists.addAll(playlist);
        notifyDataSetChanged();
    }

    public void SelectPopupMenuOption(int position, final int optionId) {
        position = position - 1;
        this.recyclerViewPlaylistOptionsListener.SelectMenuOption(position, optionId, null);
    }

    public void ViewHolderClicked(int viewType, int position) {
        this.recyclerViewPlaylistOptionsListener.ButtonClicked(viewType,position - 1);
    }

    public class ViewHolderItem extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView title;
        public TextView other;
        public ImageView options;

        public ViewHolderItem(@NonNull View itemView) {
            super(itemView);

            this.icon = itemView.findViewById(R.id.imageView_item);
            this.title = itemView.findViewById(R.id.textView_item_title);
            this.other = itemView.findViewById(R.id.textView_item_other);
            this.options = itemView.findViewById(R.id.imageView_item_options);

            this.options.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenu = new PopupMenu(context, options);
                    popupMenu.inflate(R.menu.playlist_item_options);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            SelectPopupMenuOption(getLayoutPosition(), menuItem.getItemId());
                            return true;
                        }
                    });
                    popupMenu.show();
                }
            });

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
