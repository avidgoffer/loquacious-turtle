package com.abercrombiefitch.afpromotions;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.LruCache;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap[]> {
    private final DiskLruCache _diskCache;
    private final Object _diskCacheLock;
    private final ImageView[] _imageView;
    private final LruCache<String, Bitmap> _memoryCache;

    public DownloadImageTask(LruCache<String, Bitmap> memoryCache, DiskLruCache diskCache, Object diskCacheLock, ImageView... imageView) {
        _diskCache = diskCache;
        _diskCacheLock = diskCacheLock;
        _imageView = imageView;
        _memoryCache = memoryCache;
    }

    @Override
    protected void onPostExecute(Bitmap[] bitmaps) {
        for(int i = 0; i < bitmaps.length; ++i){
            if(bitmaps[i] != null) {
                _imageView[i].setImageBitmap(bitmaps[i]);
            }
        }
    }

    @Override
    protected Bitmap[] doInBackground(String... params) {
        return Utils.downloadBitmap(_memoryCache, _diskCache, _diskCacheLock, params);
    }
}