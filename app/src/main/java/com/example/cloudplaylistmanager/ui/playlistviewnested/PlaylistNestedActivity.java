package com.example.cloudplaylistmanager.ui.playlistviewnested;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.cloudplaylistmanager.MusicPlayer.MediaPlayerActivity;
import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.PlaybackAudioInfo;
import com.example.cloudplaylistmanager.Utils.PlaylistInfo;
import com.google.android.material.tabs.TabLayout;

public class PlaylistNestedActivity extends AppCompatActivity {
    public static final String SERIALIZE_TAG = "playlist_data";
    public static final String UUID_KEY_TAG = "playlist_uuid";

    private ImageView icon;
    private TextView title;
    private TextView subtitle;
    private Button playAll;
    private Button shufflePlay;
    private ViewPager2 viewPager2;
    private TabLayout tabLayout;
    private PlaylistNestedViewPagerAdapter adapter;

    private PlaylistNestedViewModel playlistNestedViewModel;
    private PlaylistInfo playlistInfo;
    private String uuidKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_nested);

        //Sets action bar style.
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_new_24);
            actionBar.setTitle("Back");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        this.playlistInfo = (PlaylistInfo) getIntent().getSerializableExtra(SERIALIZE_TAG);
        this.uuidKey = getIntent().getStringExtra(UUID_KEY_TAG);
        if(this.playlistInfo == null) {
            this.finish();
            return;
        }

        this.icon = findViewById(R.id.imageView_playlist);
        this.title = findViewById(R.id.playlist_title);
        this.subtitle = findViewById(R.id.playlist_subtitle);
        this.playAll = findViewById(R.id.button_playall);
        this.shufflePlay = findViewById(R.id.button_shuffleplay);
        this.viewPager2 = findViewById(R.id.viewPager);
        this.tabLayout = findViewById(R.id.tabLayout);

        UpdateUI();

        this.playAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playlistInfo == null || playlistInfo.getAllVideos().isEmpty()) {
                    return;
                }
                Intent intent = new Intent(PlaylistNestedActivity.this, MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,playlistInfo);
                startActivity(intent);
            }
        });
        this.shufflePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playlistInfo == null || playlistInfo.getAllVideos().isEmpty()) {
                    return;
                }
                Intent intent = new Intent(PlaylistNestedActivity.this, MediaPlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(MediaPlayerActivity.SERIALIZE_TAG,playlistInfo);
                intent.putExtra(MediaPlayerActivity.SHUFFLED_TAG,true);
                startActivity(intent);
            }
        });


        this.playlistNestedViewModel = new ViewModelProvider(this).get(PlaylistNestedViewModel.class);
        this.playlistNestedViewModel.updateData(this.uuidKey);

        this.adapter = new PlaylistNestedViewPagerAdapter(getSupportFragmentManager(),getLifecycle());
        this.viewPager2.setAdapter(this.adapter);

        this.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        this.viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        });

        this.playlistNestedViewModel.getPlaylistData().observe(this, new Observer<PlaylistInfo>() {
            @Override
            public void onChanged(PlaylistInfo playlist) {
                playlistInfo = playlist;
                UpdateUI();
            }
        });
    }

    public void UpdateUI() {
        if(this.playlistInfo == null) {
            return;
        }
        this.title.setText(this.playlistInfo.getTitle());
        this.subtitle.setText(new String(" " + this.playlistInfo.getAllVideos().size() + " songs"));
        if(!this.playlistInfo.getAllVideos().isEmpty()) {
            PlaybackAudioInfo sourceAudio = this.playlistInfo.getAllVideos().iterator().next();
            if(sourceAudio.getThumbnailType() == PlaybackAudioInfo.PlaybackMediaType.LOCAL) {
                Bitmap bitmap = BitmapFactory.decodeFile(sourceAudio.getThumbnailSource());
                if (bitmap != null) {
                    this.icon.setImageBitmap(bitmap);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.nested_playlist_options,menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
        } else if(id == R.id.rename_option) {
            //DataManager.getInstance().RenamePlaylist(this.uuidKey,"NewName");
            this.playlistNestedViewModel.updateData(this.uuidKey);
        } else if(id == R.id.export_option) {

        } else if(id == R.id.sync_option) {

        } else if(id == R.id.backup_option) {

        } else if(id == R.id.delete_option) {
            //DataManager.getInstance().RemovePlaylist(this.uuidKey);
            this.finish();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.playlistNestedViewModel.updateData(this.uuidKey);
    }
}