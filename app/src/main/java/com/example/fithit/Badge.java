package com.example.fithit;
public class Badge {
    private String name;
    private int imageResId;

    public Badge(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }
}
