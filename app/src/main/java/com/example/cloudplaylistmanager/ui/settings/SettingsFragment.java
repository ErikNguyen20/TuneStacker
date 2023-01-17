package com.example.cloudplaylistmanager.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.cloudplaylistmanager.R;
import com.example.cloudplaylistmanager.Utils.DataManager;
import com.example.cloudplaylistmanager.Utils.SettingsHolder;
import com.example.cloudplaylistmanager.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private SwitchCompat embedButton;
    private SwitchCompat downloadButton;
    private SwitchCompat overrideExportButton;
    private Spinner spinner;
    private ArrayAdapter<CharSequence> adapter;

    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        this.binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = this.binding.getRoot();

        this.embedButton = this.binding.switchEmbed;
        this.downloadButton = this.binding.switchDownload;
        this.overrideExportButton = this.binding.switchExportOverride;
        this.spinner = this.binding.spinner;


        //Sets the click listener for the embed thumbnail switch.
        this.embedButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                DataManager.getInstance().SetSettingsField(SettingsHolder.SettingsFields.EMBED_THUMBNAIL, isChecked);
            }
        });
        //Sets the click listener for the download thumbnail switch.
        this.downloadButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                DataManager.getInstance().SetSettingsField(SettingsHolder.SettingsFields.DOWNLOAD_THUMBNAIL, isChecked);
            }
        });
        //Sets the click listener for the export override switch
        this.overrideExportButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                DataManager.getInstance().SetSettingsField(SettingsHolder.SettingsFields.OVERRIDE_EXPORT, isChecked);
            }
        });

        //Sets the adapter for the spinner (Extension Picker)
        this.adapter = ArrayAdapter.createFromResource(getActivity(), R.array.valid_audio_ext, R.layout.support_simple_spinner_dropdown_item);
        this.adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        this.spinner.setAdapter(this.adapter);
        this.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                String extension = adapterView.getItemAtPosition(pos).toString();
                DataManager.getInstance().SetSettingsField(SettingsHolder.SettingsFields.EXTENSION, extension);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        //Populates the UI with the saved settings data.
        InitializeUI();

        return root;
    }

    /**
     * Initializes the UI based on the settings data stored in DataManager.
     */
    public void InitializeUI() {
        SettingsHolder settings = DataManager.getInstance().GetSettings();
        this.embedButton.setChecked(settings.embedThumbnail);
        this.downloadButton.setChecked(settings.downloadThumbnail);
        this.overrideExportButton.setChecked(settings.overrideExport);

        int position = this.adapter.getPosition(settings.extension);
        this.spinner.setSelection(Math.max(position, 0));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}