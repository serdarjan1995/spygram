package com.ins.spygram;

public class MediaDownloadEntity {
    private String url;
    private String dimensions;
    private String id;
    private int mediaType;

    public MediaDownloadEntity(String url, String height, String width, int mediaType, String id){
        this.url = url;
        this.mediaType = mediaType;
        this.id = id;
        dimensions = height + "x" + width;
    }

    public String getUrl() {
        return url;
    }

    public String getDimensions() {
        return dimensions;
    }

    public String getId() {
        return id;
    }

    public int getMediaType(){
        return mediaType;
    }

}
