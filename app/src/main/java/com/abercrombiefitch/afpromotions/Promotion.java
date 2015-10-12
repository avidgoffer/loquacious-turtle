package com.abercrombiefitch.afpromotions;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.util.ArrayList;

public class Promotion implements Serializable {

    public Promotion() {
        buttons = new ArrayList<>();
        description = "";
        footer = "";
        title = "";
    }

    public ArrayList<PromotionButton> buttons;
    public String description;
    public String footer;
    public String imageUrl;
    public transient Bitmap image;
    public String title;
}