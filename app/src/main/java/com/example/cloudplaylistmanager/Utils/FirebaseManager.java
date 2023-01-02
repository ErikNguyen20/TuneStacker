package com.example.cloudplaylistmanager.Utils;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

interface UploadThumbnailListener {
    void onComplete(String downloadUrl);
    void onError(String message);
}

/**
 * Database Structure:
 * Playlists{
 *     String title,                         - Field may be null
 *     String creator,                       - Field may be null
 *     boolean nested,                       - Imported playlist are not nested, created playlists are.
 *     String platform,                      - Field only exists for imported playlists.
 *     String source,                        - Field only exists for imported playlists. Link of the playlist (like youtubeList)
 *     Array<SongsReferences> inserted       - Manually added songs from local files or just a single video link.
 *     Array<PlaylistsReferences> imported   - References to imported playlists (Only exists in nested playlists)
 * }
 * User{
 *     Array<PlaylistsReferences> playlists  -References to all playlists.
 * }
 */
public class FirebaseManager {
    private static final String LOG_TAG = "FirebaseManager";
    public static final String DEFAULT_THUMBNAIL_LOW = "https://firebasestorage.googleapis.com/v0/b/cloud-playlist-manager.appspot.com/o/thumbnails%2FlowRes.jpg?alt=media&token=95028015-4b9b-40ef-b6dc-0bee3934c902";
    public static final String DEFAULT_THUMBNAIL_MED = "https://firebasestorage.googleapis.com/v0/b/cloud-playlist-manager.appspot.com/o/thumbnails%2FmedRes.jpg?alt=media&token=aaf02120-09b0-48c5-89da-2cf6a3b9486d";
    public static final String DEFAULT_THUMBNAIL_HIGH = "https://firebasestorage.googleapis.com/v0/b/cloud-playlist-manager.appspot.com/o/thumbnails%2FhighRes.jpg?alt=media&token=7a21caac-769b-4dcb-90d9-90ad83bf375c";


    private static FirebaseManager instance = null;
    private FirebaseAuth authentication;
    private FirebaseStorage storage;
    private FirebaseFirestore database;

    private FirebaseManager() {
        this.authentication = FirebaseAuth.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.database = FirebaseFirestore.getInstance();
    }

    public static FirebaseManager getInstance() {
        if(instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public void UploadVideo() {

    }

    public void UploadThumbnail(Uri uri, UploadThumbnailListener uploadThumbnailListener) {
        Log.e("TEST",uri.getPath());
        Log.e("TEST",uri.getLastPathSegment());
        Log.e("TEST",uri.toString());

        String extension = '.' + uri.getLastPathSegment().split("[.]")[1];
        String hashedName = DataManager.AudioTitleToHash(uri.getLastPathSegment());

        StorageReference fileReference = this.storage.getReference().child("thumbnails").child(hashedName + extension);
        fileReference.putFile(uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String url = uri.toString();
                        uploadThumbnailListener.onComplete(url);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
                        e.printStackTrace();
                        uploadThumbnailListener.onError("Failed to get Download URL");
                    }
                });
            }
        });
    }

    public void AddPlaylistToField() {
        /*
            DocumentReference washingtonRef = db.collection("cities").document("DC");

            // Atomically add a new region to the "regions" array field.
            washingtonRef.update("regions", FieldValue.arrayUnion("greater_virginia"));

            // Atomically remove a region from the "regions" array field.
            washingtonRef.update("regions", FieldValue.arrayRemove("east_coast"));
         */
    }

    public void InitializeUserData() {
        if(this.authentication.getCurrentUser() == null) {
            return;
        }
        String userId = this.authentication.getCurrentUser().getUid();

        Map<String, Object> initialPlaylistData = new HashMap<>();
        initialPlaylistData.put("title","My First Playlist");
        initialPlaylistData.put("creator",userId);
        initialPlaylistData.put("nested",true);
        initialPlaylistData.put("inserted",null);
        initialPlaylistData.put("imported",null);

        DocumentReference newPlaylist = this.database.collection("playlists").document();
        newPlaylist.set(initialPlaylistData).addOnFailureListener(e -> {
            Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
        });

        Map<String, Object> initialUserData = new HashMap<>();
        initialUserData.put("playlists", Collections.singletonList(newPlaylist));

        this.database.collection("users").document(userId).set(initialUserData).addOnFailureListener(e -> {
            Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
        });
    }
}
