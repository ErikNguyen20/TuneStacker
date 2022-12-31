package com.example.cloudplaylistmanager;


public class PlaybackAudioInfo {
    enum PlaybackMediaType {
        LOCAL,
        STREAM,
        UNKNOWN
    }

    private String title;
    private String author;
    private int duration;
    private PlaybackMediaType type;
    private String source; /** Path or URL depending on PlaybackMediaType */

    public PlaybackAudioInfo(String title, String author, int duration, String source, PlaybackMediaType type) {
        this.title = title;
        this.author = author;
        this.duration = duration;
        this.source = source;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public PlaybackMediaType getType() {
        return type;
    }

    public void setType(PlaybackMediaType type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
