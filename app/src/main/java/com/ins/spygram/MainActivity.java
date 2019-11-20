package com.ins.spygram;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.view.MenuItem;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.onesignal.OneSignal;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import android.view.Menu;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private String USER_ID;
    private String SESSION_ID;
    private FragmentManager fragmentManager;
    private ViewFragment keyFragment;
    private ViewFragment followersFragment;
    private String checkDecryptionString;
    private String checkDecryptionSuccess;
    private String initVector;
    private AdView bannerAdView;
    private UnifiedNativeAd nativeAd;
    private boolean loggedOut = false;
    private FirebaseRemoteConfig firebaseRemoteConfig;
    final String VERSION_CODE_KEY = "version_code";
    final String UPDATE_URL = "update_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        NotificationManager nm =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String Description = "Spygram notification channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(Util.NOTIFICATION_CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.GRAY);
            mChannel.enableVibration(false);
            mChannel.setSound(null, null);
            mChannel.setShowBadge(false);
            nm.createNotificationChannel(mChannel);
        }


        //force update
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        HashMap<String, Object> remoteConfigDefaults = new HashMap<>();
        remoteConfigDefaults.put(VERSION_CODE_KEY, BuildConfig.VERSION_CODE);
        firebaseRemoteConfig.setDefaultsAsync(remoteConfigDefaults);
        firebaseRemoteConfig.setConfigSettingsAsync(new FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(30).build());

        firebaseRemoteConfig.fetch().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            firebaseRemoteConfig.activate();
                            checkForUpdate();
                        }
                        else{
                            toastMsg(getString(R.string.version_check_error_msg));
                        }
                    }
                });


        // OneSignal Initialization
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();


        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        FrameLayout adContainerView = findViewById(R.id.ad_view_container);
        bannerAdView = new AdView(this);
        bannerAdView.setAdUnitId(getString(R.string.banner_unit_id));
        adContainerView.addView(bannerAdView);



        keyFragment = new ViewFragment(R.layout.content_key);
        followersFragment = new ViewFragment(R.layout.content_followers);
        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, keyFragment).commit();
        loadNativeAd();
        loadBanner();
        Util.checkPermission(this);
        getSharedPreferencesValues();
    }

    private void checkForUpdate() {
        int latestAppVersion = (int) firebaseRemoteConfig.getDouble(VERSION_CODE_KEY);
        final String update_url = firebaseRemoteConfig.getString(UPDATE_URL);
        if (latestAppVersion > BuildConfig.VERSION_CODE) {
            new AlertDialog.Builder(this).setTitle(getString(R.string.msg_update_pls))
                    .setMessage(getString(R.string.new_version_available))
                    .setPositiveButton( getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {


                                    final String appPackageName = getPackageName();
                                    try {
                                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                                Uri.parse("market://details?id=" + appPackageName));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();

                                    } catch (android.content.ActivityNotFoundException e) {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(update_url)));
                                        finish();
                                    }
                            }
                    })
                    .setNegativeButton(getString(R.string.exit),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                            }
                    })
                    .setCancelable(false).show();
        }
    }

    private void loadBanner() {
        AdRequest adRequest =
                new AdRequest.Builder().addTestDevice("6F4C8BD9AE078F1B48B1D1F439EF5039").build();

        AdSize adSize = getAdSize();
        if (bannerAdView.getAdSize() == null){
            bannerAdView.setAdSize(adSize);
        }


        bannerAdView.loadAd(adRequest);
    }

    private AdSize getAdSize() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    private void loadNativeAd(){
        AdLoader adLoader = new AdLoader.Builder(this, getString(R.string.native_ad_unit_id))
                .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        FrameLayout frameLayout =
                                findViewById(R.id.nativeadframe);
                        UnifiedNativeAdView adView = (UnifiedNativeAdView) getLayoutInflater()
                                .inflate(R.layout.ad_unified, null);
                        populateUnifiedNativeAdView(unifiedNativeAd, adView);
                        frameLayout.removeAllViews();
                        frameLayout.addView(adView);


                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());

    }


    private void populateUnifiedNativeAdView(UnifiedNativeAd nativeAd, UnifiedNativeAdView adView) {
        // Set the media view.
        adView.setMediaView((MediaView) adView.findViewById(R.id.ad_media));

        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // The headline and mediaContent are guaranteed to be in every UnifiedNativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        adView.getMediaView().setMediaContent(nativeAd.getMediaContent());

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(
                    nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView())
                    .setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd);

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        VideoController vc = nativeAd.getVideoController();

        // Updates the UI to say whether or not this ad has a video asset.
        if (vc.hasVideoContent()) {

            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {
                    // Publishers should allow native ads to complete video playback before
                    // refreshing or replacing them with another ad in the same UI location.
                    refreshAd();
                    super.onVideoEnd();
                }
            });
        }
    }


    private void refreshAd() {

        AdLoader.Builder builder = new AdLoader.Builder(this, getString(R.string.native_ad_unit_id));

        builder.forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
            // OnUnifiedNativeAdLoadedListener implementation.
            @Override
            public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                // You must call destroy on old ads when you are done with them,
                // otherwise you will have a memory leak.
                if (nativeAd != null) {
                    nativeAd.destroy();
                }
                nativeAd = unifiedNativeAd;
                FrameLayout frameLayout =
                        findViewById(R.id.nativeadframe);
                UnifiedNativeAdView adView = (UnifiedNativeAdView) getLayoutInflater()
                        .inflate(R.layout.ad_unified, null);
                populateUnifiedNativeAdView(unifiedNativeAd, adView);
                frameLayout.removeAllViews();
                frameLayout.addView(adView);
            }

        });

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();

        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int errorCode) {
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }


    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length == 0
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    backgroundThreadShortToast("WRITE permissions needed to download content.");
                }
                break;
            }
        }
    }



    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.download_pp_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        loadBanner();
        switch (id){
            case R.id.nav_key:
                fragmentManager.beginTransaction().replace(R.id.content_frame, keyFragment).commit();
                refreshAd();
                break;
            case R.id.nav_story_reels:
                if( checkDecryptionString.equals(checkDecryptionSuccess)){
                    fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment).commit();
                    getStoryReels();
                }
                else {
                    toastMsg(getString(R.string.keyphrase_error_msg));
                }
                break;
            case R.id.nav_followers:
                if( checkDecryptionString.equals(checkDecryptionSuccess)){
                    fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment).commit();
                    getFollows(USER_ID,1);
                }
                else{
                    toastMsg(getString(R.string.keyphrase_error_msg));
                }
                break;
            case R.id.nav_followees:
                if( checkDecryptionString.equals(checkDecryptionSuccess)){
                    fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment).commit();
                    getFollows(USER_ID,2);
                }
                else{
                    toastMsg(getString(R.string.keyphrase_error_msg));
                }
                break;
            case R.id.nav_clear_session:
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.clear_session_msg))
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if( checkDecryptionString.equals(checkDecryptionSuccess)){
                                    logout();
                                }
                                SharedPreferences sharedPreferences = getSharedPreferences(getApplicationContext()
                                        .getPackageName(), Context.MODE_PRIVATE);
                                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                                sharedPreferencesEditor.clear();
                                sharedPreferencesEditor.apply();
                                if (loggedOut){
                                    Toast.makeText(getApplicationContext(), getString(R.string.clear_session_logout),
                                            Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(getApplicationContext(), getString(R.string.clear_session_destroy),
                                            Toast.LENGTH_SHORT).show();
                                }

                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                startActivityForResult(intent,0);
                            }
                        })
                        .setNegativeButton(getString(R.string.no), null)
                        .show();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onClickKeypassBtn(View v){
        EditText text = findViewById(R.id.setkeyphrasetext);
        int status = decryptArguments(text.getText().toString());
        if (status == 0){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null){
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            v.clearFocus();
            fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment)
                    .commitAllowingStateLoss();
            getStoryReels();

        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                String keyphrase = data.getStringExtra("keyphrase");
                if (keyphrase != null) {
                    getSharedPreferencesValues();
                    int status = decryptArguments(keyphrase);
                    if (status == 0) {
                        Toast.makeText(this, getString(R.string.all_right), Toast.LENGTH_SHORT).show();
                        fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment)
                                .commitAllowingStateLoss();
                        getFollows(USER_ID, 2);
                    }
                }
            }
        }
    }

    public void getFollows(String id, int followType){
        OkHttpClient client = Util.getHttpClient();
        String url;
        if (followType == 1){
            url = getString(R.string.url_host) + getString(R.string.path_get_followers);
            url = String.format(url,id);
        }
        else{
            url = getString(R.string.url_host) + getString(R.string.path_get_followees);
            url = String.format(url,id);
        }

        final ArrayList <UserFollow> userFollowArrayList = new ArrayList<>();
        Request request = Util.getRequestHeaderBuilder(url, SESSION_ID, getString(R.string.user_agent),
                getString(R.string.content_type)).build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                backgroundThreadShortToast(getString(R.string.net_err));
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.isSuccessful()){
                    ResponseBody responseBody = response.body();
                    final String followers_response;
                    if (responseBody != null){
                        followers_response = responseBody.string();
                    }
                    else{
                        backgroundThreadShortToast(getString(R.string.smth_wrong));
                        return;
                    }

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSONObject json_followers;
                            try {
                                json_followers = new JSONObject(followers_response);

                                JSONArray users  = json_followers.getJSONArray("users");
                                String userId;
                                String username;
                                String pp_url;
                                String full_name;
                                long latest_reel_media;
                                JSONObject user;
                                UserFollow userFollow;
                                for (int i = 0; i < users.length(); i++) {
                                    user = users.getJSONObject(i);
                                    userId = Long.toString(user.getLong("pk"));
                                    username = user.getString("username");
                                    pp_url = user.getString("profile_pic_url");
                                    full_name = user.getString("full_name");
                                    latest_reel_media = user.getLong("latest_reel_media");
                                    userFollow = new UserFollow(userId,username,pp_url,full_name,latest_reel_media);
                                    userFollowArrayList.add(userFollow);
                                }

                                CustomListView customListview = new CustomListView(MainActivity.this,
                                        userFollowArrayList, false);
                                ListView lst = findViewById(R.id.listview_followers);
                                lst.setAdapter(customListview);
                                lst.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                        getStoryOfUser(userFollowArrayList.get(i).getUserId());
                                    }
                                });


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            catch (Exception e) {
                                Log.e("ERROR","General error occurred. ");
                                e.printStackTrace();
                            }
                        }
                    });

                }
            }
        });
    }


    public void getStoryOfUser(String userId){
        OkHttpClient client = Util.getHttpClient();
        String url = getString(R.string.url_host) + getString(R.string.path_get_story);
        url = String.format(url,userId);
        final Request request = Util.getRequestHeaderBuilder(url, SESSION_ID, getString(R.string.user_agent),
                getString(R.string.content_type)).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                backgroundThreadShortToast(getString(R.string.net_err));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.isSuccessful()){
                    ResponseBody responseBody = response.body();
                    final String responseGetStory;
                    if (responseBody != null){
                        responseGetStory = responseBody.string();
                    }
                    else{
                        toastMsg(getString(R.string.smth_wrong));
                        return;
                    }

                    JSONObject json;
                    try {
                        json = new JSONObject(responseGetStory);
                        if (json.has("reel") && json.isNull("reel")) {
                            backgroundThreadShortToast(getString(R.string.no_story));
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(MainActivity.this, StoryViewer.class);
                            Bundle b = new Bundle();
                            b.putString("response", responseGetStory);
                            intent.putExtras(b);
                            startActivity(intent);
                        }
                    });
                }
            }
        });
    }

    public void getStoryReels(){
        final ArrayList <UserFollow> userFollowArrayList = new ArrayList<>();
        String url = getString(R.string.url_host) + getString(R.string.path_reels_tray);
        OkHttpClient client = Util.getHttpClient();
        final Request request = Util.getRequestHeaderBuilder(url, SESSION_ID,
                getString(R.string.user_agent),
                getString(R.string.content_type))
                .post(new okhttp3.FormBody.Builder().build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                backgroundThreadShortToast(getString(R.string.net_err));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if(response.isSuccessful()){
                    final ResponseBody responseBody = response.body();
                    if (responseBody != null){
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONObject responseJson = new JSONObject(responseBody.string());
                                    if (responseJson.has("status") &&
                                            responseJson.getString("status").equals("ok") &&
                                            responseJson.has("status")) {
                                        JSONArray tray = responseJson.getJSONArray("tray");
                                        String userId;
                                        String username;
                                        String pp_url;
                                        String full_name;
                                        JSONObject user;
                                        UserFollow userFollow;
                                        for (int i = 0; i < tray.length(); i++) {
                                            user = tray.getJSONObject(i).getJSONObject("user");
                                            userId = Long.toString(user.getLong("pk"));
                                            username = user.getString("username");
                                            pp_url = user.getString("profile_pic_url");
                                            full_name = user.getString("full_name");
                                            userFollow = new UserFollow(userId, username, pp_url, full_name, 0);
                                            userFollowArrayList.add(userFollow);
                                        }
                                        CustomListView customListview = new CustomListView(MainActivity.this,
                                                userFollowArrayList, true);
                                        ListView lst = findViewById(R.id.listview_followers);
                                        lst.setAdapter(customListview);
                                        lst.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                                getStoryOfUser(userFollowArrayList.get(i).getUserId());
                                            }
                                        });

                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                    }
                }
            }
        });
    }


    public void getSharedPreferencesValues(){
        SharedPreferences sharedPreferences = this.getSharedPreferences(getApplicationContext()
                .getPackageName(), Context.MODE_PRIVATE);

        if (sharedPreferences.contains("logged_in") &&
                sharedPreferences.getInt("logged_in", 0) == 1){
            try{
                USER_ID = sharedPreferences.getString("user_id", null);
                SESSION_ID = sharedPreferences.getString("session_id", null);
                checkDecryptionSuccess = sharedPreferences.getString("check", null);
                checkDecryptionString = sharedPreferences.getString("checkEnc", null);
                initVector = sharedPreferences.getString("init_vector", null);
            }
            catch (Exception e){
                e.printStackTrace();
            }

        }
        else{
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivityForResult(intent,0);
        }
    }

    public int decryptArguments(String keyPhrase){
        String decryptedTest = Util.decrypt(checkDecryptionString,keyPhrase,initVector);
        if (decryptedTest == null){
            toastMsg(getString(R.string.keyphrase_wrong));
            return -1;
        }
        byte[] decryptedTestInBytes = decryptedTest.getBytes(StandardCharsets.UTF_8);
        String decryptedTestMD5 = Util.toHexString(Util.md5hash(Util.md5hash(decryptedTestInBytes)));
        if (decryptedTestMD5.equals(checkDecryptionSuccess)){
            USER_ID = Util.decrypt(USER_ID,keyPhrase,initVector);
            SESSION_ID = Util.decrypt(SESSION_ID,keyPhrase,initVector);
            SESSION_ID = "sessionid=" + SESSION_ID;
            checkDecryptionString = decryptedTestMD5;
            toastMsg(getString(R.string.keyphrase_right));
            return 0;
        }
        return -1;
    }


    public void toastMsg(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void backgroundThreadShortToast(final String msg) {
        final Context context = getApplicationContext();
        if (context != null && msg != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void logout(){
        String urlLogout = getString(R.string.url_host) + getString(R.string.path_logout);
        OkHttpClient client = Util.getHttpClient();
        final Request request = new Request.Builder()
                .url(urlLogout)
                .header("User-Agent", getString(R.string.user_agent))
                .addHeader("Cookie", SESSION_ID)
                .addHeader("Content-Type", getString(R.string.content_type))
                .post(new okhttp3.FormBody.Builder().build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                loggedOut = false;
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.isSuccessful()){
                    ResponseBody responseBody = response.body();
                    if (responseBody != null){
                        try {
                            JSONObject responseJson = new JSONObject(responseBody.string());
                            if (responseJson.has("status") &&
                                    responseJson.getString("status").equals("ok")){
                                loggedOut = true;

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

}
