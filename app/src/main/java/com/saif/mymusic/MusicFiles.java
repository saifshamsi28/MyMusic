package com.saif.mymusic;
public class MusicFiles {
    private String path;
    private String title;
    private String artist;
    private String duration;
    private String id;
    private boolean isSelected;
    public MusicFiles(String path, String title, String artist, String duration,String id) {
        this.path = path;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.id=id;
        this.isSelected=false;
    }
    public MusicFiles() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public boolean isSelected() {
        return isSelected;
    }
    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
