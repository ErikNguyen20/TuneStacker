package com.example.cloudplaylistmanager.Utils;


public class PlaybackAudioInfo {
    public enum PlaybackMediaType {
        LOCAL,
        STREAM,
        UNKNOWN
    }

    private String title;
    private String author;
    private int duration;
    private PlaybackMediaType audioType;
    private PlaybackMediaType thumbnailType;
    private String audioSource; /** Path or URL depending on PlaybackMediaType */
    private String thumbnailSource; /** Path or URL depending on PlaybackMediaType */
    private String origin;
    private PlatformCompatUtility.Platform platformOrigin;
    private boolean isPrivate;

    public PlaybackAudioInfo() {
        this.isPrivate = false;
    }

    public PlaybackAudioInfo(String title, String author, String source, PlaybackMediaType type) {
        this.title = title;
        this.author = author;
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

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
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
