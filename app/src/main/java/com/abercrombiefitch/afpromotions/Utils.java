package com.abercrombiefitch.afpromotions;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Utils {

    public static Bitmap[] downloadBitmap(String... urls) {
        return downloadBitmap(null, null, null, urls);
    }

    public static Bitmap[] downloadBitmap(LruCache<String, Bitmap> memoryCache, DiskLruCache diskCache, Object diskCacheLock, String... urls) {
        Bitmap[] retval = new Bitmap[urls.length];
        for (int i = 0; i < urls.length; ++i) {
            retval[i] = getBitmapCache(urls[i], memoryCache, diskCache, diskCacheLock);
            if (retval[i] != null) continue;
            URL url;
            try {
                url = new URL(urls[i]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                continue;
            }

            HttpURLConnection urlConnection;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            try {
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream())) {
                        retval[i] = BitmapFactory.decodeStream(inputStream);
                        addBitmapToCache(urls[i], retval[i], memoryCache, diskCache, diskCacheLock);
                    }
                }
            } catch (IOException e) {
                Log.e("A&F", e.toString());
                e.printStackTrace();
            }
        }
        return retval;
    }

    public static void addBitmapToCache(String key, Bitmap bitmap, LruCache<String, Bitmap> memoryCache,
                                        DiskLruCache diskLruCache, Object diskCacheLock) {
        synchronized (diskCacheLock) {
            // Add to memory cache as before
            if (getBitmapCache(key, memoryCache, diskLruCache, diskCacheLock) == null) {
                memoryCache.put(key, bitmap);
            }

            // Also add to disk cache
            addToDiskCache(key, bitmap, diskLruCache, diskCacheLock);
        }
    }

    private static void addToDiskCache(final String key, final Bitmap bitmap, final DiskLruCache diskLruCache,
                                       final Object diskCacheLock) {
        synchronized (diskCacheLock) {
            try {
                String cacheKey = key
                        .toLowerCase()
                        .replace("/", "")
                        .replace(":", "")
                        .replace(".", "")
                        .replace("-", "");
                if (diskLruCache != null && diskLruCache.get(cacheKey) == null) {
                    DiskLruCache.Editor editor = diskLruCache.edit(cacheKey);
                    BufferedOutputStream stream = new BufferedOutputStream(editor.newOutputStream(0));
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    editor.commit();
                    stream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Bitmap getBitmapCache(final String key, final LruCache<String, Bitmap> memoryCache,
                                        final DiskLruCache diskLruCache, final Object diskCacheLock) {
//            // Wait while disk cache is started from background thread
//            while (mDiskCacheStarting) {
//                try {
//                    mDiskCacheLock.wait();
//                } catch (InterruptedException e) {}
//            }
        Bitmap retval = null;
        if (memoryCache != null) {
            retval = memoryCache.get(key);
        }

        synchronized (diskCacheLock) {
            if (retval == null && diskLruCache != null) {
                try {
                    DiskLruCache.Snapshot snapshot = diskLruCache.get(key
                            .toLowerCase()
                            .replace("/", "")
                            .replace(":", "")
                            .replace(".", "")
                            .replace("-", ""));
                    if (snapshot != null) {
                        try (BufferedInputStream stream = new BufferedInputStream(snapshot.getInputStream(0))) {
                            return BitmapFactory.decodeStream(stream);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    // Creates a unique subdirectory of the designated app cache directory.
    public static File getDiskCacheDir(final Context context, final String uniqueName) {
        final String cachePath = context.getCacheDir().getPath();
        return new File(cachePath + File.separator + uniqueName);
    }
}