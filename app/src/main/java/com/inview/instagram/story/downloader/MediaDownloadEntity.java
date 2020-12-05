package com.inview.instagram.story.downloader;

import android.os.Parcel;
import android.os.Parcelable;

public class MediaDownloadEntity implements Parcelable {
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


    protected MediaDownloadEntity(Parcel in) {
        url = in.readString();
        dimensions = in.readString();
        id = in.readString();
        mediaType = in.readInt();
    }


    public static final Creator<MediaDownloadEntity> CREATOR = new Creator<MediaDownloadEntity>() {
        @Override
        public MediaDownloadEntity createFromParcel(Parcel in) {
            return new MediaDownloadEntity(in);
        }

        @Override
        public MediaDownloadEntity[] newArray(int size) {
            return new MediaDownloadEntity[size];
        }
    };


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


    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(dimensions);
        dest.writeString(id);
        dest.writeInt(mediaType);
    }

}
