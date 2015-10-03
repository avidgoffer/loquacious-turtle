package com.abercrombiefitch.afpromotionalcard;

import java.util.ArrayList;

public class Promotion {

    public Promotion() {
        button = new ArrayList<>();
    }

    public ArrayList<PromotionButton> button;
    public String description;
    public String footer;
    public String image;
    public String title;
}