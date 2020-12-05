package com.inview.instagram.story.downloader;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class StoryEntity implements Parcelable {
    private long publishTime;
    private String id;
    private String username;
    private String defaultMediaUrl;
    private int mediaType;
    private ArrayList<MediaDownloadEntity> mediaDownloadEntities;


    public StoryEntity(JSONObject jsonObjectItem, String username){
        try {
            mediaDownloadEntities = new ArrayList<>();
            this.id = jsonObjectItem.getString("id");
            this.publishTime =jsonObjectItem.getLong("taken_at");
            this.mediaType = jsonObjectItem.getInt("media_type");
            this.username = username;
            if ( mediaType == 2 && jsonObjectItem.has("video_versions")){
                JSONObject videoVersion = jsonObjectItem.getJSONArray("video_versions")
                        .getJSONObject(0);
                mediaDownloadEntities.add(new MediaDownloadEntity(
                        videoVersion.getString("url"),
                        videoVersion.getString("height"),
                        videoVersion.getString("width"),
                        2,
                        id));
                this.defaultMediaUrl = videoVersion.getString("url");
                if (jsonObjectItem.has("image_versions2")){
                    JSONArray imgCandidates = jsonObjectItem.getJSONObject("image_versions2")
                            .getJSONArray("candidates");
                    mediaDownloadEntities.add(new MediaDownloadEntity(
                            imgCandidates.getJSONObject(0).getString("url"),
                            imgCandidates.getJSONObject(0).getString("height"),
                            imgCandidates.getJSONObject(0).getString("width"),
                            1,
                            id));
                }
            }
            else if (mediaType == 1 && jsonObjectItem.has("image_versions2")){
                JSONArray imgCandidates = jsonObjectItem.getJSONObject("image_versions2")
                        .getJSONArray("candidates");
                JSONObject candidate;
                for (int i=0; i<imgCandidates.length(); i++){
                    candidate = imgCandidates.getJSONObject(i);
                    mediaDownloadEntities.add(new MediaDownloadEntity(
                            candidate.getString("url"),
                            candidate.getString("height"),
                            candidate.getString("width"),
                            1,
                            id));
                }
                this.defaultMediaUrl = mediaDownloadEntities.get(0).getUrl();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    protected StoryEntity(Parcel in) {
        publishTime = in.readLong();
        id = in.readString();
        username = in.readString();
        defaultMediaUrl = in.readString();
        mediaType = in.readInt();
        mediaDownloadEntities = in.createTypedArrayList(MediaDownloadEntity.CREATOR);
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(publishTime);
        dest.writeString(id);
        dest.writeString(username);
        dest.writeString(defaultMediaUrl);
        dest.writeInt(mediaType);
        dest.writeTypedList(mediaDownloadEntities);
    }


    @Override
    public int describeContents() {
        return 0;
    }


    public static final Creator<StoryEntity> CREATOR = new Creator<StoryEntity>() {
        @Override
        public StoryEntity createFromParcel(Parcel in) {
            return new StoryEntity(in);
        }

        @Override
        public StoryEntity[] newArray(int size) {
            return new StoryEntity[size];
        }
    };


    public long getPublishTime() {
        return publishTime;
    }


    public String getId() {
        return id;
    }


    public String getUsername() {
        return username;
    }


    public String getDefaultMediaUrl() {
        return defaultMediaUrl;
    }


    public int getMediaType() {
        return mediaType;
    }


    public ArrayList<MediaDownloadEntity> getMediaDownloadEntities() {
        return mediaDownloadEntities;
    }

}
