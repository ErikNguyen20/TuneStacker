package com.example.cloudplaylistmanager.ui.dashboard.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.cloudplaylistmanager.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImportedFragment extends Fragment {

    public ImportedFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.e("ImportedFragment","OnCreateView");
        return inflater.inflate(R.layout.fragment_imported, container, false);
    }
}