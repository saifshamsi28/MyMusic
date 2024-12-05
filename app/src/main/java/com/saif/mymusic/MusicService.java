package com.saif.mymusic;

import static com.saif.mymusic.ApplicationClass.ACTION_NEXT;
import static com.saif.mymusic.ApplicationClass.ACTION_PLAY;
import static com.saif.mymusic.ApplicationClass.ACTION_PREVIOUS;
import static com.saif.mymusic.ApplicationClass.CHANNEL_ID_2;
import static com.saif.mymusic.MainActivity.currentPlayingPosition;
import static com.saif.mymusic.MusicAdapter.mFiles;
import static com.saif.mymusic.PlayerActivity.listOfSongs;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {
    IBinder mBinder = new MyBinder();
    MediaPlayer mediaPlayer;
    ArrayList<MusicFiles> musicList = new ArrayList<>();
    Uri uri;
    int position = -1;
    ControlPlayAction playAction;
    public static final String LAST_PLAYED_MUSIC="last_played_music";
    public static final String LAST_MUSIC_FILE_PATH ="last_music_file";
    public static final String LAST_MUSIC_FILE_NAME ="last_music_file_name";
    public static final String LAST_MUSIC_FILE_ARTIST ="last_music_file_artist";
    public static final String LAST_MUSIC_FILE_POSITION ="last_music_file_position" ;
    public static final String LAST_MUSIC_FILE_PROGRESS ="last_music_file_progress" ;
    MediaSessionCompat mediaSessionCompat;
    int currentPosition=0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("MusicService", "Service created");
        mediaSessionCompat=new MediaSessionCompat(getBaseContext(),"My Audio");
        if(listOfSongs!=null && !listOfSongs.isEmpty()) {
            musicList = listOfSongs;
        }else {
            musicList=mFiles;
        }
        if(musicList!=null){
            Log.e("MusicService", "onCreate: musicList size : "+musicList.size());
        }
    }

    public void setPlayAction(ControlPlayAction playAction) {
        this.playAction = playAction;
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e("Bind", "Method called");
        return mBinder;
    }

    public class MyBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null) {
            int myPosition = intent.getIntExtra("servicePosition", -1);
            String actionName = intent.getStringExtra("ActionName");
            if (myPosition >= 0 && myPosition < musicList.size()) {
                Log.e("MusicService", "onStartCommand: position is : " + myPosition);
                playSong(myPosition);
            }
            if (actionName != null) {
                switch (actionName) {
                    case "playPause":
                        playPauseClicked();
                        break;
                    case "previous":
                        previousClicked();
                        break;
                    case "next":
                        nextClicked();
                        break;
                }
            }
            return START_STICKY;
        }
        Log.e("MusicService", "onStartCommand:intent is null");
        return START_NOT_STICKY;
    }

    private void playSong(int startPosition) {
        if(listOfSongs!=null && !listOfSongs.isEmpty()) {
            musicList = listOfSongs;
        }else {
            musicList=mFiles;
        }
        position = startPosition;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        createMediaPlayer(position);
        start();
    }

    void start() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            Log.e("MusicService", "start media player called");
            mediaPlayer.start();
            currentPlayingPosition=position;
            getAudioSessionId();
            musicList.get(position).setPlaying(true);
            showNotification(R.drawable.pause_notification);
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, getCurrentPosition());
        }
    }

    void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            musicList.get(position).setPlaying(true);
            showNotification(R.drawable.play_notification);
        }
    }

    void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            musicList.get(position).setPlaying(true);
            showNotification(R.drawable.play_notification);
        }
    }

    void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    void createMediaPlayer(int position) {
        this.position=position;
        currentPlayingPosition=position;
        if(musicList==null){
            musicList=mFiles;
        }
        uri = Uri.parse(musicList.get(position).getPath());
        SharedPreferences.Editor editor=getSharedPreferences(LAST_PLAYED_MUSIC,MODE_PRIVATE).edit();
        editor.putString(LAST_MUSIC_FILE_PATH,uri.toString());
        editor.putString(LAST_MUSIC_FILE_NAME,musicList.get(position).getTitle());
        editor.putString(LAST_MUSIC_FILE_ARTIST,musicList.get(position).getArtist());
        editor.putInt(LAST_MUSIC_FILE_POSITION, position);
        editor.putInt(LAST_MUSIC_FILE_PROGRESS, 0);
        editor.apply();
        mediaPlayer = MediaPlayer.create(getBaseContext(), uri);
        mediaPlayer.setOnCompletionListener(this);
    }
    @Override
    public void onCompletion(MediaPlayer mp) {
        musicList.get(position).setPlaying(false);
        if (MainActivity.loopOneButton) {
            // If loop one button is pressed, replay the same song
            createMediaPlayer(position);
            start();
        } else if (MainActivity.shuffleButton) {
            // If shuffle button is pressed, play a random song
            position = getRandomSongPosition(musicList.size());
            createMediaPlayer(position);
            start();
        } else {
            position = (position + 1) % musicList.size();
            createMediaPlayer(position);
            start();
        }
        if(PlayerActivity.getInstance()!=null) {
            PlayerActivity.getInstance().getMetaDataOfSong(uri, position);
            PlayerActivity.getInstance().position = position;
        }
        showNotification(R.drawable.pause_notification);
//        MainActivity.getInstance().notifyAdapterAboutPlayingSong(position);
    }

    //to update the music list after sorting
    public void updateMusicList(ArrayList<MusicFiles> musicList) {
        this.musicList = musicList;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        SharedPreferencesManager.saveLastPlayedMusic(this, this);

        SharedPreferences preferences = getSharedPreferences(SharedPreferencesManager.LAST_PLAYED_MUSIC, MODE_PRIVATE);
        Log.e("MusicService", "onTaskRemoved:song stored in preference: " + preferences.getString(SharedPreferencesManager.LAST_MUSIC_FILE_NAME,null));
        Log.e("MusicService", "onTaskRemoved:song position in preference: " + preferences.getInt(LAST_MUSIC_FILE_POSITION, -1));
        Log.e("MusicService", "onTaskRemoved:song progress stored in preference: " + preferences.getInt(LAST_MUSIC_FILE_PROGRESS, -1));

        stopForeground(true); // Remove the notification
        stopSelf(); // Stop the service
    }


    public int getAudioSessionId() {
        if (mediaPlayer != null) {
            return mediaPlayer.getAudioSessionId();
        }
        return -1;

    }

    private int getRandomSongPosition(int size) {
        Random random = new Random();
        return random.nextInt(size);
    }

    void playPauseClicked(){
        if(playAction!=null) {
            Log.d("MusicService", "playPauseClicked: ");
            playAction.playPause();
        }
    }
    void nextClicked(){
        if(playAction!=null) {
            Log.d("MusicService", "nextClicked: ");
            playAction.playNext();
            //MiniPlayerFragment.getInstance().changeSongNameAndArtist();
        }
    }
    void previousClicked(){
        if(playAction!=null) {
            Log.d("MusicService", "previousClicked: ");
            playAction.playPrevious();
            //MiniPlayerFragment.getInstance().changeSongNameAndArtist();
        }
    }

    void showNotification(int playPauseBtn) {
        Intent intentContent = new Intent(this, PlayerActivity.class);
        intentContent.putExtra("Position", position);
        intentContent.putExtra("currentPosition", getCurrentPosition());
        intentContent.putExtra("fromNotification", true);
        intentContent.putExtra("isPlaying", isPlaying());
        Log.e("showNotification","showNotification: current position stored in intent: " + getCurrentPosition());
        PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, intentContent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PLAY);
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        byte[] picture;
        try {
            picture = getAlbumArt(musicList.get(position).getPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Bitmap thumb;
        if (picture != null) {
            thumb = BitmapFactory.decodeByteArray(picture, 0, picture.length);
        } else {
            thumb = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        }

        // Update Media Metadata
        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, musicList.get(position).getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, musicList.get(position).getArtist())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, thumb)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration()) // Set duration
                .build();
        mediaSessionCompat.setMetadata(metadata);

        // Update Playback State
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_SEEK_TO |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(
                        isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                        getCurrentPosition(),
                        1.0f
                )
                .build();
        mediaSessionCompat.setPlaybackState(playbackState);
        //to change the song from notification seekbar
        mediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
                seekTo((int) pos);
                updatePlaybackState(isPlaying()? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, pos);
            }
        });

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_2)
                .setSmallIcon(R.drawable.music_icon)
                .setLargeIcon(thumb)
                .setContentTitle(musicList.get(position).getTitle())
                .setContentText(musicList.get(position).getArtist())
                .setContentIntent(contentPendingIntent)
                .addAction(R.drawable.prev_notification, "Previous", prevPendingIntent)
                .addAction(playPauseBtn, "Play/Pause", playPendingIntent)
                .addAction(R.drawable.next_notification, "Next", nextPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2) //indices of actions you previously set through .addAction()
                        .setShowCancelButton(true)
                        .setMediaSession(mediaSessionCompat.getSessionToken()))
                .build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    void updatePlaybackState(int state, long position) {
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_SEEK_TO |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(state, position, 1.0f)
                .build();
        mediaSessionCompat.setPlaybackState(playbackState);
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
    public void onDestroy() {
        super.onDestroy();
//        SharedPreferencesManager.saveLastPlayedMusic(this, this);

        SharedPreferences preferences = getSharedPreferences(SharedPreferencesManager.LAST_PLAYED_MUSIC, MODE_PRIVATE);
        Log.e("MusicService", "onDestroy:song stored in preference: " + preferences.getString(SharedPreferencesManager.LAST_MUSIC_FILE_NAME,null));
        Log.e("MusicService", "onDestroy:song position in preference: " + preferences.getInt(LAST_MUSIC_FILE_POSITION, -1));
        Log.e("MusicService", "onDestroy:song progress stored in preference: " + preferences.getInt(LAST_MUSIC_FILE_PROGRESS, -1));

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);

        // Release the media session
        mediaSessionCompat.release();
        stopSelf();
    }
}

