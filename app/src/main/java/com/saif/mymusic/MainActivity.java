package com.saif.mymusic;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    public static final int REQUEST_CODE = 1;
    static ArrayList<MusicFiles> musicFiles;
    RecyclerView recyclerView;
    TextView noOfSongs, song_postfix;
    static MusicAdapter musicAdapter;
    static boolean shuffleButton = false, loopOneButton = false;
    private final String MY_SORT_PREF = "SortOrder";
    private ActionMode actionMode;
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.search, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.findItem(R.id.action_delete).setVisible(true);
            menu.findItem(R.id.action_share).setVisible(true);
            menu.findItem(R.id.sort_button).setVisible(false);
            menu.findItem(R.id.search_bar).setVisible(false);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (R.id.action_delete == item.getItemId()) {
                deleteSelectedItems();
                mode.finish();
                return true;
            } else if (R.id.action_share == item.getItemId()) {
                shareSelectedItems();
                mode.finish();
                return true;
            } else
                return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            musicAdapter.exitMultiSelectMode();
            actionMode = null;
            Menu menu = mode.getMenu();
            menu.findItem(R.id.action_share).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
            menu.findItem(R.id.sort_button).setVisible(true);
            menu.findItem(R.id.search_bar).setVisible(true);
        }
    };

    public ActionMode.Callback getActionModeCallback() {
        return actionModeCallback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.music_list);
        noOfSongs = findViewById(R.id.song_number);
        song_postfix = findViewById(R.id.songs_header);
        musicFiles = new ArrayList<>();
        requestPermissions();
    }

    private void deleteSelectedItems() {
        ArrayList<MusicFiles> selectedItems = new ArrayList<>();
        for (MusicFiles musicFile : musicFiles) {
            if (musicFile.isSelected()) {
                selectedItems.add(musicFile);
            }
        }

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }
        View view = recyclerView.getChildAt(0);
        musicAdapter.deleteSongs(selectedItems, view);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                setupMusicFiles();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void shareSelectedItems() {
        ArrayList<MusicFiles> selectedItems = new ArrayList<>();
        for (MusicFiles musicFile : musicFiles) {
            if (musicFile.isSelected()) {
                selectedItems.add(musicFile);
            }
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType("audio/*");
        ArrayList<Uri> uris = new ArrayList<>();
        for (MusicFiles musicFile : selectedItems) {
            uris.add(Uri.parse(musicFile.getPath()));
        }
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        startActivity(Intent.createChooser(shareIntent, "Share music files"));
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_CODE);
            } else {
                setupMusicFiles();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
            } else {
                setupMusicFiles();
            }
        } else {
            setupMusicFiles();
        }
    }

    private void setupMusicFiles() {
        musicFiles = getAllAudio(this);
        scanMedia("/storage/emulated/0/Download");
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        if (musicFiles != null && !musicFiles.isEmpty()) {
            ContentResolver contentResolver = getContentResolver();
            musicAdapter = new MusicAdapter(MainActivity.this, contentResolver, musicFiles);
            recyclerView.setAdapter(musicAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
            recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
            noOfSongs.setText(String.valueOf(musicFiles.size()));
            song_postfix.setVisibility(View.VISIBLE);
        } else {
            noOfSongs.setText("No music files found");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                setupMusicFiles();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public ArrayList<MusicFiles> getAllAudio(Context context) {
        SharedPreferences preferences = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE);
        String sortOrder = preferences.getString("sorting", "sortByName");
        String order = null;
        ArrayList<MusicFiles> tempAudioList = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        switch (sortOrder) {
            case "sortByName":
                order = MediaStore.MediaColumns.DISPLAY_NAME + " ASC";
                break;
            case "sortByDate":
                order = MediaStore.MediaColumns.DATE_ADDED + " DESC";
                break;
            case "sortBySize":
                order = MediaStore.MediaColumns.SIZE + " DESC";
                break;
        }

        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media._ID
        };
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, order);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                String duration = cursor.getString(1);
                String path = cursor.getString(2);
                String artist = cursor.getString(3);
                String album = cursor.getString(4);
                String id = cursor.getString(5);

                MusicFiles musicFile = new MusicFiles(path, title, artist, duration, id);
                Log.d("MusicFiles", "Found music file: " + title + " at " + path); // Add this line
                tempAudioList.add(musicFile);
            }
            cursor.close();
        } else {
            Log.d("MusicFiles", "Cursor is null, no music files found");
        }
        return tempAudioList;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        MenuItem searchItem = menu.findItem(R.id.search_bar);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE).edit();
        if (item.getItemId() == R.id.sort_by_name) {
            editor.putString("sorting", "sortByName");
            editor.apply();
            this.recreate();
        } else if (item.getItemId() == R.id.sort_by_date) {
            editor.putString("sorting", "sortByDate");
            editor.apply();
            this.recreate();
        } else if (item.getItemId() == R.id.sort_by_size) {
            editor.putString("sorting", "sortBySize");
            editor.apply();
            this.recreate();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String userInput = newText.toLowerCase();
        ArrayList<MusicFiles> filteredFiles = new ArrayList<>();

        for (MusicFiles song : musicFiles) {
            if (song.getTitle().toLowerCase().contains(userInput)) {
                filteredFiles.add(song);
            }
        }

        MusicAdapter newAdapter = new MusicAdapter(MainActivity.this, getContentResolver(), filteredFiles);
        recyclerView.setAdapter(newAdapter);
        return true;
    }

    private void scanMedia(String path) {
        MediaScannerConnection.scanFile(this, new String[]{path}, null, (scannedPath, uri) -> {
            Log.d("MediaScanner", "Scanned " + scannedPath + ":");
            Log.d("MediaScanner", "-> uri=" + uri);
        });
    }
}
