package com.saif.mymusic;

import static com.saif.mymusic.MainActivity.REQUEST_CODE;
import static com.saif.mymusic.MainActivity.loopOneButton;
import static com.saif.mymusic.MainActivity.shuffleButton;
import static com.saif.mymusic.MusicAdapter.mFiles;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.palette.graphics.Palette;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity implements ControlPlayAction, ServiceConnection {
    TextView songName,durationPlayed,durationTotal,singerName;
    ImageView album,loop,shuffle,previous,next,playPauseButton,songInfo;
    SeekBar seekBar;
    int position;
    static ArrayList<MusicFiles>listOfSongs = new ArrayList<>();
    static Uri uri;
    private final Handler handler=new Handler();
    private static PlayerActivity instance;
    MusicService musicService;
//    private BarVisualizer audioVisualizer;
    int audioSessionId;

    private String song_info_name = "My Song"; // Replace with actual data
    private String songLocation = "/storage/emulated/0/Music/MySong.mp3";
    private String songArtist = "Unknown Artist";
    private String songAlbum = "Unknown Album";
    private String songDuration = "3:45";
    private String songSize = "5 MB";
    private String dateAdded = "2023-07-01";
    private String dateModified = "2023-07-01";

    public static PlayerActivity getInstance(){
        return instance;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent serviceIntent = new Intent(this, MusicService.class);
        startService(serviceIntent);
        bindService(serviceIntent, this, BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(metadataChangeReceiver, new IntentFilter("com.saif.mymusic.METADATA_CHANGED"));
    }

    private final BroadcastReceiver metadataChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null && intent.getAction().equals("com.saif.mymusic.METADATA_CHANGED")) {
                String newTitle = intent.getStringExtra("TITLE");
                String newArtist = intent.getStringExtra("ARTIST");
                recreate();

                // Update UI
                songName.setText(newTitle);
                singerName.setText(newArtist);
                Toast.makeText(PlayerActivity.this, "Song updated successfully!", Toast.LENGTH_SHORT).show();

                // Update listOfSongs if necessary
                if (position != -1 && position < listOfSongs.size()) {
                    listOfSongs.get(position).setTitle(newTitle);
                    listOfSongs.get(position).setArtist(newArtist);
                }
            }else {
                Toast.makeText(PlayerActivity.this, "Song update failed!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(metadataChangeReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String newTitle = data.getStringExtra("TITLE");
            String newArtist = data.getStringExtra("ARTIST");

            songName.setText(newTitle);
            singerName.setText(newArtist);
            Toast.makeText(this, "Song updated successfully!", Toast.LENGTH_SHORT).show();
            if (position != -1 && position < listOfSongs.size()) {
                listOfSongs.get(position).setTitle(newTitle);
                listOfSongs.get(position).setArtist(newArtist);
            }
        }else {
            Toast.makeText(this, "Song update failed!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        album = findViewById(R.id.album_art);
        songName = findViewById(R.id.song_name);
        singerName=findViewById(R.id.singer_name);
        loop = findViewById(R.id.loop);
        shuffle = findViewById(R.id.shuffle);
        previous = findViewById(R.id.previous);
        durationPlayed = findViewById(R.id.duration_played);
        durationTotal = findViewById(R.id.duration_total);
        next = findViewById(R.id.next);
        playPauseButton = findViewById(R.id.play_pause_button);
        songInfo = findViewById(R.id.info_button);
        seekBar = findViewById(R.id.seekBar);
        instance=this;

        songName.setSelected(true);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService != null && fromUser) {
                    musicService.seekTo(progress);
                    musicService.updatePlaybackState(musicService.isPlaying()?PlaybackStateCompat.STATE_PLAYING:PlaybackStateCompat.STATE_PAUSED, progress);
                    playPauseButton.setImageResource(R.drawable.pause);
                    musicService.start();  // when song is paused and you changed the seekBar then song should play from where it sought
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                durationPlayed.setText(playedTimeMethod(seekBar.getProgress()/1000));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        //Thread to change seekbar along with the song played and updating the time played every second
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (musicService != null) {
                    int currentPosition = musicService.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    durationPlayed.setText(playedTimeMethod(currentPosition / 1000));//divided by 1000 to get time in seconds
                }
                handler.postDelayed(this, 1000);
            }
        });
        // click listeners
        playPauseButton.setOnClickListener(v -> playPause());
        previous.setOnClickListener(v -> playPrevious());
        songInfo.setOnClickListener(v -> showSongMetadataDialog());
        next.setOnClickListener(v -> playNext());
        loop.setOnClickListener(v -> {
            shuffleButton=false;
            shuffle.setImageResource(R.drawable.shuffle_off);
            if(loopOneButton) {
                loopOneButton = false;
                loop.setImageResource(R.drawable.loop_list);
                Toast.makeText(PlayerActivity.this, "Playlist Loop", Toast.LENGTH_SHORT).show();
            }
            else {
                loopOneButton=true;
                loop.setImageResource(R.drawable.loop_one);
                Toast.makeText(PlayerActivity.this, "Single Loop", Toast.LENGTH_SHORT).show();
            }
        });
        shuffle.setOnClickListener(v -> {
            loopOneButton=false;
            loop.setImageResource(R.drawable.loop_list);
            if(shuffleButton){
                shuffleButton=false;
                shuffle.setImageResource(R.drawable.shuffle_off);
                Toast.makeText(PlayerActivity.this, "Shuffle Off", Toast.LENGTH_SHORT).show();
            }
            else {
                shuffleButton=true;
                shuffle.setImageResource(R.drawable.shuffle_on);
                Toast.makeText(PlayerActivity.this, "Shuffle On", Toast.LENGTH_SHORT).show();
            }
        });

    }
    // to format time in minutes and seconds from milliseconds
    public String playedTimeMethod(int currentPosition){
        String seconds=String.valueOf(currentPosition % 60);
        String minutes=String.valueOf(currentPosition / 60);
        String totalOut = minutes + ":" + seconds;
        String totalNew = minutes + ":0" + seconds;
        if(seconds.length()==1){
            return totalNew;
        }
            return totalOut;
    }

    private void getIntentMethod() {
        Log.e("PlayerActivity", "getIntentMethod: called");
        Intent intent = getIntent();
        position = intent.getIntExtra("Position", -1);
        int currentPosition = intent.getIntExtra("currentPosition", -1);
        boolean isPlaying = intent.getBooleanExtra("isPlaying", false);
        boolean fromNotification = intent.getBooleanExtra("fromNotification", false);

        if (mFiles != null && !mFiles.isEmpty()) {
            listOfSongs = mFiles;
            uri = Uri.parse(listOfSongs.get(position).getPath());
        } else {
            Log.e("PlayerActivity", "Song list is empty");
            return;
        }

        if (musicService != null) {
            musicService.updateMusicList(listOfSongs);
            handleMusicService(fromNotification, currentPosition, isPlaying);
        } else {
            Log.e("PlayerActivity", "getIntentMethod: musicService is null");
        }
    }

    private void handleMusicService(boolean fromNotification, int currentPosition, boolean isPlaying) {
        if (fromNotification) {
            if (currentPosition != -1) {
                if(!musicService.isPlaying()) {
                    musicService.seekTo(currentPosition);
                }
            }

            if (isPlaying) {
                musicService.start();
                musicService.showNotification(R.drawable.pause_notification);
                playPauseButton.setImageResource(R.drawable.pause);
            } else {
                musicService.pause();
                musicService.showNotification(R.drawable.play_notification);
                playPauseButton.setImageResource(R.drawable.play);
            }
        } else {
            musicService.stop();
            musicService.release();
            musicService.createMediaPlayer(position);
            musicService.start();
            getMetaDataOfSong(musicService.uri, position);
        }
    }


    public void getMetaDataOfSong(Uri uri,int currentPosition) {
        if (isDestroyed() ||uri==null) {
            Intent intent1=new Intent(this,MainActivity.class);
            Log.e("MusicService","service is null,going to main activity");
            startActivity(intent1);
            finish();
            return;
        }
        MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        byte[] art=retriever.getEmbeddedPicture();
        Bitmap bitmap;
        songName.setText(listOfSongs.get(currentPosition).getTitle());
        song_info_name=listOfSongs.get(currentPosition).getTitle();
        singerName.setText(listOfSongs.get(currentPosition).getArtist());
        songArtist=listOfSongs.get(currentPosition).getArtist();
//        playPauseButton.setImageResource(R.drawable.pause);
        seekBar.setMax(musicService.getDuration());
        durationTotal.setText(playedTimeMethod(musicService.getDuration()/1000));
        songDuration= String.valueOf(musicService.getDuration());
        songLocation=uri.toString();
        songSize= String.valueOf((listOfSongs.get(currentPosition).getSize()));
        dateAdded= String.valueOf((listOfSongs.get(currentPosition).getDateAdded()));
        dateModified= String.valueOf((listOfSongs.get(currentPosition).getDateModified()));
        Log.d("PlayerActivity", "getMetaDataOfSong,Date added: "+listOfSongs.get(currentPosition).getDateAdded());
        Log.d("PlayerActivity", "getMetaDataOfSong,Date added(formatted): "+dateAdded);
        songAlbum=listOfSongs.get(currentPosition).getAlbum();
        if(art!=null){
            Glide.with(this)
                    .asBitmap()
                    .load(art)
                    .into(album);
            bitmap= BitmapFactory.decodeByteArray(art,0, art.length);
            Palette.from(bitmap).generate(palette -> {
                Palette.Swatch swatch = null;
                if(palette!=null)
                    swatch=palette.getDominantSwatch();
                if(swatch!=null){
                    ConstraintLayout playerActivityBackground=findViewById(R.id.player_activity_bg);
                    playerActivityBackground.setBackgroundResource(R.drawable.gradient_black);
                    GradientDrawable gradientDrawable=new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                            new int[]{swatch.getRgb(),0x00000000});
                    playerActivityBackground.setBackground(gradientDrawable);
                    getWindow().setStatusBarColor(swatch.getRgb());
                    songName.setTextColor(swatch.getTitleTextColor());
                    singerName.setTextColor(swatch.getTitleTextColor());
                }
                else {
                    ConstraintLayout playerActivityBackground=findViewById(R.id.player_activity_bg);
                    playerActivityBackground.setBackgroundResource(R.drawable.gradient_black);
                    GradientDrawable gradientDrawable=new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                            new int[]{0xff000000,0x00000000});
                    playerActivityBackground.setBackground(gradientDrawable);
                    getWindow().setStatusBarColor(ContextCompat.getColor(PlayerActivity.this, R.color.light_black));
                    songName.setTextColor(Color.WHITE);
                    singerName.setTextColor(Color.WHITE);
                }
            });
        }
        else {
            ConstraintLayout playerActivityBackground=findViewById(R.id.player_activity_bg);
            playerActivityBackground.setBackgroundResource(R.drawable.gradient_bg);
            songName.setTextColor(ContextCompat.getColor(this, R.color.Olive));
            album.setImageResource(R.drawable.logo);
        }
    }

    //method to play and pause the songs
    public void playPause(){
        if(musicService!=null) {
            if (musicService.isPlaying()) {
                playPauseButton.setImageResource(R.drawable.play);
                musicService.showNotification(R.drawable.play_notification);
                musicService.pause();
                songName.setSelected(false);
                if(MiniPlayerFragment.getInstance().playPauseButton!=null)
                        MiniPlayerFragment.getInstance().playPauseButton.setImageResource(R.drawable.play);
            } else {
                playPauseButton.setImageResource(R.drawable.pause);
                musicService.start();
                songName.setSelected(true);
                musicService.showNotification(R.drawable.pause_notification);
                if(MiniPlayerFragment.getInstance().playPauseButton!=null)
                    MiniPlayerFragment.getInstance().playPauseButton.setImageResource(R.drawable.pause);
//                MusicAdapter.animation.start();
            }
            musicService.updatePlaybackState(musicService.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, musicService.getCurrentPosition());
        }else {
            Log.e("PlayerActivity", "playPause: musicService is null");
        }
    }

    // Method to play the previous song
    public void playPrevious() {
        if(musicService!=null) {
            musicService.stop();
            musicService.release();
        }
        if(shuffleButton)
        {
            position=getRandomSongPosition(listOfSongs.size());
        } else {
            if(position>0)
                position=position-1;
            else
                position=listOfSongs.size()-1;
        }
        //third case remains shuffle=false && loopOne=true...means automatically song will be in loop if these two conditions false
        uri = Uri.parse(listOfSongs.get(position).getPath());
//        initializeMediaPlayer();//it initialize and ensures that next song will automatically play after this song completes
        musicService.createMediaPlayer(position);
        musicService.start();
        playPauseButton.setImageResource(R.drawable.pause);
        musicService.showNotification(R.drawable.pause_notification);
        getMetaDataOfSong(uri,position);
        if(MiniPlayerFragment.getInstance().playPauseButton!=null) {
            MiniPlayerFragment.getInstance().changeSongNameAndArtist();
            MiniPlayerFragment.getInstance().playPauseButton.setImageResource(R.drawable.pause);
        }
    }

    // Method to play the next song
    public void playNext() {
        if(musicService!=null) {
            musicService.stop();
            musicService.release();
        }
        if(shuffleButton)
        {
            position=getRandomSongPosition(listOfSongs.size());
        }else{
            position = (position + 1) % listOfSongs.size();
        }
        uri = Uri.parse(listOfSongs.get(position).getPath());
        musicService.createMediaPlayer(position);
        musicService.start();
        playPauseButton.setImageResource(R.drawable.pause);
        musicService.showNotification(R.drawable.pause_notification);
        getMetaDataOfSong(uri,position);
        if(MiniPlayerFragment.getInstance().playPauseButton!=null) {
            MiniPlayerFragment.getInstance().changeSongNameAndArtist();
            MiniPlayerFragment.getInstance().playPauseButton.setImageResource(R.drawable.pause);
        }else {
            Log.e("PlayerActivity", "playNext: playPauseButton of miniPlayer is null is null");
        }
    }

    //Method to get song randomly
    private int getRandomSongPosition(int size) {
        Random random=new Random();
        return random.nextInt(size);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder myBinder=(MusicService.MyBinder)service;
        musicService=myBinder.getService();
        getIntentMethod();
        musicService.setPlayAction(PlayerActivity.this);
        seekBar.setMax(musicService.getDuration());
//        musicService.start();

        getMetaDataOfSong(uri,position);
        audioSessionId = musicService.getAudioSessionId();
        Log.i("onServiceConnected","Connected "+musicService);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService=null;
    }

    private void showSongMetadataDialog() {

            Intent intent = new Intent(this, SongInfoActivity.class);
            intent.putExtra("TITLE", song_info_name);
            intent.putExtra("ARTIST", songArtist);
            intent.putExtra("DURATION", songDuration);
            intent.putExtra("SIZE", songSize);
            intent.putExtra("LOCATION", songLocation);
            intent.putExtra("ALBUM_ART_PATH", uri.toString());
            intent.putExtra("DATE_ADDED",dateAdded);
            intent.putExtra("ALBUM",songAlbum);
            intent.putExtra("DATE_MODIFIED",dateModified);
            startActivity(intent);

    }
}