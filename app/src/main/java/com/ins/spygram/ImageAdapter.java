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

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;


public class ImageAdapter extends PagerAdapter {

    private Context mContext;
    private ArrayList<String> url;
    private ArrayList<Integer> mediaType;
    private ImageLoader imageLoader;
    private DisplayImageOptions options;

    ImageAdapter(Context context, ArrayList<String> url,ArrayList<Integer> mediaType){
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
            options = new DisplayImageOptions.Builder()
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .build();
            imageLoader = ImageLoader.getInstance();
            imageLoader.init(ImageLoaderConfiguration.createDefault(this.mContext));
            imageLoader.getInstance().displayImage(url.get(position), imageView, options, new SimpleImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {
                    //System.out.println("download started");
                }
                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    //System.out.println("progress failed");
                }
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    imageView.setImageBitmap(loadedImage);
                    //System.out.println("download complete");
                }
            }, new ImageLoadingProgressListener() {
                @Override
                public void onProgressUpdate(String imageUri, View view, int current, int total) {
                }
            });
            container.addView(imageView,0);
            return imageView;
        }
        else{
            final VideoView videoView = new VideoView(mContext);
            MediaController mediaController = new MediaController(mContext);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);
            videoView.setVideoURI(Uri.parse(url.get(position)));
            //videoView.start();
            container.addView(videoView,0);
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
