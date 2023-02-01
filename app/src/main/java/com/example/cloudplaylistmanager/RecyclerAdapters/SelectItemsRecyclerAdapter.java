package com.example.cloudplaylistmanager.RecyclerAdapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A Recycler View Adapter that implements a filtered display feature and multi selection.
 * It is important to implement the {@link RecyclerViewSelectItemsListener} properly as to
 * fetch the information for each recycler view item.
 * Extends {@link RecyclerView.Adapter}
 */
public class SelectItemsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<String> listOfUUids; //List of unique identifiers for each item.
    private Context context;
    private boolean isPlaylist;
    private RecyclerViewSelectItemsListener recyclerViewSelectItemsListener;

    /**
     * Instantiates a new SelectItemsRecyclerAdapter object.
     * @param context Context of the application.
     * @param isPlaylist If the recycler view is displaying playlist items or audio items.
     * @param listOfUUids List of unique identifiers that will be used to retrieve the items' information
     * @param recyclerViewSelectItemsListener Listener to fetch item data and handles button presses.
     */
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
        if(this.isPlaylist) {
            //Creates a view with the layout of a playlist item.
            View view = inflater.inflate(R.layout.double_line_item_with_radio_button, parent, false);
            return new SelectItemsRecyclerAdapter.ViewHolderPlaylist(view);
        }
        else {
            //Creates a view with the layout of an audio item.
            View view = inflater.inflate(R.layout.single_line_item_with_radio_button, parent, false);
            return new SelectItemsRecyclerAdapter.ViewHolderAudio(view);
        }
    }

    /**
     * Initializes the data displayed on the item's UI.
     * @param holder Current viewholder.
     * @param position Position of the item in the recycler view.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(this.isPlaylist) {
            SelectItemsRecyclerAdapter.ViewHolderPlaylist viewHolder = (SelectItemsRecyclerAdapter.ViewHolderPlaylist) holder;

            //Gets the unique identifier for the current item.
            String uuidLink = this.listOfUUids.get(position);
            viewHolder.uuidLink = uuidLink;

            //Fetches the information of the current playlist item using the unique identifier.
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
            Bitmap exist = DataManager.getInstance().GetThumbnailImageCache(sourceAudio);
            if(exist != null) {
                viewHolder.icon.setImageBitmap(exist);
            }
            else {
                viewHolder.icon.setImageResource(R.drawable.med_res);
                SelectItemsRecyclerAdapter.AsyncBitmapRequest request = new SelectItemsRecyclerAdapter.AsyncBitmapRequest(viewHolder.icon);
                request.execute(sourceAudio);
            }
        }
        else {
            SelectItemsRecyclerAdapter.ViewHolderAudio viewHolder = (SelectItemsRecyclerAdapter.ViewHolderAudio) holder;

            //Gets the unique identifier for the current item.
            String uuidLink = this.listOfUUids.get(position);
            viewHolder.uuidLink = uuidLink;

            //Fetches the information of the current audio item using the unique identifier.
            Pair<PlaybackAudioInfo, Boolean> audioInfo = this.recyclerViewSelectItemsListener.FetchAudioInformation(uuidLink);
            if(audioInfo == null) {
                return;
            }
            viewHolder.checkBox.setChecked(audioInfo.second);
            viewHolder.title.setText(audioInfo.first.getTitle());

            //Sets the icon of the audio item.
            Bitmap exist = DataManager.getInstance().GetThumbnailImageCache(audioInfo.first);
            if(exist != null) {
                viewHolder.icon.setImageBitmap(exist);
            }
            else {
                viewHolder.icon.setImageResource(R.drawable.med_res);
                SelectItemsRecyclerAdapter.AsyncBitmapRequest request = new SelectItemsRecyclerAdapter.AsyncBitmapRequest(viewHolder.icon);
                request.execute(audioInfo.first);
            }
        }
    }

    /**
     * Sets a list of filtered identifier results that will be displayed on the recycler view.
     * @param listOfUUidsNew Filtered results.
     */
    public void FilterResults(ArrayList<String> listOfUUidsNew) {
        this.listOfUUids.clear();
        this.listOfUUids.addAll(listOfUUidsNew);
        notifyDataSetChanged();
    }

    /**
     * Calls back to the listener to handle the item that was clicked.
     * @param uuid Unique Identifier for the item that was selected.
     */
    public void ItemSelected(String uuid) {
        this.recyclerViewSelectItemsListener.ButtonClicked(uuid);
    }

    /**
     * Gets the number of items in the recycler view.
     * @return Number of items.
     */
    @Override
    public int getItemCount() {
        return this.listOfUUids.size();
    }

    /**
     * Class that represents the viewholder for an audio item in the Recycler View.
     */
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

    /**
     * Class that represents the viewholder for a playlist item in the Recycler View.
     */
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


