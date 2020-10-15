package com.app.messenger.messenger;

public class SpinnerItemData {

    private String text;
    private Integer imageId;
    SpinnerItemData(String text, Integer imageId){
        this.text = text;
        this.imageId = imageId;
    }

    public String getText(){
        return text;
    }

    public Integer getImageId(){
        return imageId;
    }
}