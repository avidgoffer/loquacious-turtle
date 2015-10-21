package com.abercrombiefitch.afpromotions;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class GetPromotionsTask extends AsyncTask<Void, Void, ArrayList<Promotion>> {

    private final TableLayout _tableLayout;
    private final LruCache<String, Bitmap> _memoryCache;
    private final DiskLruCache _diskCache;
    private final Object _diskCacheLock;
    private final ConnectivityManager _connectivityManager;

    public GetPromotionsTask(TableLayout tableLayout) {
        this(tableLayout, null);
    }

    public GetPromotionsTask(TableLayout tableLayout, LruCache<String, Bitmap> memoryCache) {
        this(tableLayout, memoryCache, null, null);
    }

    public GetPromotionsTask(TableLayout tableLayout, LruCache<String, Bitmap> memoryCache, DiskLruCache diskCahce, Object diskCacheLock) {
        _tableLayout = tableLayout;
        _memoryCache = memoryCache;
        _diskCache = diskCahce;
        _diskCacheLock = diskCacheLock;
        _connectivityManager = (ConnectivityManager)_tableLayout.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    protected void onPreExecute() {
        _tableLayout.removeAllViews();

        ProgressBar progressBar = new ProgressBar(_tableLayout.getContext());
        progressBar.setIndeterminate(true);

        _tableLayout.addView(progressBar);
    }

    protected void onPostExecute(ArrayList<Promotion> promotions) {
        final Context context = _tableLayout.getContext();
        _tableLayout.removeAllViews();
        if(promotions.isEmpty()){

            NetworkInfo activeNetwork = _connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            TextView textView = new TextView(context);
            textView.setGravity(Gravity.CENTER);
            if(!isConnected) {
                textView.setText("Please connect to the internet to receive promotions from Abercrombie & Fitch.");
            } else {
                textView.setText("No promotions received from Abercrombie & Fitch.");
            }
            _tableLayout.addView(textView);
        } else {
            for (Promotion promotion : promotions) {
                ImageView imageView = new ImageView(_tableLayout.getContext());
                imageView.setImageBitmap(promotion.image);
                imageView.setTag(promotion);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Promotion promotion = (Promotion) v.getTag();
                        _tableLayout.getContext().startActivity(
                                new Intent(context.getApplicationContext(), PromotionCardActivity.class)
                                        .putExtra("promotion", promotion));
                    }
                });

                TextView textView = new TextView(context);
                textView.setText(promotion.title);
                textView.setGravity(Gravity.CENTER);

                LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setLayoutMode(LinearLayout.VERTICAL);
                linearLayout.addView(imageView);

                _tableLayout.addView(textView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                _tableLayout.addView(linearLayout);

//            FrameLayout frameLayout = new FrameLayout(context);
//            frameLayout.addView(imageView);
//            tableRow.addView(frameLayout);

//            for(PromotionButton promotionButton : promotion.buttons){
//                Button button = new Button(context);
//                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
//                        FrameLayout.LayoutParams.WRAP_CONTENT,
//                        FrameLayout.LayoutParams.WRAP_CONTENT,
//                        Gravity.BOTTOM | Gravity.END);
//                layoutParams.rightMargin = 15;
//                frameLayout.addView(button, layoutParams);
//                button.setText(promotionButton.title);
//                button.setTag(promotionButton);
//                button.setOnClickListener(new View.OnClickListener() {
//                    public void onClick(View v) {
//                        context.startActivity(new Intent(Intent.ACTION_VIEW,
//                                Uri.parse(((PromotionButton)v.getTag()).target)));
//                    }
//                });
//            }
            }
        }
    }

    protected ArrayList<Promotion> doInBackground(Void... params) {
        ArrayList<Promotion> retval = new ArrayList<>();

        DiskLruCache.Snapshot snapshot;
        snapshot = getSnapshot("json");
        if(snapshot == null) {
            URL url;
            try {
                url = new URL("http://www.abercrombie.com/anf/nativeapp/Feeds/promotions.json");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return retval;
            }

            NetworkInfo activeNetwork = _connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            if(!isConnected) return retval;

            HttpURLConnection urlConnection;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
                return retval;
            }

            InputStream inputStream = null;
            try {
                inputStream = new BufferedInputStream(urlConnection.getInputStream());
                synchronized (_diskCacheLock) {
                    DiskLruCache.Editor editor = _diskCache.edit("json");
                    try (BufferedOutputStream stream = new BufferedOutputStream(editor.newOutputStream(0))) {
                        byte[] data = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(data, 0, 4096)) != -1) {
                            stream.write(data, 0, bytesRead);
                        }
                    }
                    editor.commit();
                }
            } catch (IllegalStateException | IOException e1) {
                Log.e("A&F", e1.toString());
                e1.printStackTrace();
            } finally {
                urlConnection.disconnect();
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e("A&F", e.toString());
                        e.printStackTrace();
                    }
                }
            }
            snapshot = getSnapshot("json");
        }
        try {
            assert snapshot != null;
            retval = readJsonStream(snapshot.getInputStream(0));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return retval;
    }

    private DiskLruCache.Snapshot getSnapshot(final String key) {
        try {
            return _diskCache.get(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ArrayList<Promotion> readJsonStream(InputStream in) throws IOException {
        return readPromotionsArray(new JsonReader(new InputStreamReader(in, "UTF-8")));
    }

    private ArrayList<Promotion> readPromotionsArray(JsonReader reader) throws IOException {
        ArrayList<Promotion> promotions = new ArrayList<>();

        reader.beginObject();
        if(reader.hasNext()){
            String name = reader.nextName();
            if(name.equals("promotions")){
                reader.beginArray();
                while (reader.hasNext()) {
                    promotions.add(readPromotion(reader));
                }
                reader.endArray();
            }
        }
        reader.endObject();
        return promotions;
    }

    private Promotion readPromotion(JsonReader reader) throws IOException {
        Promotion promotion = new Promotion();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "button":
                    JsonToken token = reader.peek();
                    if(token == JsonToken.BEGIN_ARRAY) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            promotion.buttons.add(readButton(reader));
                        }
                        reader.endArray();
                    } else if(token == JsonToken.BEGIN_OBJECT) {
                        promotion.buttons.add(readButton(reader));
                    }
                    break;
                case "description":
                    promotion.description = reader.nextString();
                    break;
                case "footer":
                    promotion.footer = reader.nextString();
                    break;
                case "image":
                    promotion.imageUrl = reader.nextString();
                    Bitmap[] bitmaps = Utils.downloadBitmap(_memoryCache, _diskCache, _diskCacheLock, promotion.imageUrl);
                    if(bitmaps.length != 0 && bitmaps[0] != null) {
                        promotion.image = bitmaps[0];
                    }
                    break;
                case "title":
                    promotion.title = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return promotion;
    }

    private PromotionButton readButton(JsonReader reader) throws IOException {
        PromotionButton promotionButton = new PromotionButton();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("target")) {
                promotionButton.target = reader.nextString();
            } else if (name.equals("title")) {
                promotionButton.title = reader.nextString();
            }
        }
        reader.endObject();
        return promotionButton;
    }
}