package com.saif.mymusic;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.chibde.visualizer.BarVisualizer;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {
    private final Context mContext;
    private ContentResolver mContentResolver;
    static ArrayList<MusicFiles> mFiles;
    private boolean multiSelectModeOn = false;
    private ArrayList<MusicFiles> selectedItems = new ArrayList<>();
    private List<LoadAlbumArtTask> taskList = new ArrayList<>();
//    static AnimationDrawable animation;
    public int playingPosition=-1;

    MusicAdapter(Context mContext, ContentResolver contentResolver, ArrayList<MusicFiles> mFiles) {
        this.mContext = mContext;
        this.mContentResolver = contentResolver;
        this.mFiles = mFiles;
    }

    @NonNull
    @Override
    public MusicAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicAdapter.MyViewHolder holder, int position) {
        MusicFiles musicFile = mFiles.get(position);
        holder.file_name.setText(mFiles.get(position).getTitle());
        holder.file_size.setText(formatMusicFileSize(mFiles.get(position).getSize()));
        //these 3 lines ,to set the moving text for BOOK Title
//        holder.file_name.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        holder.file_name.setSingleLine(true);
//        holder.file_name.setSelected(true);

        // this will Load album art in a separate thread
        LoadAlbumArtTask task = new LoadAlbumArtTask(holder, position);
        taskList.add(task);
        task.execute();

        musicFile.setPlaying(false);

        if (musicFile.isSelected()) {
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.Teal));
        }
//        animation = (AnimationDrawable) holder.music_beats.getDrawable();
        if (musicFile.isPlaying()) {
            holder.file_size.setVisibility(View.GONE);
            holder.visualizer.setVisibility(View.VISIBLE);
            holder.visualizer.setPlayer(PlayerActivity.getInstance().audioSessionId);
            holder.visualizer.setDensity(70);
            Log.e("MusicAdapter","visualizer set for: "+musicFile.getTitle());
//            animation.start();
        } else {
            holder.file_size.setVisibility(View.VISIBLE);
            holder.file_size.setText(formatMusicFileSize(mFiles.get(position).getSize()));
            holder.visualizer.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (multiSelectModeOn) {
                toggleSelection(position);
            } else {
                if (position != -1) {
                    mFiles.get(position).setPlaying(false);
                }
                MainActivity.currentPlayingPosition = position;
                playingPosition = position;
                notifyItemChanged(position);

                Intent activityIntent = new Intent(mContext, PlayerActivity.class);
                activityIntent.putExtra("Position", position);
                activityIntent.putExtra("fromNotification", false);
                mContext.startActivity(activityIntent);
            }
        });


        holder.itemView.setOnLongClickListener(v -> {
            if (!multiSelectModeOn) {
                multiSelectModeOn = true;
                ((MainActivity) mContext).startActionMode(((MainActivity) mContext).getActionModeCallback());
            }
            toggleSelection(position);
            return true;
        });
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


    private void toggleSelection(int position) {
        MusicFiles item = mFiles.get(position);
        item.setSelected(!item.isSelected());
        if (item.isSelected()) {
            selectedItems.add(item);
        } else {
            selectedItems.remove(item);
        }
        notifyItemChanged(position);
//        Toast.makeText(mContext, "selected "+selectedItems.size(), Toast.LENGTH_SHORT).show();
        ((MainActivity) mContext).updateActionModeTitle();
//        ((MainActivity) mContext).actionMode.setTitle(selectedItems.size() + " selected");
    }

    public void exitMultiSelectMode() {
        multiSelectModeOn = false;
        for (MusicFiles musicFile : mFiles) {
            musicFile.setSelected(false);
        }
        notifyDataSetChanged();
    }
    int getSelectedItemCount() {
        return selectedItems.size();
    }

    public void selectAll() {
        if (selectedItems.size() != mFiles.size()) {
            selectedItems.clear();
            for (MusicFiles musicFile : mFiles) {
                musicFile.setSelected(true);
                selectedItems.add(musicFile);
            }
        } else {
            for (MusicFiles musicFile : mFiles) {
                musicFile.setSelected(false);
            }
            selectedItems.clear();
        }
        notifyDataSetChanged();
        ((MainActivity) mContext).updateActionModeTitle();
    }

    public void deleteSongs(List<MusicFiles> selectedSongs, View v) {
        List<Integer> positions = new ArrayList<>();
        List<Uri> urisToDelete = new ArrayList<>();

        for (MusicFiles musicFile : selectedSongs) {
            int position = mFiles.indexOf(musicFile);
            if (position != -1) {
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(musicFile.getId()));
                urisToDelete.add(contentUri);
                positions.add(position);
            }
        }

        if (!urisToDelete.isEmpty()) {
            for (LoadAlbumArtTask task : taskList) {
                task.cancel(true);
            }
            taskList.clear();

            String externalStorageState = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    PendingIntent deleteRequest = MediaStore.createDeleteRequest(mContentResolver, urisToDelete);
                    try {
                        ((MainActivity) mContext).startIntentSenderForResult(deleteRequest.getIntentSender(), 1000, null, 0, 0, 0, null);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                    for (Uri uri : urisToDelete) {
                        int position = positions.get(urisToDelete.indexOf(uri));
                        deleteSongFile(position, v);
                    }
//                    Toast.makeText(mContext, selectedSongs.size()+" songs deleted successfully", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(this.toString(), "External storage is not writable.");
            }
        }
    }

    private void deleteSongFile(int position, View v) {
        try {
            mFiles.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mFiles.size());
//            Toast.makeText(mContext, " songs deleted successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(v, "Failed to delete song", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView file_name, file_size;
        ImageView album_art,music_beats;
        BarVisualizer visualizer;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            file_name = itemView.findViewById(R.id.music_file_name);
            album_art = itemView.findViewById(R.id.music_img);
            file_size = itemView.findViewById(R.id.music_file_size);
            visualizer=itemView.findViewById(R.id.visualizer);
//            music_beats=itemView.findViewById(R.id.music_beats);
        }
    }

    // AsyncTask to load album art in the background
    private static class LoadAlbumArtTask extends AsyncTask<Void, Void, byte[]> {
        private MyViewHolder holder;
        private int position;

        LoadAlbumArtTask(MyViewHolder holder, int position) {
            this.holder = holder;
            this.position = position;
        }

        @Override
        protected byte[] doInBackground(Void... voids) {
            try {
                File file = new File(mFiles.get(position).getPath());
                if (file.exists()) {
                    return getAlbumArt(mFiles.get(position).getPath());
                } else {
                    return null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            if (bytes != null) {
                Glide.with(holder.itemView.getContext())
                        .asBitmap()
                        .load(bytes)
                        .into(holder.album_art);
            } else {
                Glide.with(holder.itemView.getContext())
                        .load(R.drawable.logo)
                        .into(holder.album_art);
            }
        }
    }

    // To set album art of the songs
    public static byte[] getAlbumArt(String uri) throws IOException {
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
}

