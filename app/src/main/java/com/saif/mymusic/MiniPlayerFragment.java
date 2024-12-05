package com.saif.mymusic;

import static android.content.Context.MODE_PRIVATE;
import static com.saif.mymusic.MainActivity.LAST_MUSIC_ARTIST;
import static com.saif.mymusic.MainActivity.LAST_MUSIC_NAME;
import static com.saif.mymusic.MainActivity.PATH_TO_MINI_PLAYER;
import static com.saif.mymusic.MainActivity.SHOW_MINI_PLAYER;
import static com.saif.mymusic.MainActivity.currentPlayingPosition;
import static com.saif.mymusic.MainActivity.musicFiles;
import static com.saif.mymusic.MusicService.LAST_MUSIC_FILE_POSITION;
import static com.saif.mymusic.MusicService.LAST_MUSIC_FILE_PROGRESS;
import static com.saif.mymusic.SharedPreferencesManager.getLastPlayedMusic;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;

import java.io.IOException;

public class MiniPlayerFragment extends Fragment implements ServiceConnection,ControlPlayAction {
    ImageView albumArt,nextButton,playPauseButton;
    TextView songName,artistName;
    CardView miniPlayerCardView;
    View view;
    MusicService musicService;
    public static final String LAST_PLAYED_MUSIC="last_played_music";
    public static final String LAST_MUSIC_FILE_PATH ="last_music_file";
    public static final String LAST_MUSIC_FILE_NAME ="last_music_file_name";
    public static final String LAST_MUSIC_FILE_ARTIST ="last_music_file_artist";
    public static int SAVED_POSITION = 0;
    public static int SAVED_PROGRESS = 0;
    public static String SAVED_PATH = null;


    public MiniPlayerFragment() {
        // Required empty public constructor
    }

    private final BroadcastReceiver metadataChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null && intent.getAction().equals("com.saif.mymusic.METADATA_CHANGED")) {
                String newTitle = intent.getStringExtra("TITLE");
                String newArtist = intent.getStringExtra("ARTIST");

                // Update UI
                songName.setText(newTitle);
                artistName.setText(newArtist);

                // Update listOfSongs if necessary
                if (currentPlayingPosition != -1 && musicService != null && musicService.position != -1 && currentPlayingPosition < musicFiles.size()) {
                    musicFiles.get(musicService.position).setTitle(newTitle);
                    musicFiles.get(musicService.position).setArtist(newArtist);
                }
            }
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view=inflater.inflate(R.layout.fragment_mini_player, container, false);
        artistName=view.findViewById(R.id.mini_player_artist_name);
        songName=view.findViewById(R.id.mini_player_song_name);
        miniPlayerCardView=view.findViewById(R.id.mini_player_cardview);

        if(getActivity()!=null){
            SharedPreferences preferences = getActivity().getSharedPreferences(LAST_PLAYED_MUSIC, MODE_PRIVATE);
            SAVED_PATH = preferences.getString(LAST_MUSIC_FILE_PATH, null);
            SAVED_POSITION = preferences.getInt(LAST_MUSIC_FILE_POSITION, 0);
            SAVED_PROGRESS = preferences.getInt(LAST_MUSIC_FILE_PROGRESS, 0);
        }
        Log.e("MiniPlayerFragment","onCreate: last music saved position: "+SAVED_POSITION);
        Log.e("MiniPlayerFragment","onCreate: last music saved progress: "+SAVED_PROGRESS);

        songName.setSelected(true);
        songName.setSingleLine(true);
        songName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        songName.setMarqueeRepeatLimit(-1);

        albumArt=view.findViewById(R.id.bottom_album_art);
        nextButton=view.findViewById(R.id.next_button);
        playPauseButton=view.findViewById(R.id.mini_payer_play_pause_button);
        if(musicService!=null){
            SharedPreferencesManager.MusicInfo musicInfo;
            if(getContext()!=null) {
                musicInfo = getLastPlayedMusic(getContext());
                musicService.position = musicInfo.position;
                musicService.uri = Uri.parse(musicInfo.path);
                musicService.musicList = musicFiles;
                if (musicService.isPlaying()) {
                    playPauseButton.setImageResource(R.drawable.pause);
                } else {
                    playPauseButton.setImageResource(R.drawable.play);
                }
            }
        }
        miniPlayerCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), PlayerActivity.class);
                if(musicService!=null) {
                    if(musicService.position==-1){
                        musicService.musicList=musicFiles;
                        musicService.position=SAVED_POSITION;
                        musicService.createMediaPlayer(musicService.position);
                        musicService.seekTo(SAVED_PROGRESS);
                        musicService.start();
                    }
                    intent.putExtra("Position", musicService.position);
                    intent.putExtra("isPlaying", musicService.isPlaying());
                    intent.putExtra("currentPosition", musicService.getCurrentPosition());
                    intent.putExtra("fromNotification", true);
                }else {
                    intent.putExtra("Position", SAVED_POSITION);
                    intent.putExtra("currentPosition", SAVED_PROGRESS);
                    intent.putExtra("fromNotification", true);
                    intent.putExtra("isPlaying", false);
                }
                startActivity(intent);
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(musicService!=null) {
                    if(musicService.musicList.isEmpty()){
                        musicService.musicList=musicFiles;
                    }
                        if(musicService.position==-1){
                                musicService.position=SAVED_POSITION;
                                musicService.createMediaPlayer(musicService.position);
                                musicService.seekTo(SAVED_PROGRESS);
                        }
                            playPause();
                }else {
                    Log.e("MiniPlayerFragment", "music service is null");
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicService != null) {
                    musicService.stop();
                    if(musicService.position==-1){
                        musicService.musicList=musicFiles;
                        playNext();
                    }else {
                        playNext();
                    }
                    saveCurrentPosition(musicService.position);
                            Log.e("MiniPlayerFragment", " position: " + musicService.position);
                }else {
                    Log.e("MiniPlayerFragment", "music service is null");
                }
            }

            private void saveCurrentPosition(int position) {
                if (getActivity()!=null) {
                    SharedPreferences.Editor editor = getActivity().getSharedPreferences(LAST_PLAYED_MUSIC, MODE_PRIVATE).edit();
                    editor.putString(LAST_MUSIC_FILE_PATH, musicService.musicList.get(position).getPath());
                    editor.putString(LAST_MUSIC_FILE_NAME, musicService.musicList.get(position).getTitle());
                    editor.putString(LAST_MUSIC_FILE_ARTIST, musicService.musicList.get(position).getArtist());
                    editor.putInt(LAST_MUSIC_FILE_POSITION, position);
                    editor.apply();
                    SharedPreferences preferences = getActivity().getSharedPreferences(LAST_PLAYED_MUSIC, MODE_PRIVATE);
                    updateMiniPlayer(preferences);
                }

            }
            private void updateMiniPlayer(SharedPreferences preferences) {
                String lastPlayedSong = preferences.getString(LAST_MUSIC_FILE_PATH, null);
                LAST_MUSIC_NAME = preferences.getString(LAST_MUSIC_FILE_NAME, null);
                LAST_MUSIC_ARTIST = preferences.getString(LAST_MUSIC_FILE_ARTIST, null);
                SAVED_POSITION =preferences.getInt(LAST_MUSIC_FILE_POSITION, 0);

                if (lastPlayedSong != null) {
                    SHOW_MINI_PLAYER = true;
                    PATH_TO_MINI_PLAYER = lastPlayedSong;
                } else {
                    SHOW_MINI_PLAYER = false;
                    PATH_TO_MINI_PLAYER = null;
                }
                upDateMiniPlayer();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(metadataChangeReceiver);
        if(getContext()!=null) {
                Intent intent = new Intent(getContext(), MusicService.class);
                intent.putExtra("servicePosition",LAST_MUSIC_FILE_POSITION);
                getContext().bindService(intent, this, Service.BIND_AUTO_CREATE);
            upDateMiniPlayer();
        }
    }

    //method to get instance of mini player in other class to access the fields of this class
    public static MiniPlayerFragment getInstance(){
        return new MiniPlayerFragment();
    }

    public void upDateMiniPlayer() {
        if(SHOW_MINI_PLAYER){
            try {
                if(PATH_TO_MINI_PLAYER!=null) {
                    if(musicService!=null &&musicService.musicList!=null && !musicService.musicList.isEmpty()) {
                        byte[] art = getAlbumArt(musicService.musicList.get(musicService.position).getPath());
                        if (getContext() != null) {
                            Glide.with(getContext()).load(art)
                                    .placeholder(R.drawable.logo)
                                    .into(albumArt);
                            songName.setText(musicService.musicList.get(musicService.position).getTitle());
                            artistName.setText(musicService.musicList.get(musicService.position).getArtist());
                            if (musicService != null) {
                                if (musicService.isPlaying()) {
                                    playPauseButton.setImageResource(R.drawable.pause);
                                } else {
                                    playPauseButton.setImageResource(R.drawable.play);
                                }
                            }
                        }
                    }else {
                        byte[] art = getAlbumArt(SAVED_PATH);
                        if (getContext() != null) {
                            Glide.with(getContext()).load(art)
                                    .placeholder(R.drawable.logo)
                                    .into(albumArt);
                            songName.setText(LAST_MUSIC_NAME);
                            artistName.setText(LAST_MUSIC_ARTIST);
                            if (musicService != null) {
                                if (musicService.isPlaying()) {
                                    playPauseButton.setImageResource(R.drawable.pause);
                                } else {
                                    playPauseButton.setImageResource(R.drawable.play);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else {
            Log.e("MiniPlayerFragment", "show mini is: false");
        }
    }

    //method to change the song name and artist name when song is changed
    public void changeSongNameAndArtist() {
        songName.setText(musicService.musicList.get(musicService.position).getTitle());
        artistName.setText(musicService.musicList.get(musicService.position).getArtist());
        Log.e("MiniPlayerFragment", "changeSongNameAndArtist: called,position: "+musicService.position);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getContext() != null && getActivity() != null) {
            if (musicService != null) {
                if (musicService.uri == null) {
                    if (SAVED_PATH != null) {
                        musicService.uri = Uri.parse(SAVED_PATH);
                        musicService.position = SAVED_POSITION;
                        musicService.musicList = musicFiles;
                    } else {
                        // Handle the case where SAVED_PATH is null
                        Log.e("MiniPlayerFragment", "SAVED_PATH is null, cannot set musicService.uri");
                        return;
                    }
                }
                SharedPreferencesManager.saveLastPlayedMusic(getContext(), musicService);
            }
        }
    }


    public byte[] getAlbumArt(String uri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(uri);
            return retriever.getEmbeddedPicture();
        } catch (IllegalArgumentException e) {
            Log.e("MusicAdapter", "Failed to set data source for MediaMetadataRetriever: " + e.getMessage());
            retriever.release();
            return null;
        } catch (RuntimeException e) {
            Log.e("MusicAdapter", "Runtime exception in MediaMetadataRetriever: " + e.getMessage());
            retriever.release();
            return null;
        } finally {
            retriever.release();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder binder=(MusicService.MyBinder) service;
        musicService=binder.getService();
        musicService.setPlayAction(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService=null;
    }

    @Override
    public void playPause() {
        if(musicService!=null){
            if(musicService.isPlaying()){
                musicService.pause();
                playPauseButton.setImageResource(R.drawable.play);
                Log.e("MiniPlayerFragment", "pause called:");
            }else {
                musicService.start();
                playPauseButton.setImageResource(R.drawable.pause);
                Log.e("MiniPlayerFragment", "play called:");
            }
        }
    }

    public void playPrevious() {
        if(musicService!=null) {
            musicService.stop();
            musicService.release();
            musicService.position = (musicService.position - 1) % musicService.musicList.size();

            musicService.createMediaPlayer(musicService.position);
            musicService.start();
            musicService.showNotification(R.drawable.pause_notification);
            songName.setText(musicService.musicList.get(musicService.position).getTitle());
            artistName.setText(musicService.musicList.get(musicService.position).getArtist());
            playPauseButton.setImageResource(R.drawable.pause);
        }
    }

    // Method to play the next song
    public void playNext() {
        if(musicService!=null) {
            musicService.stop();
            musicService.release();
            musicService.position = (musicService.position + 1) % musicService.musicList.size();
            musicService.createMediaPlayer(musicService.position);
            musicService.start();
            musicService.showNotification(R.drawable.pause_notification);
            songName.setText(musicService.musicList.get(musicService.position).getTitle());
            artistName.setText(musicService.musicList.get(musicService.position).getArtist());
            playPauseButton.setImageResource(R.drawable.pause);
        }
    }
}