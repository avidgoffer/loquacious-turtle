package com.abercrombiefitch.afpromotions;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.LruCache;
import android.view.MenuItem;
import android.widget.TableLayout;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private DiskLruCache _diskLruCache;
    private final Object _diskCacheLock = new Object();
    private boolean _diskCacheStarting = true;
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    private static final String DISK_CACHE_SUBDIR = "thumbnails";

    private LruCache<String, Bitmap> _memoryCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_SHORT)
//                        .setAction("Action", null).show();
//            }
//        });

        // Initialize memory cache
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
                    File cacheDir = Utils.getDiskCacheDir(MainActivity.this, DISK_CACHE_SUBDIR);
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
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new GetPromotionsTask((TableLayout) findViewById(R.id.tableLayout), _memoryCache, _diskLruCache, _diskCacheLock).execute();
    }

    @Override
    protected void onDestroy() {
        try {
            _diskLruCache.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up buttons, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public int doMath(){
        return 3+3;
    }

}