package com.abercrombiefitch.afpromotions;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.TextView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;

public class PromotionCardActivity extends AppCompatActivity {

    private DiskLruCache _diskLruCache;
    private final Object _diskCacheLock = new Object();
    private boolean _diskCacheStarting = true;
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    private static final String DISK_CACHE_SUBDIR = "thumbnails";
    private LruCache<String, Bitmap> _memoryCache;
    private Promotion _promotion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion_card);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        _promotion = (Promotion) getIntent().getExtras().getSerializable("promotion");

        ((TextView)findViewById(R.id.title)).setText(_promotion.title);
        ((TextView)findViewById(R.id.description)).setText(_promotion.description);
        TextView footer =  (TextView)findViewById(R.id.footer);
        footer.setMovementMethod(LinkMovementMethod.getInstance());
        footer.setText(Html.fromHtml(_promotion.footer));

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        _memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        // Initialize disk cache on background thread
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (_diskCacheLock) {
                    File cacheDir = Utils.getDiskCacheDir(PromotionCardActivity.this, DISK_CACHE_SUBDIR);
                    try {
                        _diskLruCache = DiskLruCache.open(cacheDir, 1, 1, DISK_CACHE_SIZE);
                        _diskCacheStarting = false; // Finished initialization
                        _diskCacheLock.notifyAll(); // Wake any waiting threads
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new DownloadImageTask(_memoryCache, _diskLruCache, _diskCacheLock, (ImageView) findViewById(R.id.promotionImage)).execute(_promotion.imageUrl);
    }

}