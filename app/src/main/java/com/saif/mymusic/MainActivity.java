package com.saif.mymusic;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    public static final int REQUEST_CODE = 1;
    static ArrayList<MusicFiles> musicFiles;
    RecyclerView recyclerView;
    TextView noOfSongs;
     static MusicAdapter musicAdapter;
    static boolean shuffleButton=false,loopOneButton=false;
    private final String MY_SORT_PREF="SortOrder";
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
                if( R.id.action_delete == item.getItemId()) {
                    deleteSelectedItems();
                    mode.finish();
                    return true;
                }
                else if( R.id.action_share == item.getItemId()) {
                    shareSelectedItems();
                    mode.finish();
                    return true;
                }else
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
    public ActionMode.Callback getActionModeCallback(){
        return actionModeCallback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView=findViewById(R.id.music_list);
        noOfSongs=findViewById(R.id.song_number);
            requestStoragePermission();
            if(!(musicFiles.size() <1)) {
                ContentResolver contentResolver = getContentResolver();
                musicAdapter = new MusicAdapter(MainActivity.this,contentResolver,musicFiles);
                recyclerView.setAdapter(musicAdapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this,LinearLayoutManager.VERTICAL,false));
                recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
            }
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
        if (requestCode == 1000) {
            if (resultCode == RESULT_OK) {
                // Permission granted, delete the files
                ArrayList<MusicFiles> deletedFiles = new ArrayList<>();
                for (MusicFiles musicFile : musicAdapter.mFiles) {
                    if (musicFile.isSelected()) {
                        deletedFiles.add(musicFile);
                    }
                }
                int numDeleted = deletedFiles.size();
                for (MusicFiles musicFile : deletedFiles) {
                    musicAdapter.mFiles.remove(musicFile);
                }
                musicAdapter.notifyDataSetChanged();

                Toast.makeText(this, "song deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Permission denied to delete files", Toast.LENGTH_SHORT).show();
            }
        }
        noOfSongs.setText(String.valueOf(musicFiles.size()));
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
    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        } else {
            musicFiles=getAllAudio(this);
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                musicFiles=getAllAudio(this);
                noOfSongs.setText(musicFiles.size());
            } else {
                Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public ArrayList<MusicFiles> getAllAudio(Context context){
        SharedPreferences preferences=getSharedPreferences(MY_SORT_PREF,MODE_PRIVATE);
        String sortOrder=preferences.getString("sorting","sortByName");
        String order =null;
        ArrayList<MusicFiles> tempAudioList=new ArrayList<>();
        Uri uri= MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //code to set the order in which you want to arrange
        switch (sortOrder){
            case "sortByName":
                order=MediaStore.MediaColumns.DISPLAY_NAME+" ASC";
                break;
            case "sortByDate":
                order=MediaStore.MediaColumns.DATE_ADDED+" DESC";
                break;
            case "sortBySize":
                order=MediaStore.MediaColumns.SIZE+" DESC";
                break;
        }
        String[] projection={
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID
        };
        Cursor cursor=context.getContentResolver().query(uri,projection,null,null,order);
        if(cursor!=null){
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                String duration = cursor.getString(1);
                String path = cursor.getString(2);
                String artist = cursor.getString(3);
                String id= cursor.getString(4);
                MusicFiles musicFiles = new MusicFiles(path, title, artist, duration,id);
                if(!musicFiles.getTitle().startsWith("PTT-") && !musicFiles.getTitle().startsWith("AUD-")) // to skip whatsapp and other voice recordings
                    tempAudioList.add(musicFiles);
            }
                cursor.close();
            }
        noOfSongs.setText(String.valueOf(tempAudioList.size()));
        return tempAudioList;
    }

    @Override
    // to show search option  in ActionBar
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search,menu);
        MenuItem menuItem=menu.findItem(R.id.search_bar);
        SearchView searchView= (SearchView) menuItem.getActionView();
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String userInput=newText.toLowerCase();
        ArrayList<MusicFiles> myFiles=new ArrayList<>();
        for(MusicFiles song:musicFiles){
            if(song.getTitle().toLowerCase().contains(userInput)){
                myFiles.add(song);
            }
        }
        musicAdapter.updateSongList(myFiles);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor=getSharedPreferences(MY_SORT_PREF,MODE_PRIVATE).edit();
         if(item.getItemId()==R.id.sort_by_name){
             editor.putString("sorting","sortByName");
             editor.apply();
             this.recreate();
         } else if (item.getItemId()==R.id.sort_by_date) {
             editor.putString("sorting","sortByDate");
             editor.apply();
             this.recreate();
         } else if (item.getItemId()==R.id.sort_by_size) {
             editor.putString("sorting","sortBySize");
             editor.apply();
             this.recreate();
         }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (actionMode != null) {
            actionMode.finish();
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (actionMode != null) {
            actionMode.finish();
        } else {
            super.onBackPressed();
        }
    }
}
