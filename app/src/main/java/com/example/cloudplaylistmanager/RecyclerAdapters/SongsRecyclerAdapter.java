package com.example.cloudplaylistmanager.RecyclerAdapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A Recycler View Adapter that displays Audio Items.
 * This Recycler View can also have an "ADD" button placed on top of all of the items.
 * Extends {@link RecyclerView.Adapter}
 */
public class SongsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int ADD_ITEM_TOKEN = -1; //Token that uniquely identifies the add button

    private Context context;
    ArrayList<PlaybackAudioInfo> audios;
    boolean addButtonIncluded;
    RecyclerViewItemClickedListener itemClickedListener;

    /**
     * Instantiates a new SongsRecyclerAdapter object.
     * @param context Context of the application.
     * @param playlist Playlist item that holds the list of audios.
     * @param addButtonIncluded If an add button should be included in the recycler view.
     * @param itemClickedListener Listener for UI button presses.
     */
    public SongsRecyclerAdapter(Context context, PlaylistInfo playlist, boolean addButtonIncluded,
                                RecyclerViewItemClickedListener itemClickedListener) {
        this.context = context;
        this.audios = new ArrayList<>();
        this.audios.addAll(playlist.getAllVideos());
        this.addButtonIncluded = addButtonIncluded;
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
            return new SongsRecyclerAdapter.ViewHolderAdd(view);
        }
        else {
            //Creates a view with the normal audio item layout.
            View view = inflater.inflate(R.layout.single_line_item, parent, false);
            return new SongsRecyclerAdapter.ViewHolderItem(view);
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

            //Gets the image of the audio.
            Bitmap exist = DataManager.getInstance().GetThumbnailImageCache(audio);
            if(exist != null) {
                viewHolder.icon.setImageBitmap(exist);
            }
            else {
                viewHolder.icon.setImageResource(R.drawable.med_res);
                AsyncBitmapRequest request = new AsyncBitmapRequest(viewHolder.icon);
                request.execute(audio);
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
        if(this.addButtonIncluded && position == 0) {
            return ADD_ITEM_TOKEN;
        }
        else {
            return super.getItemViewType(position);
        }
    }

    /**
     * Interface that calls back the listener for when an item is clicked.
     * @param viewType Viewtype of the item that was clicked.
     * @param position Position of the item that was clicked
     */
    public void ViewHolderClicked(int viewType, int position) {
        if(this.itemClickedListener != null) {
            this.itemClickedListener.onClicked(viewType,this.addButtonIncluded ? position - 1 : position);
        }
    }

    /**
     * Updates the current recycler view items.
     * @param playlist New playlist data.
     */
    public void updateData(PlaylistInfo playlist) {
        this.audios.clear();
        this.audios.addAll(playlist.getAllVideos());
        notifyDataSetChanged();
    }

    /**
     * Gets the number of items in the recycler view.
     * @return Number of items.
     */
    @Override
    public int getItemCount() {
        return this.addButtonIncluded ? this.audios.size() + 1 : this.audios.size();
    }

    /**
     * Class that represents the viewholder for a regular item in the Recycler View.
     */
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

    /**
     * Class that represents the viewholder for an add button in the Recycler View.
     */
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

    /**
     * AsyncTask class that asynchronously processes/fetches the bitmap of an audio.
     * Extends {@link AsyncTask}
     */
    private static class AsyncBitmapRequest extends AsyncTask<PlaybackAudioInfo, Integer, Bitmap> {
        private final WeakReference<ImageView> viewReference;

        public AsyncBitmapRequest(ImageView view) {
            this.viewReference = new WeakReference<>(view);
        }

        @Override
        protected Bitmap doInBackground(PlaybackAudioInfo... params) {
            PlaybackAudioInfo audio = params[0];
            return DataManager.getInstance().GetThumbnailImage(audio);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(this.viewReference.get() != null && bitmap != null) {
                ImageView view = this.viewReference.get();
                view.setImageBitmap(bitmap);
            }
        }
    }
}
