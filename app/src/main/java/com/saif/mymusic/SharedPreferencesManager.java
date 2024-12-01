package com.saif.mymusic;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPreferencesManager {

    public static final String LAST_PLAYED_MUSIC = "last_played_music";
    public static final String LAST_MUSIC_FILE_PATH = "last_music_file_path";
    public static final String LAST_MUSIC_FILE_NAME = "last_music_file_name";
    public static final String LAST_MUSIC_FILE_ARTIST = "last_music_file_artist";
    public static final String LAST_MUSIC_FILE_POSITION = "last_music_file_position";
    public static final String LAST_MUSIC_FILE_PROGRESS = "last_music_file_progress";

    public static void saveLastPlayedMusic(Context context, MusicService musicService) {
        if(musicService!=null) {
            Log.d("SharedPreferences", "saveLastPlayedMusic: last music details saved");
            SharedPreferences.Editor editor = context.getSharedPreferences(LAST_PLAYED_MUSIC, MODE_PRIVATE).edit();
            editor.putString(LAST_MUSIC_FILE_PATH, musicService.uri.toString());
            editor.putString(LAST_MUSIC_FILE_NAME, musicService.musicList.get(musicService.position).getTitle());
            editor.putString(LAST_MUSIC_FILE_ARTIST, musicService.musicList.get(musicService.position).getArtist());
            editor.putInt(LAST_MUSIC_FILE_POSITION, musicService.position);
            editor.putInt(LAST_MUSIC_FILE_PROGRESS, musicService.getCurrentPosition());
            editor.apply();
        }else {
            Log.e("SharedPreferences", "saveLastPlayedMusic: last music details not saved ");
        }
    }

    public static MusicInfo getLastPlayedMusic(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(LAST_PLAYED_MUSIC, MODE_PRIVATE);
        String path = preferences.getString(LAST_MUSIC_FILE_PATH, null);
        String name = preferences.getString(LAST_MUSIC_FILE_NAME, null);
        String artist = preferences.getString(LAST_MUSIC_FILE_ARTIST, null);
        int position = preferences.getInt(LAST_MUSIC_FILE_POSITION, -1);
        int progress = preferences.getInt(LAST_MUSIC_FILE_PROGRESS, 0);

        return new MusicInfo(path, name, artist, position, progress);
    }

    public static class MusicInfo {
        public String path;
        public String name;
        public String artist;
        public int position;
        public int progress;

        public MusicInfo(String path, String name, String artist, int position, int progress) {
            this.path = path;
            this.name = name;
            this.artist = artist;
            this.position = position;
            this.progress = progress;
        }
    }
}

