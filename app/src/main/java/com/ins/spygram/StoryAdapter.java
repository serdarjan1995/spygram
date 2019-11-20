package com.ins.spygram;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;


import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class StoryAdapter extends PagerAdapter {

    private Context mContext;
    private ArrayList<String> url;
    private ArrayList<Integer> mediaType;

    StoryAdapter(Context context, ArrayList<String> url,ArrayList<Integer> mediaType) {
        mContext = context;
        this.url = url;
        this.mediaType = mediaType;
    }

    @Override
    public int getCount() {
        return url.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }


    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        if (mediaType.get(position) == 1) {
            final ImageView imageView = new ImageView(mContext);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Picasso.get().load(url.get(position)).into(imageView);
            container.addView(imageView,position);
            return imageView;
        }
        else{
            VideoView videoView = new VideoView(mContext);
            MediaController mediaController = new MediaController(mContext);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);
            videoView.setVideoURI(Uri.parse(url.get(position)));
            //videoView.start();
            container.addView(videoView,position);
            return videoView;
        }

    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (mediaType.get(position) == 1){
            container.removeView((ImageView) object);
        }
        else{
            container.removeView((VideoView) object);
        }

    }
}
