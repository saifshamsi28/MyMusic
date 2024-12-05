package com.saif.mymusic;

import static com.saif.mymusic.MusicAdapter.getAlbumArt;

import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class SongInfoActivity extends AppCompatActivity {
    private EditText songTitle, songArtist,songAlbum;
    private TextView songDuration, songSize, songLocation, songFormat,songAddedDate,infoTitle;
    private CircleImageView songAlbumArt;
    private String title, artist, duration, albumArtPath,albumName, location;
    private long size,dateAdded;
    private ImageView editButton, saveButton,cancelButton;
    private MyMusicPermissions myMusicPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_info);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.GreenishBlue));
        setTitle("Song Info");
        // Initialize views
        songTitle = findViewById(R.id.song_title);
        songTitle.setEllipsize(TextUtils.TruncateAt.END);
        songArtist = findViewById(R.id.song_artist);
        songDuration = findViewById(R.id.song_duration);
        songSize = findViewById(R.id.song_size);
        songAlbumArt = findViewById(R.id.song_album_art);
        songAlbum = findViewById(R.id.song_album);
        songLocation = findViewById(R.id.song_location_field);
        editButton = findViewById(R.id.edit_button);
        saveButton = findViewById(R.id.save_button);
        songFormat = findViewById(R.id.song_format);
        cancelButton = findViewById(R.id.cancel_button);
        infoTitle = findViewById(R.id.info_title);
        songAddedDate = findViewById(R.id.date_added);

        // Get data from intent
        Intent intent = getIntent();
        title = intent.getStringExtra("TITLE");
        artist = intent.getStringExtra("ARTIST");
        duration = intent.getStringExtra("DURATION");
        size = Long.parseLong(intent.getStringExtra("SIZE"));
        location = intent.getStringExtra("LOCATION");
        albumArtPath = intent.getStringExtra("ALBUM_ART_PATH");
        dateAdded = Long.parseLong(intent.getStringExtra("DATE_ADDED"));
        albumName = intent.getStringExtra("ALBUM");
        myMusicPermissions = new MyMusicPermissions();

        enableEditing(false);
        if(!isMp3File(location)){
            editButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.GONE);
        }


        //to find the format of the file by .
        int lastDotIndex = location.lastIndexOf(".");
        if (lastDotIndex != -1) {
            String format = "."+location.substring(lastDotIndex + 1);
            songFormat.setText(format);
        }

        // Set data to views
        songTitle.setText(title);
        songArtist.setText(artist);
        songAlbum.setText(albumName);
        songDuration.setText(formatSongDuration(Integer.parseInt(duration)));
        songSize.setText(formatMusicFileSize(size));
        songLocation.setText(location);
        songAddedDate.setText(formatedDate(dateAdded));

        if (albumArtPath != null) {
            try {
                byte[] albumArt = getAlbumArt(albumArtPath);
                if (albumArt != null) {
                    Glide.with(this)
                            .load(albumArt)
                            .placeholder(R.drawable.logo)
                            .into(songAlbumArt);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Handle pencil (edit) button click
        editButton.setOnClickListener(v -> {
            if (myMusicPermissions.isMediaPermissionGranted(this)) {
                enableEditing(true);
            } else {
                myMusicPermissions.requestMediaPermission(this);
                Toast.makeText(this, "Permission required to enable editing.", Toast.LENGTH_SHORT).show();
            }
        });

        saveButton.setOnClickListener(v -> {
            String updatedTitle = songTitle.getText().toString();
            String updatedArtist = songArtist.getText().toString();
            String updatedAlbum = songAlbum.getText().toString();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                updateSongMetadataWithPermission(location, updatedArtist, updatedTitle, updatedAlbum);
            } else {
                Toast.makeText(this, "Permission required to save changes.", Toast.LENGTH_SHORT).show();
            }
        });
        cancelButton.setOnClickListener(v -> {
            enableEditing(false);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200) { // Recoverable security exception result
            if (resultCode == RESULT_OK) {
                myMusicPermissions.setPermissionGranted(this, true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    updateSongMetadataWithPermission(location, songArtist.getText().toString(), songTitle.getText().toString(), songAlbum.getText().toString());
                }
            } else {
                Toast.makeText(this, "Permission denied. Cannot update metadata.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MyMusicPermissions.REQUEST_MEDIA_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                myMusicPermissions.setPermissionGranted(this, true);
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableEditing(boolean enable) {
        if (!enable) {
            songTitle.setEnabled(false);
            songArtist.setEnabled(false);
            songTitle.setFocusable(false);
            songArtist.setFocusable(false);
            songTitle.setFocusableInTouchMode(false);
            songArtist.setFocusableInTouchMode(false);
            songTitle.setClickable(false);
            songArtist.setClickable(false);
            songAlbum.setEnabled(false);
            songAlbum.setFocusable(false);
            songAlbum.setFocusableInTouchMode(false);
            songAlbum.setClickable(false);
            cancelButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.GONE);
            editButton.setVisibility(View.VISIBLE);
            infoTitle.setVisibility(View.GONE);
            songTitle.setMaxLines(2);
            songTitle.setLines(2);
            songTitle.setSelection(0);
            songTitle.setEllipsize(TextUtils.TruncateAt.END);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.GreenishBlue));
            //show title bar
            if(getSupportActionBar()!=null)
                getSupportActionBar().show();
            else
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
        }else {
            songTitle.setEnabled(true);
            songArtist.setEnabled(true);
            songTitle.setFocusable(true);
            songArtist.setFocusable(true);
            songTitle.setFocusableInTouchMode(true);
            songArtist.setFocusableInTouchMode(true);
            songTitle.setClickable(true);
            songArtist.setClickable(true);
            songAlbum.setEnabled(true);
            songAlbum.setFocusable(true);
            songAlbum.setFocusableInTouchMode(true);
            songAlbum.setClickable(true);
            saveButton.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
            infoTitle.setVisibility(View.VISIBLE);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            //hide title bar
            if(getSupportActionBar()!=null)
                getSupportActionBar().hide();
            else
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
            editButton.setVisibility(View.GONE);

            // Enable multiline editing, focus, and open keyboard
            songTitle.setSingleLine(false);
            songTitle.setMaxLines(Integer.MAX_VALUE);
            songTitle.setEllipsize(null);
            songTitle.setSelection(songTitle.getText().length());
            songTitle.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void updateSongMetadataWithPermission(String filePath, String newArtist, String newTitle, String updatedAlbum) {
        try {
            Uri fileUri = getUriFromFilePath(filePath);
            if (fileUri == null) {
                Toast.makeText(this, "Invalid file URI!", Toast.LENGTH_SHORT).show();
                return;
            }

            String tempFilePath = getCacheDir() + "/temp_audio.mp3";
            Mp3File mp3File = getMp3File(filePath, newArtist, newTitle, updatedAlbum);
            mp3File.save(tempFilePath);

            try (InputStream inputStream = new FileInputStream(tempFilePath);
                 OutputStream outputStream = getContentResolver().openOutputStream(fileUri)) {
                if (outputStream != null) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    Toast.makeText(this, "Metadata updated successfully!", Toast.LENGTH_SHORT).show();
                    enableEditing(false);
                    sendBroadcastToUpdateUi(newTitle,newArtist);
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).setData(fileUri));
                    // Refresh the media library
                    MediaScannerConnection.scanFile(this, new String[]{filePath}, null,
                            (path, uri) -> Log.d("MediaScanner", "Scanned: " + path + ", URI: " + uri));
                } else {
                    Toast.makeText(this, "Failed to access original file for writing.", Toast.LENGTH_SHORT).show();
                }
            }

        } catch (RecoverableSecurityException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PendingIntent pendingIntent = e.getUserAction().getActionIntent();
                try {
                    startIntentSenderForResult(pendingIntent.getIntentSender(), 200, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException ex) {
                    ex.printStackTrace();
                    Toast.makeText(this, "Permission request failed.", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error updating metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendBroadcastToUpdateUi(String newTitle, String newArtist) {
        Intent intent = new Intent("com.saif.mymusic.METADATA_CHANGED");
        intent.putExtra("TITLE", newTitle);
        intent.putExtra("ARTIST", newArtist);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    private Uri getUriFromFilePath(String filePath) {
        ContentResolver resolver = getContentResolver();
        Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.DATA + "=?";
        String[] selectionArgs = new String[]{filePath};
        Cursor cursor = resolver.query(contentUri, null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getColumnIndex(MediaStore.MediaColumns._ID);
            Uri uri = Uri.withAppendedPath(contentUri, cursor.getString(id));
            cursor.close();
            return uri;
        }
        return null;
    }

    private boolean isMp3File(String filePath) {
        return filePath.toLowerCase().endsWith(".mp3");
    }

    private static @NonNull Mp3File getMp3File(String filePath, String newArtist, String newTitle, String updatedAlbum)
            throws IOException, UnsupportedTagException, InvalidDataException {
        Mp3File mp3File = new Mp3File(filePath);

        if (!mp3File.hasId3v2Tag() && !mp3File.hasId3v1Tag()) {
            // Create a new ID3v2 tag if none exists
            ID3v2 id3v2Tag = new ID3v24Tag();
            mp3File.setId3v2Tag(id3v2Tag);
            Log.d("getMp3File", "Created new ID3v2 tag for the file.");
        }

        // Update metadata
        if (mp3File.hasId3v2Tag()) {
            ID3v2 id3v2Tag = mp3File.getId3v2Tag();
            if (newArtist != null && !newArtist.isEmpty()) {
                id3v2Tag.setArtist(newArtist);
            }
            if (newTitle != null && !newTitle.isEmpty()) {
                id3v2Tag.setTitle(newTitle);
            }
            if (updatedAlbum != null && !updatedAlbum.isEmpty()) {
                id3v2Tag.setAlbum(updatedAlbum);
            }
        } else if (mp3File.hasId3v1Tag()) {
            ID3v1 id3v1Tag = mp3File.getId3v1Tag();
            if (newArtist != null && !newArtist.isEmpty()) {
                id3v1Tag.setArtist(newArtist);
            }
            if (newTitle != null && !newTitle.isEmpty()) {
                id3v1Tag.setTitle(newTitle);
            }
            if (updatedAlbum != null && !updatedAlbum.isEmpty()) {
                id3v1Tag.setAlbum(updatedAlbum);
            }
        }

        return mp3File;
    }

    public String formatMusicFileSize(long sizeInBytes) {
        final long KILOBYTE = 1024;
        final long MEGABYTE = KILOBYTE * 1024;

        if (sizeInBytes >= MEGABYTE) {
            double sizeInMb = (double) sizeInBytes / MEGABYTE;
            return String.format("%.2f MB", sizeInMb);
        } else {
            double sizeInKb = (double) sizeInBytes / KILOBYTE;
            return String.format("%.2f KB", sizeInKb);
        }
    }

    public String formatSongDuration(int currentPosition){
        currentPosition/=1000; //to get time in seconds
        String seconds=String.valueOf(currentPosition % 60);
        String minutes=String.valueOf(currentPosition / 60);
        String totalOut = minutes + ":" + seconds;
        String totalNew = minutes + ":0" + seconds;
        if(seconds.length()==1){
            return totalNew;
        }
        return totalOut;
    }

    private String formatedDate(long dateAdded) {
        dateAdded*=1000L; //to make date in milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = new Date(dateAdded);
        return sdf.format(date);
    }

}

