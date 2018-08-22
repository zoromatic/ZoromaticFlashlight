package com.zoromatic.flashlight;

public class RowItem {
    private int imageId;
    private String desc;

    RowItem(int imageId, String desc) {
        this.imageId = imageId;
        this.desc = desc;
    }

    public int getImageId() {
        return imageId;
    }

    public String getDesc() {
        return desc;
    }
}
