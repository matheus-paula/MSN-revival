package com.app.messenger.messenger;

public class Wink {
    private String name;
    private int thumb;
    private int media;

    public Wink() {
    }

    Wink(String name, int thumb, int media) {
        this.name = name;
        this.thumb = thumb;
        this.media = media;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getThumb() {
        return thumb;
    }

    public void setThumb(int thumb) {
        this.thumb = thumb;
    }

    public int getMedia() {
        return media;
    }

}
