//package com.saif.mymusic;
//
//import static com.saif.mymusic.MainActivity.loopOneButton;
//import static com.saif.mymusic.MainActivity.shuffleButton;
//import static com.saif.mymusic.PlayerActivity.listOfSongs;
//import static com.saif.mymusic.PlayerActivity.mediaPlayer;
//import static com.saif.mymusic.PlayerActivity.position;
//import static com.saif.mymusic.PlayerActivity.uri;
//import android.annotation.SuppressLint;
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.pm.PackageManager;
//import android.media.MediaPlayer;
//import android.net.Uri;
//import android.os.Build;
//import android.os.IBinder;
//import android.widget.Toast;
//
//import androidx.annotation.Nullable;
//import androidx.core.app.ActivityCompat;
//import androidx.core.app.NotificationCompat;
//import androidx.core.app.NotificationManagerCompat;
//
//public class MusicNotificationService extends Service {
//    private static final String CHANNEL_ID = "MusicNotificationChannel";
//    private static final int NOTIFICATION_ID = 1;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        createNotificationChannel();
//        registerReceiver(broadcastReceiver, new IntentFilter("PLAY_PAUSE"));
//        registerReceiver(broadcastReceiver, new IntentFilter("NEXT"));
//        registerReceiver(broadcastReceiver, new IntentFilter("PREVIOUS"));
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        showNotification();
//        return START_NOT_STICKY;
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        unregisterReceiver(broadcastReceiver);
//        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//
//    private void showNotification() {
//        Intent openActivityIntent = new Intent(this, PlayerActivity.class);
//        openActivityIntent.putExtra("Title", listOfSongs.get(position).getTitle());
//        openActivityIntent.putExtra("Position", position);
//        PendingIntent openActivityPendingIntent = PendingIntent.getActivity(this, 0, openActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
//
//        PendingIntent playPauseIntent = PendingIntent.getBroadcast(this, 0, new Intent("PLAY_PAUSE"), PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
//        PendingIntent nextIntent = PendingIntent.getBroadcast(this, 0, new Intent("NEXT"), PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
//        PendingIntent previousIntent = PendingIntent.getBroadcast(this, 0, new Intent("PREVIOUS"), PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.music_icon)
//                .setContentTitle(listOfSongs.get(position).getTitle())
//                .setContentIntent(openActivityPendingIntent)
//                .addAction(R.drawable.previous, "Previous", previousIntent)
//                .addAction(mediaPlayer.isPlaying() ? R.drawable.pause : R.drawable.play, "Play/Pause", playPauseIntent)
//                .addAction(R.drawable.next, "Next", nextIntent)
//                .setPriority(NotificationCompat.PRIORITY_LOW)
//                .setOnlyAlertOnce(true);
//
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        notificationManager.notify(NOTIFICATION_ID, builder.build());
//    }
//
//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Music Notification", NotificationManager.IMPORTANCE_LOW);
//            NotificationManager manager = getSystemService(NotificationManager.class);
//            manager.createNotificationChannel(channel);
//        }
//    }
//
//    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (action != null) {
//                switch (action) {
//                    case "PLAY_PAUSE":
//                        if (mediaPlayer.isPlaying()) {
//                            mediaPlayer.pause();
//                        } else {
//                            mediaPlayer.start();
//                        }
//                        showNotification();
//                        break;
//                    case "NEXT":
//                        playNext();
//                        showNotification();
//                        break;
//                    case "PREVIOUS":
//                        playPrevious();
//                        showNotification();
//                        break;
//                    default:
//                        Intent openMusicPlayerIntent=new Intent(MusicNotificationService.this, PlayerActivity.class);
//                        intent.putExtra("Title", listOfSongs.get(position).getTitle());
//                        intent.putExtra("Position", position);
//                        listOfSongs.get(position).setPlaying(true);
//                        startActivity(intent);
//                }
//            }
//        }
//    };
//    // Method to play the previous song
//    private void playPrevious() {
//        if(mediaPlayer!=null) {
//            mediaPlayer.stop();
//            mediaPlayer.release();
//        }
//        if(position>0)
//                position=position-1;
//            else
//                position=listOfSongs.size()-1;
//        //third case remains shuffle=false && loopOne=true...means automatically song will be in loop if these two conditions false
//        uri = Uri.parse(listOfSongs.get(position).getPath());
//        initializeMediaPlayer();//it initialize and ensures that next song will automatically play after this song completes
//    }
//    // Method to play the next song
//    private void playNext() {
//        if(mediaPlayer!=null) {
//            mediaPlayer.stop();
//            mediaPlayer.release();
//        }
//            position = (position + 1) % listOfSongs.size();
//
//        uri = Uri.parse(listOfSongs.get(position).getPath());
//        initializeMediaPlayer();
//    }
//
//    private void initializeMediaPlayer() {
//        if (mediaPlayer != null) {
//            mediaPlayer.release();
//        }
//        mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
//        if (mediaPlayer != null) {
//            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mp) {
////                        initializeMediaPlayer(); // if loop is on then restart the same song
//                        playNext(); // if loop is off then play next song after completing previous song
//                }
//            });
//            MainActivity.currentPlayingPosition = PlayerActivity.position;
//        } else {
//            Toast.makeText(this, "Unable to play this file", Toast.LENGTH_SHORT).show();
//        }
//    }
//}
