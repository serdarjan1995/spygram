package com.inview.instagram.story.downloader;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CustomListView extends ArrayAdapter<String> {

    private ArrayList<UserIG> followers;
    private Activity context;
    private boolean allHasStory;
    private Handler handler;
    private ViewAnimator progressView;


    @Override
    public int getCount() {
        return followers.size();
    }


    public CustomListView(Activity context, ArrayList<UserIG> followers, boolean allHaveStory) {
        super(context, R.layout.followers_list);
        this.context = context;
        this.followers = followers;
        this.allHasStory = allHaveStory;
        handler = new Handler(Looper.getMainLooper());
        progressView = context.findViewById(R.id.progress_main);
    }


    @Override
    public View getView(int position, View convertView, @NotNull final ViewGroup parent) {
        View r = convertView;
        final ViewHolder viewHolder;
        final int finalPositionInt = position;

        if (r == null)
        {
            LayoutInflater layoutInflater = context.getLayoutInflater();
            r=layoutInflater.inflate(R.layout.followers_list,null,true);
            viewHolder = new ViewHolder(r);
            r.setTag(viewHolder);

        }
        else{
            viewHolder = (ViewHolder) r.getTag();
        }

        Picasso.get().load(followers.get(position).getPp_url()).into(viewHolder.avatar);
        viewHolder.name_text.setText(followers.get(position).getFull_name());
        viewHolder.username_text.setText(followers.get(position).getUsername());
        if(allHasStory){
            viewHolder.hasStory.setVisibility(View.VISIBLE);
        }
        else if(followers.get(position).getLatest_reel_media() != 0){
            viewHolder.hasStory.setVisibility(View.VISIBLE);
        }
        else{
            viewHolder.hasStory.setVisibility(View.INVISIBLE);
        }

        viewHolder.downloadPpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.checkPermission(context);
                if (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                progressShow();
                String url = Util.URL_PP_DOWNLOAD +
                        followers.get(finalPositionInt).getUserId();
                OkHttpClient client = new OkHttpClient.Builder().build();
                final Request request = new Request.Builder()
                        .url(url)
                        .header("x-requested-with", "0")
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        e.printStackTrace();
                        backgroundThreadShortToast(context.getString(R.string.net_err));
                        progressHide();
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        if(response.isSuccessful()){
                            final ResponseBody responseBody = response.body();
                            if (responseBody != null){
                                JSONObject jsonResponse;
                                try {
                                    jsonResponse = new JSONObject(responseBody.string());
                                    if (jsonResponse.has("user")){
                                       boolean success = true;
                                        JSONObject user = jsonResponse.getJSONObject("user");
                                        ArrayList<MediaDownloadEntity> imageDownloadEntities = new ArrayList<>();

                                        if (user.has("hd_profile_pic_url_info")){
                                            JSONObject hd_profile_pic_url_info = user.getJSONObject(
                                                    "hd_profile_pic_url_info");
                                            imageDownloadEntities.add(new MediaDownloadEntity(
                                                    hd_profile_pic_url_info
                                                            .getString("url"),
                                                    hd_profile_pic_url_info
                                                            .getString("height"),
                                                    hd_profile_pic_url_info
                                                            .getString("width"),
                                                    1,
                                                    ""));
                                        }
                                        else{
                                            success = false;
                                        }

                                        if (user.has("hd_profile_pic_versions")){
                                            JSONArray hd_profile_pic_versions = user.getJSONArray(
                                                    "hd_profile_pic_versions");
                                            for(int i=0; i<hd_profile_pic_versions.length(); i++){
                                                imageDownloadEntities.add(new MediaDownloadEntity(
                                                        hd_profile_pic_versions.getJSONObject(i)
                                                                .getString("url"),
                                                        hd_profile_pic_versions.getJSONObject(i)
                                                                .getString("height"),
                                                        hd_profile_pic_versions.getJSONObject(i)
                                                                .getString("width"),
                                                        1,
                                                        ""));
                                            }
                                        }
                                        else{
                                            success = false;
                                        }

                                        if (success){
                                            backgroundThreadDialog(imageDownloadEntities,
                                                    followers.get(finalPositionInt).getUsername());
                                            progressHide();
                                        }
                                        else{
                                            //backgroundThreadShortToast("Please try again!");
                                            downloadFromInstaDp(followers.get(finalPositionInt).getUsername());
                                        }

                                    }
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                    backgroundThreadShortToast(context.getString(R.string.net_err));
                                    progressHide();
                                }
                            }
                            else{
                                backgroundThreadShortToast("Please try again!");
                                progressHide();
                            }

                        }
                        else{
                            downloadFromInstaDp(followers.get(finalPositionInt).getUsername());
                        }
                    }
                });
            }
        });

        return r;
    }


    public void downloadFromInstaDp(final String username){
        String url = Util.URL_PP_DOWNLOAD_IZ;
        OkHttpClient client = new OkHttpClient.Builder().build();
        RequestBody req = new okhttp3.FormBody.Builder()
                .add("submit", username)
                .build();
        final Request request = new Request.Builder()
                .url(url)
                .post(req)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                backgroundThreadShortToast(context.getString(R.string.net_err));
                progressHide();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response)
                    throws IOException {
                ResponseBody responseBody = response.body();
                if (responseBody == null){
                    backgroundThreadShortToast(context.getString(R.string.net_err));
                    return;
                }
                String response_str = responseBody.string();
                Pattern pattern = Pattern.compile(
                        "https?://[-a-zA-Z0-9@:%._+~/?&;#=]+.jpe?g[-a-zA-Z0-9@:%._+~/?&;#=]+");
                Matcher matcher = pattern.matcher(response_str);

                if (matcher.find()) {
                    String ppUrl = matcher.group();
                    ppUrl = ppUrl.replace("&amp;","&");
                    downloadImage(ppUrl, username, "0");
                }
                else{
                    backgroundThreadShortToast(context.getString(R.string.net_err));
                }
                progressHide();
            }
        });
    }


    public void backgroundThreadShortToast(final String msg) {
        if (context != null && msg != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    public void backgroundThreadDialog(final ArrayList<MediaDownloadEntity> images,
                                       final String username) {
        if (context != null && images != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    final Dialog dialog = new Dialog(context);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.radiobutton_dialog);
                    final RadioGroup rg = dialog.findViewById(R.id.radio_group);
                    Button downloadButton = dialog.findViewById(R.id.radiogroup_button);
                    downloadButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (ContextCompat.checkSelfPermission(context,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                downloadImage(
                                        images.get(rg.getCheckedRadioButtonId()).getUrl(),
                                        username,
                                        images.get(rg.getCheckedRadioButtonId()).getDimensions());
                                dialog.dismiss();
                            }
                            else{
                                Util.checkPermission(context);
                            }

                        }
                    });
                    for(int i=0; i<images.size(); i++){
                        RadioButton rb=new RadioButton(context);
                        rb.setText(images.get(i).getDimensions());
                        rb.setId(i);
                        rg.addView(rb);
                    }
                    rg.check(0);
                    dialog.show();

                }
            });
        }
    }


    public void downloadImage(String url, final String username, final String dim){
        OkHttpClient client = new OkHttpClient.Builder().build();
        final Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                backgroundThreadShortToast(context.getString(R.string.net_err));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if(response.isSuccessful()){
                    final ResponseBody responseBody = response.body();
                    if (responseBody != null){
                        InputStream inputStream = responseBody.byteStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        File saveDir = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES),
                                context.getString(R.string.app_name));
                        if(!saveDir.exists()){
                            if(!saveDir.mkdirs()){
                                Util.checkPermission(context);
                                return;
                            }
                        }
                        String fileName = "PP_" + username + "_" + dim + ".jpg";
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
                            MediaScannerConnection.scanFile(context, new String[] {
                                    file.getAbsolutePath()},
                                    new String[] {"image/*"}, null);
                        }
                        catch (IOException e){
                            Util.checkPermission(context);
                            return;
                        }

                        NotificationManager nm =(NotificationManager) context.getSystemService(
                                Context.NOTIFICATION_SERVICE);
                        if (nm!=null){
                            Uri uri = FileProvider.getUriForFile(context,
                                    context.getApplicationContext()
                                            .getPackageName() + ".mfileprovider", file);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, "image/*");
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                    intent, PendingIntent.FLAG_CANCEL_CURRENT);
                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context ,
                                    Util.NOTIFICATION_CHANNEL_ID ) ;
                            mBuilder.setContentTitle(context.getString(R.string.app_name));
                            mBuilder.setContentIntent(contentIntent);
                            mBuilder.setAutoCancel(true);
                            mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            mBuilder.setVibrate(new long[] {100,100});
                            mBuilder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY);
                            mBuilder.setSmallIcon(R.drawable.ic_wallpaper_black_24dp);
                            mBuilder.setContentText( String.format(
                                    context.getString(R.string.pp_downloaded),
                                    username,
                                    dim) );
                            nm.notify((int)System.currentTimeMillis() , mBuilder.build()) ;
                        }
                    }
                }
            }
        });

    }

    static class ViewHolder
    {
        TextView name_text;
        TextView username_text;
        ImageView avatar;
        ImageView hasStory;
        ImageView downloadPpButton;
        ViewHolder(View v)
        {
            name_text = v.findViewById(R.id.text_view_name);
            username_text = v.findViewById(R.id.text_view_username);
            avatar = v.findViewById(R.id.avatar);
            hasStory = v.findViewById(R.id.hasstoryindicator);
            downloadPpButton = v.findViewById(R.id.download_pp_button);
        }
    }


    public void progressShow(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressView.setVisibility(ViewAnimator.VISIBLE);
                context.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });
    }


    public void progressHide(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressView.setVisibility(ViewAnimator.INVISIBLE);
                context.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });
    }

}
