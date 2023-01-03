package com.example.cloudplaylistmanager.Utils;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

interface UploadListener {
    void onComplete(String downloadUrl);
    void onError(int errorCode, String message);
}
interface FetchListener {
    void onComplete(DocumentSnapshot documentSnapshot);
    void onError(int errorCode, String message);
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
 * Song{
 *     String audioUrl,
 *     String author,
 *     String origin,
 *     String thumbnailUrl,
 *     String title,
 *     String externalSource{pcloud,etc...}   - If external source exists, then the audioUrl will link to a reference to the audio source in that external cloud storage.
 * }
 */
public class FirebaseManager {
    private static final String LOG_TAG = "FirebaseManager";
    public static final String DEFAULT_THUMBNAIL_LOW = "https://firebasestorage.googleapis.com/v0/b/cloud-playlist-manager.appspot.com/o/thumbnails%2FlowRes.jpg?alt=media&token=95028015-4b9b-40ef-b6dc-0bee3934c902";
    public static final String DEFAULT_THUMBNAIL_MED = "https://firebasestorage.googleapis.com/v0/b/cloud-playlist-manager.appspot.com/o/thumbnails%2FmedRes.jpg?alt=media&token=aaf02120-09b0-48c5-89da-2cf6a3b9486d";
    public static final String DEFAULT_THUMBNAIL_HIGH = "https://firebasestorage.googleapis.com/v0/b/cloud-playlist-manager.appspot.com/o/thumbnails%2FhighRes.jpg?alt=media&token=7a21caac-769b-4dcb-90d9-90ad83bf375c";


    private static FirebaseManager instance = null;
    private HashMap<String, Object> userMetadataCache;
    private FirebaseAuth authentication;
    private FirebaseStorage storage;
    private FirebaseFirestore database;

    private FirebaseManager() {
        this.authentication = FirebaseAuth.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.database = FirebaseFirestore.getInstance();
        this.userMetadataCache = null;
    }

    public static FirebaseManager getInstance() {
        if(instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }


    /**
     * Uploads a file into the Cloud Firebase Storage.
     * It is required to implement {@link UploadListener} to obtain the
     * result of this call and to catch potential errors.
     * https://firebase.google.com/docs/storage/android/handle-errors
     * @param file File Path of the thumbnail source.
     * @param uploadListener Listener used to get the results/errors of this call.
     */
    public void UploadToFirebase(File file, StorageReference fileReference, UploadListener uploadListener) {
        Uri uri = Uri.fromFile(file);
        fileReference.putFile(uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()) {
                    fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String url = uri.toString();
                            uploadListener.onComplete(url);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            StorageException exception = (StorageException) e;
                            String errorMsg = (exception.getMessage() != null) ? exception.getMessage() : "Error when fetching the Download URL.";
                            uploadListener.onError(exception.getErrorCode(), errorMsg);
                            Log.e(LOG_TAG, errorMsg);
                            e.printStackTrace();
                        }
                    });
                }
                else {
                    if(task.getException() != null) {
                        StorageException exception = (StorageException) task.getException();
                        String errorMsg = (exception.getMessage() != null) ? exception.getMessage() : "Error on upload.";
                        uploadListener.onError(exception.getErrorCode(),errorMsg);
                        Log.e(LOG_TAG, errorMsg);
                        exception.printStackTrace();
                    }
                    else {
                        uploadListener.onError(StorageException.ERROR_UNKNOWN,"Error on upload.");
                        Log.e(LOG_TAG,"Error on upload.");
                    }
                }
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

    /**
     * Fetches a document from the database using a specific path and source.
     * It is required to implement {@link FetchListener} to obtain the
     * result of this call and to catch potential errors.
     * https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/FirebaseFirestoreException.Code
     * @param pathToDocument Path to the document. Ex: playlists/Czpf5cvQ2gNWMKvcACbr
     * @param source Source of the get (Source.DEFAULT, Source.CACHE, Source.SERVER)
     * @param fetchListener Listener used to get the results/errors of this call.
     */
    public void GetDocumentFromDatabase(String pathToDocument, Source source, FetchListener fetchListener) {
        DocumentReference reference = database.document(pathToDocument);
        reference.get(source).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if(doc.exists()) {
                        fetchListener.onComplete(doc);
                    }
                    else {
                        fetchListener.onError(FirebaseFirestoreException.Code.NOT_FOUND.value(), "Document does not exist.");
                        Log.e(LOG_TAG, "Document does not exist.");
                    }
                }
                else {
                    if(task.getException() != null) {
                        FirebaseFirestoreException exception = (FirebaseFirestoreException) task.getException();
                        String errorMsg = (exception.getMessage() != null) ? exception.getMessage() : "Failed to fetch Document from Database.";
                        fetchListener.onError(exception.getCode().value(), errorMsg);
                        Log.e(LOG_TAG, errorMsg);
                        exception.printStackTrace();
                    }
                    else {
                        fetchListener.onError(FirebaseFirestoreException.Code.UNKNOWN.value(), "Failed to fetch Document from Database.");
                        Log.e(LOG_TAG, "Failed to fetch Document from Database.");
                    }
                }
            }
        });
    }

    /**
     * Synchronously checks to see if the song already exists in the database.
     * This method should not be called from the main thread.
     * @param title Title of the song.
     * @param origin Origin of the song.
     * @return If the song exists in the database.
     */
    public boolean CheckIfSongExistsInDatabase(String title, String origin) {
        //Queries all of the songs to match the title.
        final CountDownLatch latch1 = new CountDownLatch(1);
        StringBuilder result1 = new StringBuilder();
        this.database.collection("metadata")
                .whereEqualTo("title",title).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    if(!task.getResult().isEmpty()) {
                        result1.append('t');
                    }
                }
                else {
                    if(task.getException() != null) {
                        FirebaseFirestoreException exception = (FirebaseFirestoreException) task.getException();
                        Log.e(LOG_TAG, (exception.getMessage() != null) ? exception.getMessage() : "Error on Checking if Song Exists.");
                        exception.printStackTrace();
                    }
                    else {
                        Log.e(LOG_TAG,"Error on Checking if Song Exists.");
                    }
                }
                latch1.countDown();
            }
        });
        try {
            latch1.await();
        } catch(InterruptedException e) {
            Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "Latch was interrupted.");
            e.printStackTrace();
        }

        //If there was no match for the titles, query for matching origin urls.
        if(!origin.equals(PlaybackAudioInfo.ORIGIN_UPLOAD) && result1.toString().isEmpty()) {
            final CountDownLatch latch2 = new CountDownLatch(1);
            StringBuilder result2 = new StringBuilder();
            this.database.collection("metadata")
                    .whereEqualTo("origin",origin).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if(task.isSuccessful()) {
                        if(!task.getResult().isEmpty()) {
                            result2.append('t');
                        }
                    }
                    else {
                        if(task.getException() != null) {
                            FirebaseFirestoreException exception = (FirebaseFirestoreException) task.getException();
                            Log.e(LOG_TAG, (exception.getMessage() != null) ? exception.getMessage() : "Error on Checking if Song Exists.");
                            exception.printStackTrace();
                        }
                        else {
                            Log.e(LOG_TAG,"Error on Checking if Song Exists.");
                        }
                    }
                    latch2.countDown();
                }
            });
            try {
                latch2.await();
            } catch(InterruptedException e) {
                Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "Latch was interrupted.");
                e.printStackTrace();
            }

            return !result2.toString().isEmpty();
        }
        else {
            return !result1.toString().isEmpty();
        }
    }

    /**
     * Creates and adds the song's metadata onto the database.
     * @param audio Audio containing all necessary metadata.
     * @param external (Optional) Indicates that this song is stored on an external cloud database.
     *                 If this is true, then the audio.getAudioSource() should contain a reference to that song in the external database.
     */
    public DocumentReference CreateNewSongMetadata(PlaybackAudioInfo audio, String external) {
        Map<String, Object> songMetadata = new HashMap<>();
        songMetadata.put("title",audio.getTitle());
        songMetadata.put("author",audio.getAuthor());
        songMetadata.put("audioUrl",audio.getAudioSource());
        songMetadata.put("thumbnailUrl",audio.getThumbnailSource());
        songMetadata.put("origin",audio.getOrigin());
        if(external != null && !external.isEmpty()) {
            songMetadata.put("externalSource",external);
        }

        DocumentReference newSongMetadata = this.database.collection("metadata").document();
        newSongMetadata.set(songMetadata).addOnFailureListener(e -> {
            Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
        });

        return newSongMetadata;
    }

    /**
     * Creates a new playlist and places it in the database.
     * @param title Title of the playlist.
     * @param nested Whether or not this playlist is a nested playlist.
     * @param platform If the playlist is not a nested playlist, specify a platform from {@link PlatformCompatUtility}.
     * @param source If the playlist is not a nested playlist, specify the source (playlist url link like from youtube.)
     */
    public DocumentReference CreateNewPlaylist(String title, boolean nested, String platform, String source) {
        if(this.authentication.getCurrentUser() == null) {
            return null; //Error Handling
        }
        String userId = this.authentication.getCurrentUser().getUid();

        if(title == null) {
            title = "New Playlist";
        }

        Map<String, Object> initialPlaylistData = new HashMap<>();
        initialPlaylistData.put("title",title);
        initialPlaylistData.put("creator",userId);
        initialPlaylistData.put("nested",nested);
        initialPlaylistData.put("inserted",null);
        if(nested) {
            initialPlaylistData.put("imported",null);
        }
        else {
            initialPlaylistData.put("platform",platform);
            initialPlaylistData.put("source",source);
        }

        DocumentReference newPlaylist = this.database.collection("playlists").document();
        newPlaylist.set(initialPlaylistData).addOnFailureListener(e -> {
            Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
        });

        return newPlaylist;
    }

    //TODO - Modify this to work with different accounts (LOGOUT)
    /**
     * Fetches and stores the current user's metadata.
     * Use this when the app is initialized.
     */
    public void UpdateUserMetadata() {
        if(this.authentication.getCurrentUser() == null) {
            return; //Error handling
        }
        if(this.userMetadataCache != null) {
            return;
        }
        String userId = this.authentication.getCurrentUser().getUid();

        this.database.collection("users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if(doc.exists()) {
                        userMetadataCache = (HashMap<String, Object>) task.getResult().getData();
                    }
                    else {
                        Log.e(LOG_TAG, "Metadata Document does not exist.");
                    }
                }
                else {
                    if(task.getException() != null) {
                        FirebaseFirestoreException exception = (FirebaseFirestoreException) task.getException();
                        Log.e(LOG_TAG, (exception.getMessage() != null) ? exception.getMessage() : "Error on Update Metadata.");
                        exception.printStackTrace();
                    }
                    else {
                        Log.e(LOG_TAG,"Error on Update Metadata.");
                    }
                }
            }
        });
    }

    /**
     * When a user successfully registers on the application, initialize the
     * user's data.
     */
    public void InitializeUserData() {
        if(this.authentication.getCurrentUser() == null) {
            return; //Error handling
        }
        String userId = this.authentication.getCurrentUser().getUid();

        DocumentReference newPlaylist = CreateNewPlaylist("My First Playlist", true,null,null);
        if(newPlaylist == null) {
            return; //Error Handling
        }
        Map<String, Object> initialUserData = new HashMap<>();
        initialUserData.put("playlists", Collections.singletonList(newPlaylist));
        initialUserData.put("cloudLocation","firebase");

        this.database.collection("users").document(userId).set(initialUserData).addOnFailureListener(e -> {
            Log.e(LOG_TAG, (e.getMessage() != null) ? e.getMessage() : "An Error has Occurred");
            e.printStackTrace();
        });
    }

    public StorageReference GetStorageReferenceToThumbnails() {
        return this.storage.getReference().child("thumbnails");
    }

    public StorageReference GetStorageReferenceToAudio() {
        return this.storage.getReference().child("audio");
    }

}
