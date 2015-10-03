package com.abercrombiefitch.afpromotionalcard;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.widget.TableLayout;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class GetPromotionsTask extends AsyncTask<Void, Void, ArrayList<Promotion>> {

    private final TableLayout _tableLayout;

    public GetPromotionsTask(TableLayout tableLayout) {
        _tableLayout = tableLayout;
    }

    protected void onPreExecute() {
    }

    protected void onPostExecute(ArrayList<Promotion> promotions) {
        super.onPostExecute(promotions);
    }

    protected ArrayList<Promotion> doInBackground(Void... params) {
        URL url;
        try {
            url = new URL("http://www.abercrombie.com/anf/nativeapp/Feeds/promotions.json");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(urlConnection.getInputStream());
            return readJsonStream(inputStream);
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

        return null;
    }

    private ArrayList<Promotion> readJsonStream(InputStream in) throws IOException {
        try (JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"))) {
            return readPromotionsArray(reader);
        }
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
                            promotion.button.add(readButton(reader));
                        }
                        reader.endArray();
                    } else if(token == JsonToken.BEGIN_OBJECT) {
                        promotion.button.add(readButton(reader));
                    }
                    break;
                case "description":
                    promotion.description = reader.nextString();
                    break;
                case "footer":
                    promotion.footer = reader.nextString();
                    break;
                case "image":
                    promotion.image = reader.nextString();
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