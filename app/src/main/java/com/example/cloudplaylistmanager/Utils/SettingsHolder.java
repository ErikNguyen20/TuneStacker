package com.example.cloudplaylistmanager.Utils;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

/**
 * Settings class that implements {@link Serializable}.
 * It is simply a collection that holds the settings of the
 * application.
 */
public class SettingsHolder implements Serializable {
    public static final String DEFAULT_EXTENSION = "opus";
    public static final boolean DEFAULT_EMBED_THUMBNAIL = true;
    public static final boolean DEFAULT_DOWNLOAD_THUMBNAIL = true;

    public enum SettingsFields {
        EMBED_THUMBNAIL,
        DOWNLOAD_THUMBNAIL,
        EXTENSION
    }

    @Expose
    public boolean embedThumbnail;
    @Expose
    public boolean downloadThumbnail;
    @Expose
    public String extension;

    /**
     * Instantiates a new SettingsHolder object.
     */
    public SettingsHolder() {
        this.embedThumbnail = DEFAULT_EMBED_THUMBNAIL;
        this.downloadThumbnail = DEFAULT_DOWNLOAD_THUMBNAIL;
        this.extension = DEFAULT_EXTENSION;
    }

    /**
     * Sets a field in the settings object to a specified value.
     * @param field Field of the settings item.
     * @param value New value of the field.
     */
    public void changeSettingsItem(SettingsFields field, Object value) {
        try {
            switch (field) {
                case EXTENSION:
                    this.extension = (String) value;
                    break;
                case EMBED_THUMBNAIL:
                    this.embedThumbnail = (boolean) value;
                    break;
                case DOWNLOAD_THUMBNAIL:
                    this.downloadThumbnail = (boolean) value;
                    break;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
