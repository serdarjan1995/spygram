package com.inview.instagram.story.downloader;

import android.Manifest;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.ViewAnimator;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MediaFragment extends Fragment {
    private Handler handler;
    private ViewAnimator progressView;
    public static MediaFragment newInstance(StoryEntity storyEntity) {
        MediaFragment f = new MediaFragment();
        Bundle args = new Bundle();
        args.putParcelable("story",storyEntity);
        f.setArguments(args);
        return f;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.story_fragment, container, false);
        if (getArguments() == null){
            return v;
        }
        handler = new Handler(Looper.getMainLooper());
        progressView = v.findViewById(R.id.progress_view_story_fragment);
        progressShow(0);
        final StoryEntity storyEntity = getArguments().getParcelable("story");
        assert storyEntity != null;
        String mediaUrl = storyEntity.getDefaultMediaUrl();
        int mediaType = storyEntity.getMediaType();
        final FrameLayout story_fragment = v.findViewById(R.id.story_fragment);
        ImageView downloadIcon = new ImageView(v.getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.width = 60;
        params.height = 60;
        params.rightMargin = 50;
        params.topMargin = 120;
        params.gravity = Gravity.END;
        downloadIcon.setImageResource(R.mipmap.download_icon);
        downloadIcon.setImageAlpha(200);
        downloadIcon.setLayoutParams(params);
        downloadIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backgroundThreadDialog(storyEntity.getMediaDownloadEntities(),
                        storyEntity.getUsername(),
                        v.getContext());
            }
        });

        TextView publishDate = v.findViewById(R.id.story_date);
        Date storyDate = new Date(storyEntity.getPublishTime()*1000);
        TimeZone tz = TimeZone.getDefault();
        SimpleDateFormat ft = new SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault());
        ft.setTimeZone(tz);
        publishDate.setText(ft.format(storyDate));
        ImageView imageView = v.findViewById(R.id.story_image);
        final VideoView videoView = v.findViewById(R.id.story_video);
        if (mediaType == 1) {
            videoView.setVisibility(View.INVISIBLE);
            ImageView playButton = v.findViewById(R.id.story_video_play_pause);
            playButton.setVisibility(View.INVISIBLE);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Picasso.get().load(mediaUrl).into(imageView, new com.squareup.picasso.Callback() {
                @Override
                public void onSuccess() {
                    progressHide(0);
                }

                @Override
                public void onError(Exception e) {
                    backgroundThreadShortToast(getString(R.string.net_err),v.getContext());
                    progressHide(0);
                }
            });
        }
        else{
            imageView.setVisibility(View.INVISIBLE);
            final ImageView playButton = v.findViewById(R.id.story_video_play_pause);
            videoView.setVideoURI(Uri.parse(mediaUrl));
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    videoView.seekTo( 1 );
                    mp.setLooping(true);
                    videoView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View view, boolean b) {
                            if(!b){
                                if(videoView.isPlaying()){
                                    videoView.pause();
                                    playButton.setImageResource(R.mipmap.play_button);
                                }
                            }
                        }
                    });
                    playButton.setVisibility(View.VISIBLE);
                    progressHide(0);
                }
            });


            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(videoView.isPlaying()){
                        videoView.pause();
                        playButton.setImageResource(R.mipmap.play_button);
                    }
                    else{
                        videoView.start();
                        playButton.setImageResource(R.mipmap.pause_button);
                    }
                }
            });

        }
        story_fragment.addView(downloadIcon);
        return v;
    }


    public void backgroundThreadDialog(final ArrayList<MediaDownloadEntity> mediaDownloadEntities,
                                       final String username, final Context context) {
        if (context != null && mediaDownloadEntities != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressShow(0);
                    final Dialog dialog = new Dialog(context);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.radiobutton_dialog);
                    final RadioGroup rg = dialog.findViewById(R.id.radio_group);
                    Button downloadButton = dialog.findViewById(R.id.radiogroup_button);
                    downloadButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Util.checkPermission(getActivity());
                            if (ContextCompat.checkSelfPermission(context,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                downloadMedia(mediaDownloadEntities.get(rg.getCheckedRadioButtonId()),username,context);
                                dialog.dismiss();
                            }

                        }
                    });
                    for(int i=0; i<mediaDownloadEntities.size(); i++){
                        RadioButton rb=new RadioButton(context);
                        String text;
                        if(mediaDownloadEntities.get(i).getMediaType()==1){
                            text = mediaDownloadEntities.get(i).getDimensions() + " " + getString(R.string.image);
                        }
                        else{
                            text = mediaDownloadEntities.get(i).getDimensions() + " " + getString(R.string.video);
                        }
                        rb.setText(text);
                        rb.setId(i);
                        rg.addView(rb);
                    }
                    rg.check(0);
                    progressHide(0);
                    dialog.show();

                }
            });
        }
        else{
            backgroundThreadShortToast(getString(R.string.gen_error),context);
        }
    }


    public void downloadMedia(final MediaDownloadEntity media, final String username, final Context context){
        progressShow(1);
        OkHttpClient client = new OkHttpClient.Builder().build();
        final Request request = new Request.Builder()
                .url(media.getUrl())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                backgroundThreadShortToast(context.getString(R.string.net_err),context);
                progressHide(1);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if(response.isSuccessful()){
                    final ResponseBody responseBody = response.body();
                    if (responseBody != null){
                        InputStream inputStream = responseBody.byteStream();
                        Uri uri = null;
                        if (media.getMediaType() == 1){
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            File saveDir = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES),
                                    context.getString(R.string.app_name));
                            if(!saveDir.exists()){
                                if(!saveDir.mkdirs()){
                                    Util.checkPermission(getActivity());
                                    progressHide(1);
                                    return;
                                }
                            }
                            String fileName = "ST_" + username + "_" + media.getDimensions() +
                                    media.getId() + ".jpg";
                            File file = new File(saveDir, fileName);
                            if (file.exists()){
                                file.delete();
                            }
                            FileOutputStream out;
                            try{
                                out = new FileOutputStream(file);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                out.flush();
                                out.close();
                                uri = FileProvider.getUriForFile(context,
                                        context.getApplicationContext().getPackageName() + ".mfileprovider",
                                        file);

                                MediaScannerConnection.scanFile(context, new String[] {file.getAbsolutePath()},
                                        new String[] {"image/*"}, null);
                            }
                            catch (IOException e){
                                Util.checkPermission(getActivity());
                                progressHide(1);
                                return;
                            }

                        }
                        else if (media.getMediaType() == 2){
                            File saveDir = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_MOVIES),
                                    context.getString(R.string.app_name));
                            if(!saveDir.exists()){
                                if(!saveDir.mkdirs()){
                                    Util.checkPermission(getActivity());
                                    progressHide(1);
                                    return;
                                }
                            }
                            String fileName = "ST_" + username + "_" + media.getDimensions() +
                                    media.getId() + ".mp4";
                            File file = new File(saveDir, fileName);
                            if (file.exists()){
                                file.delete();
                            }
                            FileOutputStream out;
                            byte[] buff = new byte[1024 * 4];
                            try{
                                out = new FileOutputStream(file);
                                while (true) {
                                    int readed = inputStream.read(buff);

                                    if (readed == -1) {
                                        break;
                                    }
                                    out.write(buff, 0, readed);
                                }
                                out.flush();
                                out.close();
                                uri = FileProvider.getUriForFile(context,
                                        context.getApplicationContext().getPackageName() + ".mfileprovider",
                                        file);

                                MediaScannerConnection.scanFile(context, new String[] {file.getAbsolutePath()},
                                        new String[] {"video/*"}, null);
                            }
                            catch (IOException e){
                                Util.checkPermission(getActivity());
                                progressHide(1);
                                return;
                            }
                        }
                        progressHide(1);
                        NotificationManager nm =(NotificationManager) context.getSystemService(
                                Context.NOTIFICATION_SERVICE);
                        if (uri != null && nm != null){
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            if (media.getMediaType()==1){
                                intent.setDataAndType(uri, "image/*");
                            }
                            else{
                                intent.setDataAndType(uri, "video/*");
                            }
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                    intent, PendingIntent.FLAG_CANCEL_CURRENT);
                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context ,
                                    Util.NOTIFICATION_CHANNEL_ID ) ;
                            mBuilder.setContentTitle(context.getString(R.string.app_name));
                            mBuilder.setContentIntent(contentIntent);
                            if (media.getMediaType()==1){
                                mBuilder.setSmallIcon(R.drawable.ic_menu_camera);
                            }
                            else{
                                mBuilder.setSmallIcon(R.drawable.ic_menu_slideshow);
                            }
                            mBuilder.setAutoCancel(true);
                            mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            mBuilder.setVibrate(new long[] {100,100});
                            mBuilder.setContentText( String.format("Story of %s downloaded. [%s]",
                                    username,media.getDimensions()) );
                            nm.notify((int)System.currentTimeMillis() , mBuilder.build()) ;
                        }

                    }

                }
            }
        });

    }


    public void backgroundThreadShortToast(final String msg, final Context context) {
        if (context != null && msg != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    public void progressShow(int freezeUI){
        if (freezeUI==1  && getActivity()!= null){
            this.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Objects.requireNonNull(getActivity()).getWindow().
                            setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            });
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressView.setVisibility(ViewAnimator.VISIBLE);
            }
        });
    }


    public void progressHide(int freezeUI){
        if (freezeUI==1 && getActivity()!= null){
            this.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Objects.requireNonNull(getActivity()).getWindow()
                            .clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            });

        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressView.setVisibility(ViewAnimator.INVISIBLE);
            }
        });
    }

}
