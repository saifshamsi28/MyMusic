package com.saif.mymusic;

import static com.saif.mymusic.MusicService.LAST_MUSIC_FILE_ARTIST;
import static com.saif.mymusic.MusicService.LAST_MUSIC_FILE_NAME;
import static com.saif.mymusic.MusicService.LAST_MUSIC_FILE_PATH;
import static com.saif.mymusic.MusicService.LAST_MUSIC_FILE_POSITION;
import static com.saif.mymusic.MusicService.LAST_MUSIC_FILE_PROGRESS;
import static com.saif.mymusic.MusicService.LAST_PLAYED_MUSIC;
import static com.saif.mymusic.MyMusicPlayerPermissions.REQUEST_MEDIA_PERMISSION;
import android.Manifest;
import android.content.BroadcastReceiver;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener,AdapterView.OnItemSelectedListener {
    public static final int REQUEST_CODE = 1;
    static ArrayList<MusicFiles> musicFiles;
    RecyclerView recyclerView;
    TextView noOfSongs;
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
    private Spinner categorySpinner;
    private String currentSortOrder ;
    private String currentCategory ;
    private DividerItemDecoration dividerItemDecoration;
    private boolean isUserAction = false;

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode=mode;
            mode.getMenuInflater().inflate(R.menu.search, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
//            int selectedCount = musicAdapter.getSelectedItemCount();
            menu.findItem(R.id.action_select_all).setVisible(true);
            menu.findItem(R.id.action_delete).setVisible(true);
            menu.findItem(R.id.action_share).setVisible(true);
            menu.findItem(R.id.sort_button).setVisible(false);
            menu.findItem(R.id.refresh_button).setVisible(false);
            menu.findItem(R.id.search_bar).setVisible(false);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (R.id.action_delete == item.getItemId()) {
                deleteSelectedItems();
                mode.finish();
                return true;
            } else if (R.id.action_info == item.getItemId()) {
                showDetails();
                mode.finish();
                return true;
            }else if (R.id.action_share == item.getItemId()) {
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
            musicAdapter.selectedItems.clear();
            actionMode = null;
            Menu menu = mode.getMenu();
            menu.findItem(R.id.action_share).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
            menu.findItem(R.id.action_select_all).setVisible(false);
            menu.findItem(R.id.sort_button).setVisible(true);
            menu.findItem(R.id.search_bar).setVisible(true);
            menu.findItem(R.id.refresh_button).setVisible(true);
            menu.findItem(R.id.action_info).setVisible(false);

        }
    };
    public void updateActionModeTitle() {
        if (actionMode != null) {
            int selectedCount = musicAdapter.getSelectedItemCount();
            Menu menu = actionMode.getMenu();
            if (selectedCount == 1) {
                menu.findItem(R.id.action_info).setVisible(true);
            } else {
                menu.findItem(R.id.action_info).setVisible(false);
            }
            if(selectedCount==0){
                actionMode.finish();
                musicAdapter.selectedItems.clear();
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

    private void showDetails() {
        // Get the metadata of the first selected item (assuming only one selection for info)
        MusicFiles selectedSong = musicAdapter.getSelectedSong();

        if (selectedSong != null) {
            // Extract metadata from the selected song
            String songTitle = selectedSong.getTitle();
            String songArtist = selectedSong.getArtist();
            String songDuration = selectedSong.getDuration();
            String songSize = String.valueOf(selectedSong.getSize());
            String songLocation = selectedSong.getPath();
            String albumArtPath = selectedSong.getPath(); // Assuming it's a URI
            String dateAdded = String.valueOf(selectedSong.getDateAdded());
            String dateModified = String.valueOf(selectedSong.getDateModified());
            String songAlbum = selectedSong.getAlbum();

            // Create the intent and pass the metadata
            Intent intent = new Intent(this, SongInfoActivity.class);
            intent.putExtra("TITLE", songTitle);
            intent.putExtra("ARTIST", songArtist);
            intent.putExtra("DURATION", songDuration);
            intent.putExtra("SIZE", songSize);
            intent.putExtra("LOCATION", songLocation);
            intent.putExtra("ALBUM_ART_PATH", albumArtPath);
            intent.putExtra("DATE_ADDED", dateAdded);
            intent.putExtra("DATE_MODIFIED", dateModified);
            intent.putExtra("ALBUM", songAlbum);
            musicAdapter.selectedItems.clear();
            // Start the SongInfoActivity
            startActivity(intent);
        } else {
            Toast.makeText(this, "No song selected for info.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //to set the status bar color
        getWindow().setStatusBarColor(getResources().getColor(R.color.GreenishBlue));
        recyclerView = findViewById(R.id.music_list);
        noOfSongs = findViewById(R.id.no_of_songs);
        miniPlayerSongName = findViewById(R.id.mini_player_song_name);
        categorySpinner = findViewById(R.id.category_spinner);
        myMusicPlayerPermissions=new MyMusicPlayerPermissions();
        miniPlayer = findViewById(R.id.mini_player_container);
        if (getSupportFragmentManager().findFragmentById(R.id.mini_player_container) == null) {
            MiniPlayerFragment miniPlayerFragment = new MiniPlayerFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.mini_player_container, miniPlayerFragment)
                    .commit();
        }
        musicFiles = new ArrayList<>();
        SharedPreferences preferences = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE);
        currentSortOrder = preferences.getString("sorting", "sortByDate");
        currentCategory = preferences.getString("category", "All");
        requestPermissions();

        isUserAction = false;
        categorySpinner.setOnItemSelectedListener(this);
        setSpinnerSelection(currentCategory);
        isUserAction = true;
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
                setupMusicFiles(currentSortOrder);
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
            setupMusicFiles(currentSortOrder);
        }else {
            if (!myMusicPlayerPermissions.isMediaOk(this)) {
                myMusicPlayerPermissions.requestMediaPermission(this);
            }
        }
    }

    private void setupMusicFiles(String currentSortOrder) {
        musicFiles = getAllAudio(this,currentSortOrder);
        // Update the adapter's list as well
        if (musicAdapter != null) {
            musicAdapter.updateMusicFiles(musicFiles);
        }
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        if (musicFiles != null && !musicFiles.isEmpty()) {
            ContentResolver contentResolver = getContentResolver();
            musicAdapter = new MusicAdapter(MainActivity.this, contentResolver, musicFiles);
            recyclerView.setAdapter(musicAdapter);
            musicAdapter.notifyDataSetChanged();
            recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
            // Initialize dividerItemDecoration only once
            if (dividerItemDecoration == null) {
                dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
                recyclerView.addItemDecoration(dividerItemDecoration);
            }
            noOfSongs.setText(musicFiles.size()+" songs");
        } else {
            noOfSongs.setText("No music files found");
        }
    }

    public ArrayList<MusicFiles> getAllAudio(Context context, String currentSortOrder) {
        String order = null;
        ArrayList<MusicFiles> tempAudioList = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        switch (currentSortOrder) {
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
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED
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
                long dateAdded = cursor.getLong(7);
                long dateModified = cursor.getLong(8);
                Log.d("MusicFiles", "Date added: " + dateAdded + " album name " + album);

                MusicFiles musicFile = new MusicFiles(path, title, artist,album, duration, id,size,dateAdded,dateModified,false);
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
            searchView.setQueryHint("Search by Title or Artist");
            searchView.setOnQueryTextListener(this);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE).edit();
        if (item.getItemId() == R.id.sort_by_name) {
            currentSortOrder = "sortByName";
            editor.putString("sorting", currentSortOrder);
            currentCategory="All";
            editor.apply();
            setupMusicFiles(currentSortOrder);
            categorySpinner.setSelection(0);
            Toast.makeText(this, "Sorted by Name " , Toast.LENGTH_SHORT).show();
        } else if (item.getItemId() == R.id.sort_by_date) {
            currentSortOrder = "sortByDate";
            editor.putString("sorting", currentSortOrder);
            editor.apply();
            currentCategory="All";
            categorySpinner.setSelection(0);
            setupMusicFiles(currentSortOrder);
            Toast.makeText(this, "Sorted by Date", Toast.LENGTH_SHORT).show();
        } else if (item.getItemId() == R.id.sort_by_size) {
            currentSortOrder = "sortBySize";
            editor.putString("sorting", currentSortOrder);
            editor.apply();
            currentCategory="All";
            categorySpinner.setSelection(0);
            setupMusicFiles(currentSortOrder);
            Toast.makeText(this, "Sorted by Size", Toast.LENGTH_SHORT).show();
        }else if(item.getItemId()==R.id.refresh_button){
            //refresh the current list to reflect any changes recently made
            MediaScannerConnection.scanFile(this, new String[]{currentCategory}, null, null);
            musicFiles = getAllAudio(this, currentSortOrder);
            // Filter the files by the currently selected category
            filterMusicFilesByCategory(currentCategory);
            Toast.makeText(this, "Refreshed: "+currentCategory, Toast.LENGTH_SHORT).show();
        }
//        Toast.makeText(this, "Sorting by: " + currentSortOrder, Toast.LENGTH_SHORT).show();
        //only call this method when any of the sort options are selected
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

        if(!newText.isEmpty()){
            for (MusicFiles song : musicFiles) {
                if (song.getTitle().toLowerCase().contains(userInput) || song.getArtist().toLowerCase().contains(userInput)) {
                    filteredFiles.add(song);
                }
            }
            //to show the number of songs found on top
            noOfSongs.setText(filteredFiles.size()+" items matched");
            //to align the this textview to the center
            noOfSongs.setGravity(Gravity.CENTER);
        }else {
            //to sort by stored category
            filterMusicFilesByCategory(currentCategory);
            noOfSongs.setGravity(Gravity.CENTER);
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
                setupMusicFiles(currentSortOrder);
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(metadataChangeReceiver);
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

    private final BroadcastReceiver metadataChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null && intent.getAction().equals("com.saif.mymusic.METADATA_CHANGED")) {
                String newTitle = intent.getStringExtra("TITLE");
                String newArtist = intent.getStringExtra("ARTIST");

                musicFiles.get(currentPlayingPosition).setTitle(newTitle);
                musicFiles.get(currentPlayingPosition).setArtist(newArtist);
                musicAdapter.notifyItemChanged(currentPlayingPosition);

                // Update listOfSongs if necessary
                if (currentPlayingPosition != -1 && currentPlayingPosition < musicFiles.size()) {
                    musicFiles.get(currentPlayingPosition).setTitle(newTitle);
                    musicFiles.get(currentPlayingPosition).setArtist(newArtist);
                }
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(metadataChangeReceiver);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Get selected category
        String category = parent.getItemAtPosition(position).toString();
        // Save the selected category in SharedPreferences
        SharedPreferences.Editor editor = getSharedPreferences(MY_SORT_PREF, MODE_PRIVATE).edit();
        editor.putString("category", category);
        currentCategory = category;
        editor.apply();
        // Filter the music files by the selected category
        filterMusicFilesByCategory(category);

        if (isUserAction) {
            Toast.makeText(this, "Filtered by: " + category, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Default to all music files if nothing is selected
        musicFiles = getAllAudio(this, currentSortOrder);
        musicAdapter.updateMusicFiles(musicFiles);
    }

    private void setSpinnerSelection(String category) {
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) categorySpinner.getAdapter();
        if (adapter != null) {
            int position = adapter.getPosition(category);
            if (position >= 0) {
                categorySpinner.setSelection(position);
                onItemSelected(categorySpinner, null, position, 0);
            }
        }
    }

    private void filterMusicFilesByCategory(String category) {
        ArrayList<MusicFiles> filteredList = new ArrayList<>();
        //using switch case to check for categories
        switch (category){
            case "Songs":
                for (MusicFiles file : musicFiles) {
                    if (!file.getPath().contains("WhatsApp Audio") && !file.getPath().contains("Call")) {
                        filteredList.add(file);
                    }
                }
                noOfSongs.setText(filteredList.size()+" songs");
                break;
            case "WhatsApp Voice Recordings":
                    for (MusicFiles file : musicFiles) {
                        if (file.getPath().contains("WhatsApp Audio")) {
                            filteredList.add(file);
                        }
                    }
                    noOfSongs.setText(filteredList.size()+" Voice Recordings");
                    //to truncate the long text
                    noOfSongs.setEllipsize(TextUtils.TruncateAt.END);
                    break;
            case "Call Recordings":
                for (MusicFiles file : musicFiles) {
                    if (file.getPath() == null || file.getPath().contains("Call")) {
                        filteredList.add(file);
                    }
                }
                noOfSongs.setText(filteredList.size()+" Call Recordings");
                noOfSongs.setEllipsize(TextUtils.TruncateAt.END);
                break;
            default:
                filteredList.addAll(musicFiles);
                noOfSongs.setText(filteredList.size()+" songs");
        }
        musicAdapter.updateMusicFiles(filteredList);
    }
}
