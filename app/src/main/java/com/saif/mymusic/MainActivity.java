package com.saif.mymusic;

import static com.saif.mymusic.MusicService.LAST_MUSIC_FILE_ARTIST;
import static com.saif.mymusic.MusicService.LAST_MUSIC_FILE_NAME;
import static com.saif.mymusic.MusicService.LAST_MUSIC_FILE_PATH;
import static com.saif.mymusic.MusicService.LAST_MUSIC_FILE_POSITION;
import static com.saif.mymusic.MusicService.LAST_MUSIC_FILE_PROGRESS;
import static com.saif.mymusic.MusicService.LAST_PLAYED_MUSIC;
import static com.saif.mymusic.MyMusicPlayerPermissions.REQUEST_MEDIA_PERMISSION;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    public static final int REQUEST_CODE = 1;
    static ArrayList<MusicFiles> musicFiles;
    RecyclerView recyclerView;
    TextView noOfSongs;
    TextView song_postfix;
    TextView miniPlayerSongName;
    MusicAdapter musicAdapter;
    static boolean shuffleButton = false, loopOneButton = false;
    private final String MY_SORT_PREF = "SortOrder";
    private ActionMode actionMode;
    FrameLayout miniPlayer;
    public static int currentPlayingPosition = -1;
    private MyMusicPlayerPermissions myMusicPlayerPermissions;
    public static boolean SHOW_MINI_PLAYER = false;
    public static String PATH_TO_MINI_PLAYER = null;
    public static String LAST_MUSIC_NAME = null;
    public static String LAST_MUSIC_ARTIST = null;
    public static int LAST_MUSIC_POSITION = 0;
    public static int LAST_MUSIC_PROGRESS = 0;
    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode=mode;
            mode.getMenuInflater().inflate(R.menu.search, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.findItem(R.id.action_select_all).setVisible(true);
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
            } else if (R.id.action_select_all == item.getItemId()) {
                musicAdapter.selectAll();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            musicAdapter.exitMultiSelectMode();
            actionMode = null;
            Menu menu = mode.getMenu();
            menu.findItem(R.id.action_share).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
            menu.findItem(R.id.action_select_all).setVisible(false);
            menu.findItem(R.id.sort_button).setVisible(true);
            menu.findItem(R.id.search_bar).setVisible(true);
        }
    };
    public void updateActionModeTitle() {
        if (actionMode != null) {
            int selectedCount = musicAdapter.getSelectedItemCount();
            if(selectedCount==0){
                actionMode.finish();
                actionMode=null;
            }else {
                actionMode.setTitle(selectedCount + " selected");
            }
        }else {
            Toast.makeText(this, " action mode null", Toast.LENGTH_SHORT).show();
        }
    }
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
        miniPlayerSongName = findViewById(R.id.mini_player_song_name);
        myMusicPlayerPermissions=new MyMusicPlayerPermissions();
        miniPlayer = findViewById(R.id.mini_player_container);
        if (getSupportFragmentManager().findFragmentById(R.id.mini_player_container) == null) {
            MiniPlayerFragment miniPlayerFragment = new MiniPlayerFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.mini_player_container, miniPlayerFragment)
                    .commit();
        }

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
        if(myMusicPlayerPermissions.isMediaOk(this)){
            setupMusicFiles();
        }else {
            if (!myMusicPlayerPermissions.isMediaOk(this)) {
                myMusicPlayerPermissions.requestMediaPermission(this);
            }
        }
    }

    private void setupMusicFiles() {
        musicFiles = getAllAudio(this);
//        scanMedia("/storage/emulated/0/Download");
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
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.SIZE
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
                long size = cursor.getLong(6);

                MusicFiles musicFile = new MusicFiles(path, title, artist, duration, id,size,false);
//                Log.d("MusicFiles", "Found music file: " + title + " at " + path); // Add this line
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
        if (searchView != null) {
            searchView.setOnQueryTextListener(this);
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MEDIA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                setupMusicFiles();
            } else {
                // Permission denied
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // User has denied the permission and selected "Don't ask again"
                    showPermissionDeniedDialog();
                }
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Storage permissions are required to access music files. Please enable them in app settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences=getSharedPreferences(LAST_PLAYED_MUSIC,MODE_PRIVATE);
        String lastPlayedSong=preferences.getString(LAST_MUSIC_FILE_PATH,null);
        LAST_MUSIC_NAME=preferences.getString(LAST_MUSIC_FILE_NAME,null);
        LAST_MUSIC_ARTIST=preferences.getString(LAST_MUSIC_FILE_ARTIST,null);
        LAST_MUSIC_POSITION= preferences.getInt(LAST_MUSIC_FILE_POSITION,0);
        LAST_MUSIC_PROGRESS= preferences.getInt(LAST_MUSIC_FILE_PROGRESS,0);
        Log.e("MainActivity", "onResume:song stored in preference: "+LAST_MUSIC_NAME);
        Log.e("MainActivity", "onResume:song position in preference: "+LAST_MUSIC_POSITION);
        Log.e("MainActivity", "onResume:song progress stored in preference: "+LAST_MUSIC_PROGRESS);
        if(lastPlayedSong!=null){
            SHOW_MINI_PLAYER=true;
            PATH_TO_MINI_PLAYER=lastPlayedSong;
        }else {
            SHOW_MINI_PLAYER=false;
            PATH_TO_MINI_PLAYER=null;
        }
        Log.e("MainActivity","onResume: lastSongPath is: "+lastPlayedSong+" mini player is: "+SHOW_MINI_PLAYER);
    }
}
