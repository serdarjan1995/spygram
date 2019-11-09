package com.ins.spygram;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;

public class CustomListview extends ArrayAdapter<String> {

    private ArrayList<UserFollower> followers;
    private Activity context;
    private ImageLoader imageLoader;
    private DisplayImageOptions options;
    private ImageLoaderConfiguration config;


    @Override
    public int getCount() {
        return followers.size();
    }

    public CustomListview(Activity context, ArrayList<UserFollower> followers) {
        super(context, R.layout.followers_list);
        this.context = context;
        this.followers = followers;
        options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();

        config = new ImageLoaderConfiguration.Builder(context)
                .defaultDisplayImageOptions(options)
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .diskCacheSize(20 * 1024 * 1024)
                .memoryCacheSize(20 * 1024 * 1024)
                .build();

        imageLoader = ImageLoader.getInstance();
        imageLoader.init(config);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View r = convertView;
        ViewHolder viewHolder = null;

        if (r == null)
        {
            LayoutInflater layoutInflater = context.getLayoutInflater();
            r=layoutInflater.inflate(R.layout.followers_list,null,true);
            viewHolder = new ViewHolder(r);
            r.setTag(viewHolder);

        }
        else
        {
            viewHolder = (ViewHolder) r.getTag();
        }

        imageLoader.getInstance();
        final ViewHolder finalViewHolder = viewHolder;
        imageLoader.displayImage(followers.get(position).getPp_url(), viewHolder.avatar, options, new SimpleImageLoadingListener() {
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
                finalViewHolder.avatar.setImageBitmap(loadedImage);
            }
        }, new ImageLoadingProgressListener() {
            @Override
            public void onProgressUpdate(String imageUri, View view, int current, int total) {
            }
        });
        viewHolder.name_text.setText(followers.get(position).getFull_name());
        viewHolder.username_text.setText(followers.get(position).getUsername());
        viewHolder.hasStory.setVisibility(View.INVISIBLE);
        return r;
    }

    class ViewHolder
    {
        TextView name_text;
        TextView username_text;
        ImageView avatar;
        ImageView hasStory;
        ViewHolder(View v)
        {
            name_text = (TextView) v.findViewById(R.id.text_view_name);
            username_text = (TextView) v.findViewById(R.id.text_view_username);
            avatar = (ImageView) v.findViewById(R.id.avatar);
            hasStory = (ImageView) v.findViewById(R.id.hasstoryindicator);
        }
    }
}
