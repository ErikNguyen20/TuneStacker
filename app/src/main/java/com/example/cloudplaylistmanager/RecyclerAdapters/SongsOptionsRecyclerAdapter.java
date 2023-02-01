package com.example.cloudplaylistmanager.RecyclerAdapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
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
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A Recycler View Adapter that displays Audio Items with an ellipsis for menu options.
 * This Recycler View can also have an "ADD" button placed on top of all of the items.
 * Extends {@link RecyclerView.Adapter}
 */
public class SongsOptionsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    public static final int ADD_ITEM_TOKEN = -1; //Token that uniquely identifies the add button

    private Context context;
    private ArrayList<PlaybackAudioInfo> audios;
    private boolean addButtonIncluded;
    private RecyclerViewOptionsListener recyclerViewSongsOptionsListener;
    private int menuOption;

    /**
     * Instantiates a new PlaylistOptionsRecyclerAdapter object.
     * @param context Context of the application.
     * @param playlist Playlist item that holds the list of audios.
     * @param addButtonIncluded If an add button should be included in the recycler view.
     * @param menuOption Menu Resource Item Id.
     * @param recyclerViewSongsOptionsListener Listener for UI button/menu option presses.
     */
    public SongsOptionsRecyclerAdapter(Context context, PlaylistInfo playlist, boolean addButtonIncluded, int menuOption,
                                       RecyclerViewOptionsListener recyclerViewSongsOptionsListener) {
        this.context = context;
        this.audios = new ArrayList<>();
        this.audios.addAll(playlist.getAllVideos());
        this.addButtonIncluded = addButtonIncluded;
        this.menuOption = menuOption;
        this.recyclerViewSongsOptionsListener = recyclerViewSongsOptionsListener;
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
            return new SongsOptionsRecyclerAdapter.ViewHolderAdd(view);
        }
        else {
            //Creates a view with the normal audio item layout.
            View view = inflater.inflate(R.layout.single_line_item_with_options, parent, false);
            return new SongsOptionsRecyclerAdapter.ViewHolderItem(view);
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
            SongsOptionsRecyclerAdapter.ViewHolderAdd viewHolder = (SongsOptionsRecyclerAdapter.ViewHolderAdd) holder;
            viewHolder.title.setText(R.string.add_song_display);
        }
        else {
            if(this.audios.isEmpty()) {
                return;
            }
            SongsOptionsRecyclerAdapter.ViewHolderItem viewHolder = (SongsOptionsRecyclerAdapter.ViewHolderItem) holder;

            //Populate the view with information on the UI.
            PlaybackAudioInfo audio = this.audios.get(this.addButtonIncluded ? position - 1 : position);

            viewHolder.title.setText(audio.getTitle());

            //Sets image of the audio.
            Bitmap exist = DataManager.getInstance().GetThumbnailImageCache(audio);
            if(exist != null) {
                viewHolder.icon.setImageBitmap(exist);
            }
            else {
                viewHolder.icon.setImageResource(R.drawable.med_res);
                SongsOptionsRecyclerAdapter.AsyncBitmapRequest request = new SongsOptionsRecyclerAdapter.AsyncBitmapRequest(viewHolder.icon);
                request.execute(audio);
            }
        }
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
     * Updates the current recycler view items.
     * @param playlist New playlist data.
     */
    public void updateData(PlaylistInfo playlist) {
        this.audios.clear();
        this.audios.addAll(playlist.getAllVideos());
        notifyDataSetChanged();
    }

    /**
     * Returns the list of audio items in the recycler view.
     * @return List of audio items.
     */
    public ArrayList<PlaybackAudioInfo> getAudios() {
        return this.audios;
    }

    /**
     * Swaps an item in the recycler view.
     * @param fromPosition Original position of the item.
     * @param toPosition New position of the item.
     */
    public void swapDataPoints(int fromPosition, int toPosition) {
        Collections.swap(this.audios,
                (this.addButtonIncluded ? fromPosition - 1 : fromPosition) % this.audios.size(),
                (this.addButtonIncluded ? toPosition - 1 : toPosition) % this.audios.size());
        notifyItemMoved(fromPosition, toPosition);
    }

    /**
     * Interface that calls back the listener for when an option is selected.
     * @param position Position of the item that was clicked
     * @param optionId Menu item option id.
     */
    public void SelectPopupMenuOption(int position, final int optionId) {
        position = this.addButtonIncluded ? position - 1 : position;
        this.recyclerViewSongsOptionsListener.SelectMenuOption(position, optionId, this.audios.get(position).getTitle());
    }

    /**
     * Interface that calls back the listener for when an item is clicked.
     * @param viewType Viewtype of the item that was clicked.
     * @param position Position of the item that was clicked
     */
    public void ButtonClicked(int viewType, int position) {
        this.recyclerViewSongsOptionsListener.ButtonClicked(viewType, this.addButtonIncluded ? position - 1 : position);
    }

    /**
     * Class that represents the viewholder for a regular item in the Recycler View.
     */
    public class ViewHolderItem extends RecyclerView.ViewHolder {

        public ImageView icon;
        public TextView title;
        public ImageView options;

        public ViewHolderItem(@NonNull View itemView) {
            super(itemView);

            this.icon = itemView.findViewById(R.id.imageView_item);
            this.title = itemView.findViewById(R.id.textView_item_title);
            this.options = itemView.findViewById(R.id.imageView_item_options);

            //Setup menu options popup.
            this.options.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenu = new PopupMenu(context, options);
                    popupMenu.inflate(menuOption);
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

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ButtonClicked(getItemViewType(),getLayoutPosition());
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
                    ButtonClicked(getItemViewType(), getLayoutPosition());
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
