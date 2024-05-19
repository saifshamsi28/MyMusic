package com.saif.mymusic;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.saif.mymusic.MusicFiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {
    private Context mContext;
    private ContentResolver mContentResolver;
    static ArrayList<MusicFiles> mFiles;

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
        holder.file_name.setText(mFiles.get(position).getTitle());

        // Load album art in a separate thread
        new LoadAlbumArtTask(holder, position).execute();

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, PlayerActivity.class);
                intent.putExtra("Title", mFiles.get(position).getTitle());
                intent.putExtra("Position", position);
                mContext.startActivity(intent);
            }
        });
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                PopupMenu popupMenu = new PopupMenu(mContext, v);
                popupMenu.getMenuInflater().inflate(R.menu.delete_items, popupMenu.getMenu());
                popupMenu.show();
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (R.id.delete == item.getItemId()) {
                            deleteSong(position, v);
                        }
                        return true;
                    }
                });
            }
        });
    }

    private void deleteSong(int position, View v) {
        String externalStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
            Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    Long.parseLong(mFiles.get(position).getId()));

            // Use SAF for API Level 30+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Collection<Uri> urisToDelete = Collections.singletonList(contentUri);
                PendingIntent deleteRequest = MediaStore.createDeleteRequest(mContentResolver, urisToDelete);

                try {
                    // Start IntentSenderForResult to prompt the user
                    ((MainActivity) mContext).startIntentSenderForResult(deleteRequest.getIntentSender(), 1000, null, 0, 0, 0, null);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            } else {
                // For API Level < 30, fallback to the existing deletion method
                deleteSongFile(position, v, contentUri);
            }
            deleteSongFile(position, v, contentUri);
        } else {
            Log.e(this.toString(), "External storage is not writable.");
            // Handle the case where external storage is not available
        }
    }
    private void deleteSongFile(int position, View v, Uri contentUri) {
            mFiles.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mFiles.size());
            Snackbar.make(v, "Song deleted : ", Snackbar.LENGTH_LONG).show();
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView file_name;
        ImageView album_art, delete;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            file_name = itemView.findViewById(R.id.music_file_name);
            album_art = itemView.findViewById(R.id.music_img);
            delete = itemView.findViewById(R.id.delete_option);
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
                return getAlbumArt(mFiles.get(position).getPath());
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
        retriever.setDataSource(uri);
        return retriever.getEmbeddedPicture();
    }

    // This method is called when searching is performed
    void updateSongList(ArrayList<MusicFiles> filteredMusicFiles) {
        mFiles = new ArrayList<>();
        mFiles.addAll(filteredMusicFiles);
        notifyDataSetChanged();
    }
}
