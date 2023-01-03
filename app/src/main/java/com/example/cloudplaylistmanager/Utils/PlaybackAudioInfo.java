package com.example.cloudplaylistmanager.Utils;


public class PlaybackAudioInfo {
    public static final String ORIGIN_UPLOAD = "Upload";
    public enum PlaybackMediaType {
        LOCAL,
        STREAM,
        UNKNOWN
    }

    private String title;
    private String author;
    private PlatformCompatUtility.Platform platformOrigin;
    private boolean isPrivate;

    private String origin;
    private PlaybackMediaType audioType;
    private String audioSource; /** Path or URL depending on PlaybackMediaType */
    private PlaybackMediaType thumbnailType;
    private String thumbnailSource; /** Path or URL depending on PlaybackMediaType */

    public PlaybackAudioInfo() {
        this.author = null;
        this.isPrivate = false;
    }

    public PlaybackAudioInfo(String title, String source, PlaybackMediaType type) {
        this.title = title;
        this.author = null;
        this.audioSource = source;
        this.audioType = type;
        this.isPrivate = false;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public PlaybackMediaType getAudioType() {
        return this.audioType;
    }

    public void setAudioType(PlaybackMediaType type) {
        this.audioType = type;
    }

    public String getAudioSource() {
        return this.audioSource;
    }

    public void setAudioSource(String source) {
        this.audioSource = source;
    }

    public String getThumbnailSource() {
        return this.thumbnailSource;
    }

    public void setThumbnailSource(String thumbnailSource) {
        this.thumbnailSource = thumbnailSource;
    }

    public PlaybackMediaType getThumbnailType() {
        return this.thumbnailType;
    }

    public void setThumbnailType(PlaybackMediaType thumbnailType) {
        this.thumbnailType = thumbnailType;
    }

    public boolean getIsPrivate() {
        return this.isPrivate;
    }

    public void setIsPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getOrigin() {
        return this.origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }
}
