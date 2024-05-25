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
import android.text.TextUtils;
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
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {
    private Context mContext;
    private ContentResolver mContentResolver;
    static ArrayList<MusicFiles> mFiles;
    private boolean multiSelectModeOn = false;
    private List<LoadAlbumArtTask> taskList = new ArrayList<>();

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
        //these 3 lines ,to set the moving text for BOOK Title
        holder.file_name.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        holder.file_name.setSingleLine(true);
        holder.file_name.setSelected(true);

        // this will Load album art in a separate thread
        LoadAlbumArtTask task = new LoadAlbumArtTask(holder, position);
        taskList.add(task);
        task.execute();

        if (musicFile.isSelected()) {
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.Teal));
        }

        holder.itemView.setOnClickListener(v -> {
            if (multiSelectModeOn) {
                toggleSelection(position);
            } else {
                Intent intent = new Intent(mContext, PlayerActivity.class);
                intent.putExtra("Title", mFiles.get(position).getTitle());
                intent.putExtra("Position", position);
                mContext.startActivity(intent);
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

    private void toggleSelection(int position) {
        MusicFiles item = mFiles.get(position);
        item.setSelected(!item.isSelected());
        notifyItemChanged(position);
    }

    public void exitMultiSelectMode() {
        multiSelectModeOn = false;
        for (MusicFiles musicFile : mFiles) {
            musicFile.setSelected(false);
        }
        notifyDataSetChanged();
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
            // Cancel ongoing tasks(which is performing on thread)  to avoid app crash
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
                        deleteSongFile(position, v, uri);
                    }
//                    Toast.makeText(mContext, selectedSongs.size()+" songs deleted successfully", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(this.toString(), "External storage is not writable.");
            }
        }
    }

    private void deleteSongFile(int position, View v, Uri contentUri) {
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
        TextView file_name;
        ImageView album_art;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            file_name = itemView.findViewById(R.id.music_file_name);
            album_art = itemView.findViewById(R.id.music_img);
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
    private static byte[] getAlbumArt(String uri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(uri);
            return retriever.getEmbeddedPicture();
        } catch (IllegalArgumentException e) {
            Log            .e("MusicAdapter", "Failed to set data source for MediaMetadataRetriever: " + e.getMessage());
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

    // This method is called when searching is performed
    void updateSongList(ArrayList<MusicFiles> filteredMusicFiles) {
        mFiles.clear();
        mFiles.addAll(filteredMusicFiles);
        notifyDataSetChanged();
    }
}

