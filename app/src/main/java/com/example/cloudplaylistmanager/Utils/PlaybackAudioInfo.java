package com.example.cloudplaylistmanager.Utils;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

/**
 * A class that represents an audio object, containing all the necessary audio
 * data. This class is also implements {@link Serializable}.
 * Fields with @Expose indicate that the Gson serializable can touch those fields.
 */
public class PlaybackAudioInfo implements Serializable {
    public static final String ORIGIN_UPLOAD = "Upload";
    public static final String DEFAULT_TITLE = "Unknown";

    //Enum to indicate the playback media type.
    public enum PlaybackMediaType {
        LOCAL,  //Indicates that the source is a file path
        STREAM, //Indicates that the source is a media stream.
        UNKNOWN //Indicates that the source is unknown.
    }

    @Expose
    private String title;
    @Expose
    private String author;
    @Expose
    private String origin;
    @Expose
    private PlaybackMediaType audioType;
    @Expose
    private String audioSource;
    @Expose
    private PlaybackMediaType thumbnailType;
    @Expose
    private String thumbnailSource;


    /**
     * Instantiates a new PlaybackAudioInfo object.
     */
    public PlaybackAudioInfo() {
        this.author = null;
        this.origin = null;
        this.title = DEFAULT_TITLE;
        this.audioType = PlaybackMediaType.UNKNOWN;
        this.thumbnailType = PlaybackMediaType.UNKNOWN;
        this.thumbnailSource = null;
        this.audioSource = null;
    }

    /**
     * Returns the title of the audio.
     * @return Title of the audio.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Sets the title of the audio.
     * @param title Title of the audio.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the author of the audio.
     * @return Author of the audio.
     */
    public String getAuthor() {
        return this.author;
    }

    /**
     * Sets the author of the audio.
     * @param author Author of the audio.
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Gets the playback media type of the audio source.
     * @return Playback Media Type of the audio source.
     */
    public PlaybackMediaType getAudioType() {
        return this.audioType;
    }

    /**
     * Sets the playback media type of the audio source.
     * @param type Playback Media Type of the audio source.
     */
    public void setAudioType(PlaybackMediaType type) {
        this.audioType = type;
    }

    /**
     * Returns the file path or the url of the audio source.
     * @return Path or url of the audio source.
     */
    public String getAudioSource() {
        return this.audioSource;
    }

    /**
     * Sets the file path or the url of the audio source.
     * @param source Path or url of the audio source.
     */
    public void setAudioSource(String source) {
        this.audioSource = source;
    }

    /**
     * Returns the file path or the url of the thumbnail source.
     * @return Path or url of the thumbnail source.
     */
    public String getThumbnailSource() {
        return this.thumbnailSource;
    }

    /**
     * Sets the file path or the url of the thumbnail source.
     * @param thumbnailSource Path or url of the thumbnail source.
     */
    public void setThumbnailSource(String thumbnailSource) {
        this.thumbnailSource = thumbnailSource;
    }

    /**
     * Returns the Playback Media Type of the thumbnail source.
     * @return Playback Media Type of the thumbnail source.
     */
    public PlaybackMediaType getThumbnailType() {
        return this.thumbnailType;
    }

    /** Sets the Playback Media Type of the thumbnail source.
     * @param thumbnailType Playback Media Type of the thumbnail source.
     */
    public void setThumbnailType(PlaybackMediaType thumbnailType) {
        this.thumbnailType = thumbnailType;
    }

    /**
     * Returns the Url link origin of the audio. This value will not be a url if it is
     * uploaded locally.
     * @return Url link origin of the audio.
     */
    public String getOrigin() {
        return this.origin;
    }

    /** Returns the Url link origin of the audio. This value will not be a url if it is
     * uploaded locally.
     * @param origin Url link origin of the audio.
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }


    public boolean equals(Object o) {
        if(o instanceof PlaybackAudioInfo) {
            PlaybackAudioInfo audioObject = (PlaybackAudioInfo) o;
            return this.title.equals(audioObject.title);
        }
        return false;
    }

    public int hashCode() {
        return this.title.hashCode();
    }

}
