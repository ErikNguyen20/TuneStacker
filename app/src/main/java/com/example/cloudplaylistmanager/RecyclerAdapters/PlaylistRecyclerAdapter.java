package com.example.cloudplaylistmanager.RecyclerAdapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;


import java.util.ArrayList;

/**
 * A Recycler View Adapter that displays Playlist Items.
 * This Recycler View can also have an "ADD" button placed on top of all of the items.
 * Extends {@link RecyclerView.Adapter}
 */
public class PlaylistRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int ADD_ITEM_TOKEN = -1; //Token that uniquely identifies the add button
    @StringRes
    private int addButtonResId;
    private Context context;
    private ArrayList<PlaylistInfo> playlist;
    RecyclerViewItemClickedListener itemClickedListener;

    /**
     * Instantiates a new PlaylistRecyclerAdapter object.
     * @param context Context of the application.
     * @param playlist List of playlist items.
     * @param addButtonResId Add Button String Text Resource Id.
     * @param itemClickedListener Listener for UI button presses.
     */
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

    /**
     * Creates a new viewholder based on the viewType.
     * @param parent Parent of the view.
     * @param viewType Type of the view.
     * @return {@link RecyclerView.ViewHolder}
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        if(viewType == ADD_ITEM_TOKEN) {
            //Creates a view with the layout of an add button.
            View view = inflater.inflate(R.layout.single_line_item_add, parent, false);
            return new PlaylistRecyclerAdapter.ViewHolderAdd(view);
        }
        else {
            //Creates a view with the normal playlist item layout.
            View view = inflater.inflate(R.layout.double_line_item, parent, false);
            return new PlaylistRecyclerAdapter.ViewHolderItem(view);
        }
    }

    /**
     * Initializes the data displayed on the item's UI.
     * @param holder Current viewholder.
     * @param position Position of the item in the recycler view.
     */
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

            //Gets the playlist based on the position.
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
            Bitmap bitmap = DataManager.getInstance().GetThumbnailImage(sourceAudio);
            if(bitmap != null) {
                viewHolder.icon.setImageBitmap(bitmap);
            }
            else {
                viewHolder.icon.setImageResource(R.drawable.med_res);
            }
        }
    }

    /**
     * Gets the view type based on the position of the item.
     * @param position Position of the item.
     * @return viewtype of the viewholder.
     */
    @Override
    public int getItemViewType(int position) {
        if(position == 0) {
            return ADD_ITEM_TOKEN;
        }
        else {
            return super.getItemViewType(position - 1);
        }
    }

    /**
     * Interface that calls back the listener for when an item is clicked.
     * @param viewType Viewtype of the item that was clicked.
     * @param position Position of the item that was clicked
     */
    public void ViewHolderClicked(int viewType, int position) {
        this.itemClickedListener.onClicked(viewType,position - 1);
    }

    /**
     * Updates the current recycler view items.
     * @param playlist New playlist data.
     */
    public void updateData(ArrayList<PlaylistInfo> playlist) {
        this.playlist.clear();
        this.playlist.addAll(playlist);
        notifyDataSetChanged();
    }

    /**
     * Gets the number of items in the recycler view.
     * @return Number of items.
     */
    @Override
    public int getItemCount() {
        return this.playlist.size() + 1;
    }

    /**
     * Class that represents the viewholder for a regular item in the Recycler View.
     */
    public class ViewHolderItem extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView title;
        public TextView other;

        public ViewHolderItem(@NonNull View itemView) {
            super(itemView);

            this.icon = itemView.findViewById(R.id.imageView_item);
            this.title = itemView.findViewById(R.id.textView_item_title);
            this.other = itemView.findViewById(R.id.textView_item_other);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ViewHolderClicked(getItemViewType(), getLayoutPosition());
                }
            });
        }
    }

    /**
     * Class that represents the viewholder for an add button in the Recycler View.
     */
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
