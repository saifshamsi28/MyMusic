package com.saif.mymusic;

import static com.saif.mymusic.MainActivity.loopOneButton;
import static com.saif.mymusic.MainActivity.shuffleButton;
import static com.saif.mymusic.MusicAdapter.mFiles;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity {
    TextView songName,durationPlayed,durationTotal,singerName;
    ImageView album,loop,shuffle,previous,next,playPauseButton,playlist;
    SeekBar seekBar;
    int position;
    static ArrayList<MusicFiles>listOfSongs = new ArrayList<>();
    static Uri uri;
    static MediaPlayer mediaPlayer;
    private Handler handler=new Handler();

    @Override
    protected void onDestroy() {
        loopOneButton=false;
        super.onDestroy();
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
        seekBar = findViewById(R.id.seekBar);
        getIntentMethod();
        songName.setSelected(true);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress);
                    playPauseButton.setImageResource(R.drawable.pause);
                    mediaPlayer.start();  // when song is paused and you changed the seekBar then song should play from where it sought
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        //Thread to change seekbar along with the song played and updating the time played every second
        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    durationPlayed.setText(playedTimeMethod(currentPosition / 1000));//divided by 1000 to get time in seconds
                }
                handler.postDelayed(this, 1000);
            }
        });
        // action when play-pause button is clicked
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    playPauseButton.setImageResource(R.drawable.play);
                    mediaPlayer.pause();
                } else {
                    playPauseButton.setImageResource(R.drawable.pause);
                    mediaPlayer.start();
                }
            }
        });
        //action when previous button is clicked
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    playPrevious();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    playNext();
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(loopOneButton)
                    initializeMediaPlayer(); // if loop is on then restart the same song
                else
                    playNext();
            }
        });
        loop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
    }
    // to format time in minutes and seconds from milliseconds
    private String playedTimeMethod(int currentPosition){
        String totalOut="";
        String totalNew="";
        String seconds=String.valueOf(currentPosition % 60);
        String minutes=String.valueOf(currentPosition / 60);
        totalOut=minutes+":"+seconds;
        totalNew=minutes+":0"+seconds;
        if(seconds.length()==1){
            return totalNew;
        }
            return totalOut;
    }
    private void getIntentMethod() {
        position=getIntent().getIntExtra("Position",0);
        listOfSongs=mFiles;
        if(listOfSongs!=null){
            playPauseButton.setImageResource(R.drawable.pause);
            uri=Uri.parse(listOfSongs.get(position).getPath());
        }
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer=MediaPlayer.create(getApplicationContext(),uri);
            mediaPlayer.start();
        }
        else {
            mediaPlayer=MediaPlayer.create(getApplicationContext(),uri);
            mediaPlayer.start();
        }
        seekBar.setMax(mediaPlayer.getDuration());
        getMetaDataOfSong(uri);
    }
    private void getMetaDataOfSong(Uri uri) {
        MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());
        byte[] art=retriever.getEmbeddedPicture();
        Bitmap bitmap;
        songName.setText(listOfSongs.get(position).getTitle());
        singerName.setText(listOfSongs.get(position).getArtist());
        durationTotal.setText(playedTimeMethod(mediaPlayer.getDuration()/1000));
        if(art!=null){
            Glide.with(this)
                    .asBitmap()
                    .load(art)
                    .into(album);
            bitmap= BitmapFactory.decodeByteArray(art,0, art.length);
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(@Nullable Palette palette) {
                    Palette.Swatch swatch=palette.getDominantSwatch();
                    if(swatch!=null){
                        ConstraintLayout playerActivityBackground=findViewById(R.id.player_activity_bg);
                        playerActivityBackground.setBackgroundResource(R.drawable.gradient_black);
                        GradientDrawable gradientDrawable=new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{swatch.getRgb(),0x00000000});
                        playerActivityBackground.setBackground(gradientDrawable);
                        songName.setTextColor(swatch.getTitleTextColor());
                        singerName.setTextColor(swatch.getTitleTextColor());
                    }
                    else {
                        ConstraintLayout playerActivityBackground=findViewById(R.id.player_activity_bg);
                        playerActivityBackground.setBackgroundResource(R.drawable.gradient_black);
                        GradientDrawable gradientDrawable=new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{0xff000000,0x00000000});
                        playerActivityBackground.setBackground(gradientDrawable);
                        songName.setTextColor(Color.WHITE);
                        singerName.setTextColor(Color.WHITE);
                    }
                }
            });
        }
        else {
            ConstraintLayout playerActivityBackground=findViewById(R.id.player_activity_bg);
            playerActivityBackground.setBackgroundResource(R.drawable.gradient_bg);
//            songName.setTextColor(Color.parseColor("#808000"));  // to change colour directly
            songName.setTextColor(ContextCompat.getColor(this, R.color.Olive));
            album.setImageResource(R.drawable.logo);
        }
    }
    // Method to initialize MediaPlayer and set OnCompletionListener
    private void initializeMediaPlayer() {
        mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(loopOneButton)
                    initializeMediaPlayer(); // if loop is on then restart the same song
                else
                    playNext(); // if loop is off then play next song after completing previous song
            }
        });
        seekBar.setMax(mediaPlayer.getDuration());
        mediaPlayer.start();
        playPauseButton.setImageResource(R.drawable.pause);
        getMetaDataOfSong(uri);
    }
    // Method to play the previous song
    private void playPrevious() {
        mediaPlayer.stop();
        mediaPlayer.release();
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
        initializeMediaPlayer();//it initialize and ensures that next song will automatically play after this song completes
    }
    // Method to play the next song
    private void playNext() {
        mediaPlayer.stop();
        mediaPlayer.release();
        if(shuffleButton)
        {
            position=getRandomSongPosition(listOfSongs.size());
        } else {
            position = (position + 1) % listOfSongs.size();
        }
        uri = Uri.parse(listOfSongs.get(position).getPath());
        initializeMediaPlayer();
    }

//Method to get song randomly
    private int getRandomSongPosition(int size) {
        Random random=new Random();
        return random.nextInt(size);
    }

}