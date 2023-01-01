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
    private PlaybackMediaType type;
    private PlaybackMediaType thumbnailType;
    private String source; /** Path or URL depending on PlaybackMediaType */
    private String thumbnailSource; /** Path or URL depending on PlaybackMediaType */
    private boolean isPrivate;

    public PlaybackAudioInfo() {
        this.isPrivate = false;
    }

    public PlaybackAudioInfo(String title, String author, String source, PlaybackMediaType type) {
        this.title = title;
        this.author = author;
        this.source = source;
        this.type = type;
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

    public PlaybackMediaType getType() {
        return this.type;
    }

    public void setType(PlaybackMediaType type) {
        this.type = type;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
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
}
