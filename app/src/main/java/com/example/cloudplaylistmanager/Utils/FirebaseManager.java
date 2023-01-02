package com.example.cloudplaylistmanager.Utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

public class FirebaseManager {
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

    
}
