package com.saif.mymusic;
public class MusicFiles {
    private String path;
    private String title;
    private String artist;
    private String album;
    private String duration;
    private String id;
    private boolean isPlaying;
    private long size;
    private long dateAdded;
    private long dateModified;
    private boolean isSelected;
    public MusicFiles(String path, String title, String artist,String album,
                      String duration,String id,long size,
                        long dateAdded, long dateModified,boolean isPlaying) {
        this.path = path;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.id=id;
        this.size=size;
        this.dateAdded=dateAdded;
        this.dateModified=dateModified;
        this.album=album;
        this.isSelected=false;
        this.isPlaying=isPlaying;
    }
    public MusicFiles() {
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public long getDateModified() {
        return dateModified;
    }

    public void setDateModified(long dateModified) {
        this.dateModified = dateModified;
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
    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
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
