package com.inview.instagram.story.downloader;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
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
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.remoteconfig.BuildConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.onesignal.OneSignal;
import com.squareup.picasso.Picasso;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private String USER_ID;
    private String SESSION_ID;
    private FragmentManager fragmentManager;
    private ViewFragment keyFragment;
    private ViewFragment downloadByLinkFragment;
    private ViewFragment followersFragment;
    private ViewFragment searchUserFragment;
    private String checkDecryptionString;
    private String checkDecryptionSuccess;
    private String initVector;
    private AdView bannerAdView;
    private UnifiedNativeAd nativeAd;
    private UnifiedNativeAd nativeAdDownloadByLink;
    private UnifiedNativeAd nativeAdUserSearch;
    private boolean loggedOut = false;
    private Handler handler;
    private ViewAnimator progressView;
    private FirebaseRemoteConfig firebaseRemoteConfig;
    final String VERSION_CODE_KEY = "version_code";
    final String UPDATE_URL = "update_url";
    private boolean publicProfile = true;
    private boolean hasPrevSession = false;
    private boolean hasOpenSession = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.nav_header_title);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        final NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        handler = new Handler(this.getMainLooper());
        progressView = findViewById(R.id.progress_main);

        NotificationManager nm =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            String description = getString(R.string.app_name) + " notification channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(
                    Util.NOTIFICATION_CHANNEL_ID,
                    name,
                    importance);
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.YELLOW);
            mChannel.enableVibration(true);
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

        // admob initialization
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        FrameLayout adContainerView = findViewById(R.id.ad_view_container);
        bannerAdView = new AdView(this);
        bannerAdView.setAdUnitId(Util.BANNER_UNIT_ID);
        adContainerView.addView(bannerAdView);
        View headerView = navigationView.getHeaderView(0);
        SwitchMaterial publicSwitch = headerView.findViewById(R.id.public_switch);
        publicSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    enablePublicProfile();
                }
                else{
                    disablePublicProfile();
                }
            }
        });

        //prepare UI
        keyFragment = new ViewFragment(R.layout.content_key);
        downloadByLinkFragment = new ViewFragment(R.layout.content_download_by_link);
        followersFragment = new ViewFragment(R.layout.content_followers);
        searchUserFragment = new ViewFragment(R.layout.content_user_search);
        fragmentManager = getSupportFragmentManager();

        //check saved session
        getSharedPreferencesValues();

        if(hasPrevSession){
            navigationView.getMenu().getItem(0).setChecked(true);
            fragmentManager.beginTransaction().replace(R.id.content_frame, keyFragment).commit();
        }
        else{
            navigationView.getMenu().getItem(5).setChecked(true);
            fragmentManager.beginTransaction().replace(R.id.content_frame, downloadByLinkFragment).commit();
        }

        //load ads
        loadNativeAd();
        loadBanner();

        // TODO: turning off publicSwitch currently
        publicSwitch.setVisibility(SwitchMaterial.INVISIBLE);
    }


    /**
     * checks current app version. if newer is available, forces to update
     * redirects to google play
    **/
    private void checkForUpdate() {
        int latestAppVersion = (int) firebaseRemoteConfig.getLong(VERSION_CODE_KEY);
        final String update_url = firebaseRemoteConfig.getString(UPDATE_URL);
        int buildVersionCode = -1;
        try {
            buildVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }
        if (latestAppVersion > buildVersionCode) {
            new AlertDialog.Builder(this).setTitle(getString(R.string.msg_update_pls))
                    .setMessage(getString(R.string.new_version_available))
                    .setPositiveButton( getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                                Uri.parse(update_url));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();

                                    } catch (android.content.ActivityNotFoundException e) {
                                        backgroundThreadShortToast(getString(R.string.gen_error));
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


    /**
     * Banner ads. Load and update
     */
    private void loadBanner() {
        AdRequest adRequest = new AdRequest.Builder().build();
        AdSize adSize = getAdSize();
        if (bannerAdView.getAdSize() == null){
            bannerAdView.setAdSize(adSize);
        }
        bannerAdView.loadAd(adRequest);
    }


    /**
     * Check size for ads area  defined on UI.
     */
    private AdSize getAdSize() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }


    /**
     * Native ads. Load and update
     * There are 3 native add fields on UI.
     * Check which one of them active then apply procedures
     */
    private void loadNativeAd(){
        final NavigationView navigationView = findViewById(R.id.nav_view);
        AdLoader adLoader = new AdLoader.Builder(this, Util.NATIVE_AD_UNIT_ID)
                .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                    @Override
                    public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                        FrameLayout frameLayout;
                        if (navigationView.getMenu().getItem(0).isChecked()){
                            frameLayout = findViewById(R.id.nativeadframe);
                        }else if (navigationView.getMenu().getItem(4).isChecked()){
                            frameLayout = findViewById(R.id.native_ad_search_user);
                        }else {
                            frameLayout = findViewById(R.id.nativeadframe_downloadbylink);
                        }
                        if (frameLayout != null){
                            UnifiedNativeAdView adView = (UnifiedNativeAdView) getLayoutInflater()
                                    .inflate(R.layout.ad_unified, null);
                            populateUnifiedNativeAdView(unifiedNativeAd, adView);
                            frameLayout.removeAllViews();
                            frameLayout.addView(adView);
                        }
                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }


    /**
     * Get advertisement and apply necessary field to UI
     * For native ads!!!
     */
    private void populateUnifiedNativeAdView(UnifiedNativeAd nativeAd, UnifiedNativeAdView adView){
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
        VideoController vc = nativeAd.getMediaContent().getVideoController();

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


    /**
     * Refresh advertisement. Get new one if applicable
     */
    private void refreshAd(){
        AdLoader.Builder builder = new AdLoader.Builder(this, Util.NATIVE_AD_UNIT_ID);
        builder.forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
            // OnUnifiedNativeAdLoadedListener implementation.
            @Override
            public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                if (nativeAd != null) {
                    nativeAd.destroy();
                }
                if (nativeAdDownloadByLink != null) {
                    nativeAdDownloadByLink.destroy();
                }
                if (nativeAdUserSearch != null) {
                    nativeAdUserSearch.destroy();
                }

                NavigationView navigationView = findViewById(R.id.nav_view);
                FrameLayout frameLayout;
                if (navigationView.getMenu().getItem(0).isChecked()){
                    frameLayout = findViewById(R.id.nativeadframe);
                    nativeAd = unifiedNativeAd;
                }else if (navigationView.getMenu().getItem(4).isChecked()){
                    frameLayout = findViewById(R.id.native_ad_search_user);
                    nativeAdUserSearch = unifiedNativeAd;
                }else {
                    frameLayout = findViewById(R.id.nativeadframe_downloadbylink);
                    nativeAdDownloadByLink = unifiedNativeAd;
                }
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

        AdLoader adLoader = builder.withAdListener(new AdListener() {}).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }


    /**
     * Callback for WRITE_EXTERNAL_STORAGE
     * If user doesn't allow, then notify about it.
     */
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions,
                                           @NotNull int[] grantResults){
        if (requestCode == 1) {
            if (grantResults.length == 0
                    || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                backgroundThreadShortToast(getString(R.string.write_perm));
            }
        }
    }


    /**
     * Android Back Button control
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else{
            super.onBackPressed();
        }
    }


    /**
     * This method disables menu items available for logged in user
     */
    public void enablePublicProfile(){
        NavigationView navigationView = findViewById(R.id.nav_view);
        Menu nav_Menu = navigationView.getMenu();
        nav_Menu.clear();
        navigationView.inflateMenu(R.menu.activity_main_drawer);
        nav_Menu.findItem(R.id.nav_followers).setVisible(false);
        nav_Menu.findItem(R.id.nav_followees).setVisible(false);
        nav_Menu.findItem(R.id.nav_story_reels).setVisible(false);
        nav_Menu.findItem(R.id.nav_search_profile).setVisible(false);
        if (hasPrevSession){
            nav_Menu.findItem(R.id.nav_key).setVisible(true);
            nav_Menu.findItem(R.id.nav_clear_session).setVisible(true);
            nav_Menu.findItem(R.id.nav_sign_in).setVisible(false);
        }
        else{
            nav_Menu.findItem(R.id.nav_sign_in).setVisible(true);
            nav_Menu.findItem(R.id.nav_clear_session).setVisible(false);
            nav_Menu.findItem(R.id.nav_key).setVisible(false);
        }
        publicProfile = true;
    }


    /**
     * This method enables all menu items
     */
    public void disablePublicProfile(){
        NavigationView navigationView = findViewById(R.id.nav_view);
        Menu nav_Menu = navigationView.getMenu();
        nav_Menu.clear();
        navigationView.inflateMenu(R.menu.activity_main_drawer);
        nav_Menu.findItem(R.id.nav_followers).setVisible(true);
        nav_Menu.findItem(R.id.nav_followees).setVisible(true);
        nav_Menu.findItem(R.id.nav_story_reels).setVisible(true);
        nav_Menu.findItem(R.id.nav_search_profile).setVisible(true);
        if (hasOpenSession){
            nav_Menu.findItem(R.id.nav_followers).setEnabled(true);
            nav_Menu.findItem(R.id.nav_followees).setEnabled(true);
            nav_Menu.findItem(R.id.nav_story_reels).setEnabled(true);
            nav_Menu.findItem(R.id.nav_search_profile).setEnabled(true);
            getUserInfo();
        }
        else{
            nav_Menu.findItem(R.id.nav_followers).setEnabled(false);
            nav_Menu.findItem(R.id.nav_followees).setEnabled(false);
            nav_Menu.findItem(R.id.nav_story_reels).setEnabled(false);
            nav_Menu.findItem(R.id.nav_search_profile).setEnabled(false);
        }
        if (hasPrevSession){
            nav_Menu.findItem(R.id.nav_key).setVisible(true);
            nav_Menu.findItem(R.id.nav_clear_session).setVisible(true);
            nav_Menu.findItem(R.id.nav_sign_in).setVisible(false);
        }
        else{
            nav_Menu.findItem(R.id.nav_key).setVisible(false);
            nav_Menu.findItem(R.id.nav_sign_in).setVisible(true);
            nav_Menu.findItem(R.id.nav_clear_session).setVisible(false);
        }
        publicProfile = false;
    }


    /**
     * Navigation control
     */
    public void getUserInfo(){
        progressShow();
        OkHttpClient client = Util.getHttpClient();
        String url = Util.URL_HOST + Util.PATH_USER_INFO;
        url = String.format(url, USER_ID);
        Request request = Util.getRequestHeaderBuilder(url, SESSION_ID,
                Util.USER_AGENT, Util.CONTENT_TYPE).build();
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
                    final String userInfo;
                    if (responseBody != null){
                        userInfo = responseBody.string();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                JSONObject jsonUserInfo;
                                try {
                                    jsonUserInfo = new JSONObject(userInfo);
                                    JSONObject user  = jsonUserInfo.getJSONObject("user");
                                    String username = user.getString("username");;
                                    String ppUrl = user.getString("profile_pic_url");
                                    TextView profileName = findViewById(R.id.profile_name);
                                    profileName.setText(username);
                                    CircleImageView profileImage = findViewById(R.id.profile_image);
                                    Picasso.get().load(ppUrl)
                                            .placeholder(R.drawable.ic_baseline_account_circle_24)
                                            .error(R.drawable.ic_baseline_account_circle_24)
                                            .into(profileImage);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                catch (Exception e) {
                                    Log.e("ERROR",getString(R.string.gen_error));
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    else{
                        backgroundThreadShortToast(getString(R.string.smth_wrong));
                    }
                }
            }
        });
    }


    /**
     * Navigation control
     */
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
            case R.id.nav_download_by_link:
                fragmentManager.beginTransaction().replace(R.id.content_frame, downloadByLinkFragment).commit();
                refreshAd();
                break;
            case R.id.nav_search_profile:
                fragmentManager.beginTransaction().replace(R.id.content_frame, searchUserFragment).commit();
                refreshAd();
                break;
            case R.id.nav_download_how_to:
                fragmentManager.beginTransaction().replace(
                        R.id.content_frame,
                        new DownloadHowToParentFragment()).commit();
                break;
            case R.id.nav_sign_in:
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivityForResult(intent,0);
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


    /**
     * Check Keypass Button
     * If password is right then we able to decrypt saved session
     *    logic function @decryptArguments();
     */
    public void onClickKeypassBtn(View v){
        progressShow();
        EditText text = findViewById(R.id.setkeyphrasetext);
        int status = decryptArguments(text.getText().toString());
        progressHide();
        if (status == 0){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null){
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            v.clearFocus();
            fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment)
                    .commitAllowingStateLoss();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    getStoryReels();
                }
            }).start();
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.getMenu().getItem(1).setChecked(true);
            disablePublicProfile();
        }
    }


    /**
     * Search User
     */
    public void onClickUserSearch(View v){
        progressShow();
        EditText profileNameEditText = findViewById(R.id.search_username_input);
        String profileName = profileNameEditText.getText().toString();
        getFollows(profileName, 0);
        fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment)
                .commitAllowingStateLoss();
    }


    /**
     * Download with Instagram post link
     */
    public void onClickDownloadByLink(View v){
        Util.checkPermission(this);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            return;
        }
        progressShow();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.getPrimaryClipDescription() != null
                && clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
            if( clipboard.getPrimaryClip() != null){
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                String pasteData = "" + item.getText();
                //if this is Post or IGTV
                if (pasteData.contains("instagram.com/p/") || pasteData.contains("instagram.com/tv/")){
                    String url = pasteData + "&__a=1";
                    OkHttpClient client = Util.getHttpClient();
                    Request request;
                    if (publicProfile){
                        request = Util.getRequestHeaderBuilder(url, "",
                                Util.USER_AGENT,Util.CONTENT_TYPE)
                                .build();
                    }
                    else{
                        request = Util.getRequestHeaderBuilder(url, SESSION_ID,
                                Util.USER_AGENT,Util.CONTENT_TYPE)
                                .build();
                    }
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            backgroundThreadShortToast(getString(R.string.net_err));
                            progressHide();
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response)
                                throws IOException{
                            if(response.isSuccessful()){
                                ResponseBody responseBody = response.body();
                                if (responseBody != null){
                                    ArrayList<MediaDownloadEntity> mediaDownloadEntities =
                                            Util.parseDownloadLinkResponse(responseBody.string());
                                    progressHide();
                                    backgroundThreadDialog(mediaDownloadEntities, MainActivity.this);
                                }
                            }
                        }
                    });
                }
                // is this highlighted story?
                else if (pasteData.contains("instagram.com/s/")){
                    Pattern pattern = Pattern.compile("/s/(.*?)[?]");
                    Matcher matcher = pattern.matcher(pasteData);
                    Pattern pattern_st = Pattern.compile("story_media_id=[0-9_]+");
                    Matcher matcher_st = pattern_st.matcher(pasteData);
                    if (matcher.find()) {
                        String highlight = matcher.group().replace("/s/", "")
                                .replace("?","");
                        matcher_st.find();
                        final String media_id = matcher_st.group()
                                .replace("story_media_id=", "");
                        try{
                            final String highlight_id = new String(Base64.decode(highlight,Base64.NO_WRAP));
                            String url = Util.URL_HOST + Util.PATH_REELS_MEDIA;
                            OkHttpClient client;
                            RequestBody requestBody;
                            try {
                                JSONObject json = new JSONObject();
                                JSONArray user_ids = new JSONArray();
                                user_ids.put(highlight_id);
                                json.put("user_ids", user_ids);
                                client = Util.getHttpClient();
                                requestBody = Util.getRequestBody(json);
                            }
                            catch (Exception e){
                                backgroundThreadShortToast(getString(R.string.net_err));
                                progressHide();
                                return;
                            }
                            Request request;
                            if (publicProfile){
                                request = Util.getRequestHeaderBuilder(url, "",
                                        Util.USER_AGENT,Util.CONTENT_TYPE)
                                        .build();
                            }
                            else{
                                request = Util.getRequestHeaderBuilder(url, SESSION_ID,
                                        Util.USER_AGENT,Util.CONTENT_TYPE)
                                        .build();
                            }
                            client.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                    backgroundThreadShortToast(getString(R.string.net_err));
                                    progressHide();
                                }

                                @Override
                                public void onResponse(@NotNull Call call, @NotNull Response response)
                                        throws IOException{
                                    if(response.isSuccessful()){
                                        ResponseBody responseBody = response.body();
                                        if (responseBody != null){
                                            ArrayList<MediaDownloadEntity> mediaDownloadEntities =
                                                    Util.getHighlightMediaEntities(
                                                            responseBody.string(),
                                                            highlight_id,
                                                            media_id);
                                            progressHide();
                                            if (mediaDownloadEntities.size()>0){
                                                backgroundThreadDialog(mediaDownloadEntities,
                                                        MainActivity.this);
                                            }
                                            else{
                                                backgroundThreadShortToast(getString(
                                                        R.string.clipboard_not_valid));
                                            }
                                        }
                                    }
                                }
                            });
                        }
                        catch (Exception e){
                            toastMsg(getString(R.string.clipboard_not_valid));
                        }

                    }
                }
                // is this current available active story?
                else if (pasteData.contains("instagram.com/stories/")){
                    Pattern pattern = Pattern.compile("/[0-9]+");
                    Matcher matcher = pattern.matcher(pasteData);
                    if (matcher.find()) {
                        final String story_id = matcher.group().replace("/", "");
                        try{
                            String url = pasteData + "&__a=1";
                            OkHttpClient client = Util.getHttpClient();
                            Request request;
                            if (publicProfile){
                                request = Util.getRequestHeaderBuilder(url, "",
                                        Util.USER_AGENT,Util.CONTENT_TYPE)
                                        .build();
                            }
                            else{
                                request = Util.getRequestHeaderBuilder(url, SESSION_ID,
                                        Util.USER_AGENT,Util.CONTENT_TYPE)
                                        .build();
                            }
                            client.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                    backgroundThreadShortToast(getString(R.string.net_err));
                                    progressHide();
                                }

                                @Override
                                public void onResponse(@NotNull Call call, @NotNull Response response){
                                    if(response.isSuccessful()){
                                        ResponseBody responseBody = response.body();
                                        if (responseBody != null){
                                            try {
                                                JSONObject responseJson = new JSONObject(responseBody.string());
                                                progressHide();
                                                if (responseJson.has("user") &&
                                                        responseJson.getJSONObject("user").has("id")) {
                                                    String user_id = responseJson.getJSONObject("user")
                                                            .getString("id");
                                                    getStoryOfUserByStoryId(user_id,story_id);
                                                }
                                                else{
                                                    throw new Exception();
                                                }
                                            }
                                            catch(Exception e){
                                                e.printStackTrace();
                                                progressHide();
                                                backgroundThreadShortToast(
                                                        getString(R.string.clipboard_not_valid));
                                            }
                                        }
                                    }
                                }
                            });
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            toastMsg(getString(R.string.clipboard_not_valid));
                        }

                    }
                    else{
                        toastMsg(getString(R.string.clipboard_not_valid));
                    }
                }
                else{
                    toastMsg(getString(R.string.clipboard_not_valid));
                }
            }
        }
        progressHide();
    }


    /**
     * Request user story by storyId
     */
    public void getStoryOfUserByStoryId(String user_id, final String story_id){
        OkHttpClient client = Util.getHttpClient();
        String url = Util.URL_HOST + Util.PATH_GET_STORY;
        url = String.format(url,user_id);
        final Request request = Util.getRequestHeaderBuilder(url, SESSION_ID,
                Util.USER_AGENT, Util.CONTENT_TYPE).build();

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
                        }
                        else if (json.has("reel")){
                            JSONArray items = json.getJSONObject("reel").getJSONArray("items");
                            JSONObject item;
                            boolean found = false;
                            for (int i = 0; i < items.length(); i++) {
                                item = items.getJSONObject(i);
                                if (item.getString("pk").equals(story_id)){
                                    found = true;
                                    ArrayList<MediaDownloadEntity> mediaDownloadEntities = new ArrayList<>();
                                    int media_type = item.getInt("media_type");
                                    String media_id = item.getString("id");
                                    if (media_type == 1){
                                        JSONArray imgCandidates = item.getJSONObject("image_versions2")
                                                .getJSONArray("candidates");
                                        JSONObject candidate;
                                        for (int j=0; j<imgCandidates.length(); j++){
                                            candidate = imgCandidates.getJSONObject(j);
                                            mediaDownloadEntities.add(
                                                    new MediaDownloadEntity(
                                                            candidate.getString("url"),
                                                            candidate.getString("height"),
                                                            candidate.getString("width"),
                                                            1, media_id));
                                        }
                                    }
                                    else if (media_type == 2){
                                        JSONObject videoVersion = item.getJSONArray("video_versions")
                                                .getJSONObject(0);
                                        mediaDownloadEntities.add(
                                                new MediaDownloadEntity(
                                                        videoVersion.getString("url"),
                                                        videoVersion.getString("height"),
                                                        videoVersion.getString("width"),
                                                        2,
                                                        media_id));
                                        if (item.has("image_versions2")){
                                            JSONArray imgCandidates = item.getJSONObject("image_versions2")
                                                    .getJSONArray("candidates");
                                            mediaDownloadEntities.add(new MediaDownloadEntity(
                                                    imgCandidates.getJSONObject(0)
                                                            .getString("url"),
                                                    imgCandidates.getJSONObject(0)
                                                            .getString("height"),
                                                    imgCandidates.getJSONObject(0)
                                                            .getString("width"),
                                                    1,
                                                    media_id));
                                        }
                                    }
                                    backgroundThreadDialog(mediaDownloadEntities, MainActivity.this);
                                    break;
                                }
                            }
                            if (!found){
                                backgroundThreadShortToast(getString(R.string.no_story));
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        backgroundThreadShortToast(getString(R.string.no_story));
                    }

                }
            }
        });
    }


    /**
     * Dialog menu
     * Used for available sizes for downloading media
     */
    public void backgroundThreadDialog(final ArrayList<MediaDownloadEntity> mediaDownloadEntities,
                                       final Activity context) {
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
                        Util.checkPermission(context);
                        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            downloadMedia(mediaDownloadEntities.get(rg.getCheckedRadioButtonId()),context);
                        }
                    }
                });
                Random random = new Random();
                boolean firstRadioChecked = false;
                int firstRadio = 0;
                int slideNumber = 1;
                for(int i=0; i<mediaDownloadEntities.size(); i++){
                    if (mediaDownloadEntities.get(i) == null){
                        View vvv = new View(context);
                        vvv.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                5
                        ));
                        String colorCode = String.format("#%06x", random.nextInt(0xffffff + 1));
                        vvv.setBackgroundColor(Color.parseColor(colorCode));
                        rg.addView(vvv);
                        RadioButton rb = new RadioButton(context);
                        rb.setButtonDrawable(R.drawable.ic_panorama_black_24dp);
                        rb.setButtonTintList(new ColorStateList(
                                new int[][] {
                                    new int[] { android.R.attr.state_enabled}, // enabled
                                    new int[] {-android.R.attr.state_enabled}, // disabled
                                    new int[] {-android.R.attr.state_checked}, // unchecked
                                    new int[] { android.R.attr.state_pressed}  // pressed
                                },
                                new int[] {
                                    Color.parseColor(colorCode),
                                    Color.parseColor(colorCode),
                                    Color.parseColor(colorCode),
                                    Color.parseColor(colorCode)
                                }
                        ));
                        String text_for_display = "# "+ slideNumber++;
                        rb.setText(text_for_display);
                        rb.setClickable(false);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.gravity = Gravity.CENTER;
                        rb.setLayoutParams(params);
                        rg.addView(rb);
                        continue;
                    }
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
                    if(!firstRadioChecked){
                        firstRadio = i;
                        firstRadioChecked = true;
                    }
                }
                rg.check(firstRadio);
                dialog.show();
            }
        });
    }


    /**
     * Download media and save it to gallery
     */
    public void downloadMedia(final MediaDownloadEntity media, final Activity context){
        progressShow();
        OkHttpClient client = new OkHttpClient.Builder().build();
        final Request request = new Request.Builder()
                .url(media.getUrl())
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
                        InputStream inputStream = responseBody.byteStream();
                        Uri uri = null;
                        if (media.getMediaType() == 1){
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            File saveDir = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES),
                                    context.getString(R.string.app_name));
                            if(!saveDir.exists()){
                                if(!saveDir.mkdirs()){
                                    Util.checkPermission(getParent());
                                    progressHide();
                                    return;
                                }
                            }
                            String fileName = "MD_" + media.getId() + "_" + media.getDimensions() +
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
                                        context.getApplicationContext()
                                                .getPackageName() + ".mfileprovider", file);

                                MediaScannerConnection.scanFile(context, new String[] {
                                        file.getAbsolutePath()},
                                        new String[] {"image/*"}, null);
                            }
                            catch (IOException e){
                                Util.checkPermission(getParent());
                                progressHide();
                                return;
                            }

                        }
                        else if (media.getMediaType() == 2){
                            File saveDir = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_MOVIES),
                                    context.getString(R.string.app_name));
                            if(!saveDir.exists()){
                                if(!saveDir.mkdirs()){
                                    Util.checkPermission(getParent());
                                    progressHide();
                                    return;
                                }
                            }
                            String fileName = "MD_" + media.getId() + "_" + media.getDimensions() +
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
                                        context.getApplicationContext()
                                                .getPackageName() + ".mfileprovider", file);

                                MediaScannerConnection.scanFile(context, new String[] {
                                        file.getAbsolutePath()},
                                        new String[] {"video/*"}, null);
                            }
                            catch (IOException e){
                                Util.checkPermission(getParent());
                                progressHide();
                                return;
                            }
                        }
                        progressHide();
                        NotificationManager nm = (NotificationManager) context
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                        if (uri != null && nm != null){
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            if (media.getMediaType()==1){
                                intent.setDataAndType(uri, "image/*");
                            }
                            else{
                                intent.setDataAndType(uri, "video/*");
                            }
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            PendingIntent contentIntent = PendingIntent.getActivity(
                                    context,
                                    0,
                                    intent,
                                    PendingIntent.FLAG_CANCEL_CURRENT);
                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                                    context ,
                                    Util.NOTIFICATION_CHANNEL_ID ) ;
                            mBuilder.setContentTitle(context.getString(R.string.app_name));
                            mBuilder.setContentIntent(contentIntent);
                            mBuilder.setAutoCancel(true);
                            String contentText;
                            if (media.getMediaType()==1){
                                mBuilder.setSmallIcon(R.drawable.ic_panorama_black_24dp);
                                contentText = String.format(
                                        getString(R.string.image_downloaded),
                                        media.getId(),
                                        media.getDimensions());
                            }
                            else{
                                mBuilder.setSmallIcon(R.drawable.ic_ondemand_video_black_24dp);
                                contentText = String.format(
                                        getString(R.string.video_downloaded),
                                        media.getId(),
                                        media.getDimensions());
                            }
                            mBuilder.setContentText( contentText );
                            mBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                            mBuilder.setVibrate(new long[] {100,100});
                            nm.notify((int)System.currentTimeMillis() , mBuilder.build()) ;
                        }

                    }

                }
            }
        });

    }


    /**
     * Handle result from LoginActivity
     */
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
                        disablePublicProfile();
                    }
                }
                if (data.getBooleanExtra("skipped",true)){
                    enablePublicProfile();
                }
            }
        }
    }


    /**
     * Get followers and list to UI
     */
    public void getFollows(String id, int followType){
        progressShow();
        OkHttpClient client = Util.getHttpClient();
        String url;
        if (followType == 0){
            url = Util.URL_HOST + Util.PATH_USER_SEARCH + "?q=" + id;
        }
        else if(followType == 1){
            url = Util.URL_HOST + Util.PATH_GET_FOLLOWERS;
            url = String.format(url, id);
        }
        else{
            url = Util.URL_HOST + Util.PATH_GET_FOLLOWINGS;
            url = String.format(url,id);
        }

        final ArrayList <UserIG> userIGArrayList = new ArrayList<>();
        Request request = Util.getRequestHeaderBuilder(url, SESSION_ID,
                Util.USER_AGENT,Util.CONTENT_TYPE).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                backgroundThreadShortToast(getString(R.string.net_err));
                e.printStackTrace();
                progressHide();
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
                        progressHide();
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
                                String ppUrl;
                                String fullName;
                                long latestReelMedia;
                                JSONObject user;
                                UserIG userIG;
                                for (int i = 0; i < users.length(); i++) {
                                    user = users.getJSONObject(i);
                                    userId = Long.toString(user.getLong("pk"));
                                    username = user.getString("username");
                                    ppUrl = user.getString("profile_pic_url");
                                    fullName = user.getString("full_name");
                                    latestReelMedia = user.getLong("latest_reel_media");
                                    userIG = new UserIG(userId,
                                            username,
                                            ppUrl,
                                            fullName,
                                            latestReelMedia);
                                    userIGArrayList.add(userIG);
                                }

                                CustomListView customListview = new CustomListView(MainActivity.this,
                                        userIGArrayList, false);
                                ListView lst = findViewById(R.id.listview_followers);
                                lst.setAdapter(customListview);
                                lst.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                        getStoryOfUser(userIGArrayList.get(i).getUserId());
                                    }
                                });


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            catch (Exception e) {
                                Log.e("ERROR",getString(R.string.gen_error));
                                e.printStackTrace();
                            }
                            progressHide();
                        }
                    });

                }
            }
        });
    }


    /**
     * Get story by userId
     */
    public void getStoryOfUser(String userId){
        progressShow();
        OkHttpClient client = Util.getHttpClient();
        String url = Util.URL_HOST + Util.PATH_GET_STORY;
        url = String.format(url,userId);
        final Request request = Util.getRequestHeaderBuilder(url, SESSION_ID,
                Util.USER_AGENT,Util.CONTENT_TYPE).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                backgroundThreadShortToast(getString(R.string.net_err));
                progressHide();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.isSuccessful()){
                    ResponseBody responseBody = response.body();
                    String responseGetStory;
                    if (responseBody != null){
                        responseGetStory = responseBody.string();
                    }
                    else{
                        toastMsg(getString(R.string.smth_wrong));
                        progressHide();
                        return;
                    }
                    JSONObject json;
                    JSONArray items;
                    String username = "";
                    ArrayList<StoryEntity> storyEntities = new ArrayList<>();
                    try {
                        json = new JSONObject(responseGetStory);
                        if (json.has("reel") && json.isNull("reel")) {
                            backgroundThreadShortToast(getString(R.string.no_story));
                            progressHide();
                            return;
                        }
                        items = json.getJSONObject("reel").getJSONArray("items");
                        username = json.getJSONObject("reel").getJSONObject("user")
                                .getString("username");

                        for (int i = 0; i < items.length(); i++) {
                            storyEntities.add(new StoryEntity(items.getJSONObject(i),username));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    final String usernameFinal = username;
                    final ArrayList<StoryEntity> storyEntitiesFinal = storyEntities;
                    progressHide();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(MainActivity.this, StoryViewer.class);
                            Bundle b = new Bundle();
                            b.putString("username", usernameFinal);
                            b.putParcelableArrayList("storyEntities",storyEntitiesFinal);
                            intent.putExtras(b);
                            startActivity(intent);
                        }
                    });
                }
            }
        });
    }


    /**
     * Get all stories available and list them
     */
    public void getStoryReels(){
        progressShow();
        final ArrayList <UserIG> userIGArrayList = new ArrayList<>();
        String url = Util.URL_HOST + Util.PATH_REELS_TRAY;
        OkHttpClient client = Util.getHttpClient();
        final Request request = Util.getRequestHeaderBuilder(url, SESSION_ID,
                Util.USER_AGENT,Util.CONTENT_TYPE)
                .post(new okhttp3.FormBody.Builder().build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                backgroundThreadShortToast(getString(R.string.net_err));
                progressHide();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if(response.isSuccessful()){
                    final ResponseBody responseBody = response.body();
                    if (responseBody != null){/*
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {*/
                                try {
                                    JSONObject responseJson = new JSONObject(responseBody.string());
                                    if (responseJson.has("status") &&
                                            responseJson.getString("status").equals("ok") &&
                                            responseJson.has("status")) {
                                        JSONArray tray = responseJson.getJSONArray("tray");
                                        String userId;
                                        String username;
                                        String ppUrl;
                                        String fullName;
                                        JSONObject user;
                                        UserIG userIG;
                                        for (int i = 0; i < tray.length(); i++) {
                                            user = tray.getJSONObject(i).getJSONObject("user");
                                            userId = Long.toString(user.getLong("pk"));
                                            username = user.getString("username");
                                            ppUrl = user.getString("profile_pic_url");
                                            fullName = user.getString("full_name");
                                            userIG = new UserIG(userId,
                                                    username,
                                                    ppUrl,
                                                    fullName,
                                                    0);
                                            userIGArrayList.add(userIG);
                                        }
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                CustomListView customListview = new CustomListView(
                                                        MainActivity.this,
                                                        userIGArrayList,
                                                        true);
                                                ListView lst = findViewById(R.id.listview_followers);
                                                lst.setAdapter(customListview);
                                                lst.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                                    @Override
                                                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                                        getStoryOfUser(userIGArrayList.get(i)
                                                                .getUserId());
                                                    }
                                                });
                                            }
                                        });


                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    backgroundThreadShortToast(getString(R.string.gen_error));
                                }
                                progressHide();
                          /*  }
                        });*/
                    }
                }
            }
        });
    }


    /**
     * Handle previous session or init new
     * ! Starts LoginActivity
     */
    public void getSharedPreferencesValues(){
        SharedPreferences sharedPreferences = this.getSharedPreferences(getApplicationContext()
                .getPackageName(), Context.MODE_PRIVATE);

        if (sharedPreferences.contains("logged_in") &&
                sharedPreferences.getInt("logged_in", 0) == 1){
            hasPrevSession = true;
            try{
                USER_ID = sharedPreferences.getString("user_id", null);
                SESSION_ID = sharedPreferences.getString("session_id", null);
                checkDecryptionSuccess = sharedPreferences.getString("check", null);
                checkDecryptionString = sharedPreferences.getString("checkEnc", null);
                initVector = sharedPreferences.getString("init_vector", null);
                disablePublicProfile();
            }
            catch (Exception e){
                enablePublicProfile();
                e.printStackTrace();
            }
        }
        else{
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivityForResult(intent,0);
        }
    }


    /**
     * Decryption of previous session
     * For successful decryption user should enter his keyphrase
     * Otherwise decryption is not available
     */
    public int decryptArguments(String keyPhrase){
        String decryptedTest = Util.decrypt(checkDecryptionString, keyPhrase, initVector);
        if (decryptedTest == null){
            toastMsg(getString(R.string.keyphrase_wrong));
            return -1;
        }
        byte[] decryptedTestInBytes = decryptedTest.getBytes(StandardCharsets.UTF_8);
        String decryptedTestMD5 = Util.toHexString(Util.md5hash(Util.md5hash(decryptedTestInBytes)));
        if (decryptedTestMD5.equals(checkDecryptionSuccess)){
            USER_ID = Util.decrypt(USER_ID, keyPhrase, initVector);
            SESSION_ID = Util.decrypt(SESSION_ID, keyPhrase, initVector);
            SESSION_ID = "sessionid=" + SESSION_ID;
            checkDecryptionString = decryptedTestMD5;
            toastMsg(getString(R.string.keyphrase_right));
            hasOpenSession = true;
            return 0;
        }
        return -1;
    }


    /**
     * Toast message. Code friendly.
     */
    public void toastMsg(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    /**
     * Toast message using handler
     */
    public void backgroundThreadShortToast(final String msg) {
        final Context context = getApplicationContext();
        if (context != null && msg != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    /**
     * if keyphrase entered and decryption was successful then logout invalidating session
     * otherwise just clear encrypted session data
     */
    public void logout(){
        String urlLogout = Util.URL_HOST + Util.PATH_LOGOUT;
        OkHttpClient client = Util.getHttpClient();
        final Request request = new Request.Builder()
                .url(urlLogout)
                .header("User-Agent", Util.USER_AGENT)
                .addHeader("Cookie", SESSION_ID)
                .addHeader("Content-Type", Util.CONTENT_TYPE)
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
        hasPrevSession = false;
    }


    /**
     * Progress start
     */
    public void progressShow(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressView.setVisibility(ViewAnimator.VISIBLE);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });
    }


    /**
     * Progress stop
     */
    public void progressHide(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressView.setVisibility(ViewAnimator.INVISIBLE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });
    }

}
