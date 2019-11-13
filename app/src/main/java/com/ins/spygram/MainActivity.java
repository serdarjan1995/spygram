package com.ins.spygram;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
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
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import android.view.Menu;
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
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private ArrayList<UserFollower> followers;
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
        getSharedPreferencesValues();
    }

    private void loadBanner() {
        AdRequest adRequest =
                new AdRequest.Builder().addTestDevice("6F4C8BD9AE078F1B48B1D1F439EF5039")
                        .build();

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
        //getMenuInflater().inflate(R.menu.main, menu);
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
        switch (id){
            case R.id.nav_key:
                fragmentManager.beginTransaction().replace(R.id.content_frame, keyFragment).commit();
                refreshAd();
                loadBanner();
                break;
            case R.id.nav_followers:
                loadBanner();
                if( checkDecryptionString.equals(checkDecryptionSuccess)){
                    fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment).commit();
                    followers = getFollows(USER_ID,1);
                }
                else{
                    Toast.makeText(this,"Please check your keyphrase. It is incorrect",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.nav_following:
                loadBanner();
                if( checkDecryptionString.equals(checkDecryptionSuccess)){
                    fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment).commit();
                    followers = getFollows(USER_ID,2);
                }
                else{
                    Toast.makeText(this,"Please check your keyphrase. It is incorrect",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.nav_clear_session:
                new AlertDialog.Builder(this)
                        .setMessage("Login credentials will be removed. Are you sure?")
                        .setCancelable(true)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if( checkDecryptionString.equals(checkDecryptionSuccess)){
                                    logout();
                                }
                                SharedPreferences sharedPreferences = getSharedPreferences(getApplicationContext()
                                        .getPackageName(), Context.MODE_PRIVATE);
                                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                                sharedPreferencesEditor.clear();
                                sharedPreferencesEditor.apply();
                                Toast.makeText(getApplicationContext(), "Your session invalidated",
                                        Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                startActivityForResult(intent,0);
                            }
                        })
                        .setNegativeButton("No", null)
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
            fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment)
                    .commitAllowingStateLoss();
            followers = getFollows(USER_ID,2);
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
                        Toast.makeText(this, "All done", Toast.LENGTH_SHORT).show();
                        fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment)
                                .commitAllowingStateLoss();
                        followers = getFollows(USER_ID, 2);
                    }
                }
            }
        }
    }

    public ArrayList <UserFollower> getFollows(String id, int followType){
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

        final ArrayList <UserFollower> userFollowerArrayList = new ArrayList<>();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", getString(R.string.user_agent))
                .addHeader("Cookie", SESSION_ID)
                .addHeader("Content-Type", getString(R.string.content_type))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
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
                        Toast.makeText(getApplicationContext(),"Received Empty body from server",
                                Toast.LENGTH_SHORT).show();
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
                                JSONObject user;
                                UserFollower userFollower;
                                for (int i = 0; i < users.length(); i++) {
                                    user = users.getJSONObject(i);
                                    userId = Long.toString(user.getLong("pk"));
                                    username = user.getString("username");
                                    pp_url = user.getString("profile_pic_url");
                                    full_name = user.getString("full_name");
                                    userFollower = new UserFollower(userId,username,pp_url,full_name);
                                    userFollowerArrayList.add(userFollower);
                                }

                                CustomListview customListview = new CustomListview(MainActivity.this,followers);
                                ListView lst = findViewById(R.id.listview_followers);
                                lst.setAdapter(customListview);
                                lst.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                        getStoryOfUser(followers.get(i).getUserId());
                                    }
                                });


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            catch (Exception e) {
                                System.out.println("General Error occured: ");
                                e.printStackTrace();
                            }
                        }
                    });


                }
            }
        });

        return userFollowerArrayList;
    }


    public void getStoryOfUser(String userId){
        OkHttpClient client = Util.getHttpClient();
        String url = getString(R.string.url_host) + getString(R.string.path_get_story);
        url = String.format(url,userId);
        final Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", getString(R.string.user_agent))
                .addHeader("Cookie", SESSION_ID)
                .addHeader("Content-Type", getString(R.string.content_type))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
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
                        Toast.makeText(getApplicationContext(),"Received Empty body from server",
                                Toast.LENGTH_SHORT).show();
                        return;
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
            Toast.makeText(this,"KeyPhrase not correct",Toast.LENGTH_SHORT).show();
            return -1;
        }
        byte[] decryptedTestInBytes = decryptedTest.getBytes(StandardCharsets.UTF_8);
        String decryptedTestMD5 = Util.toHexString(Util.md5hash(Util.md5hash(decryptedTestInBytes)));
        if (decryptedTestMD5.equals(checkDecryptionSuccess)){
            USER_ID = Util.decrypt(USER_ID,keyPhrase,initVector);
            SESSION_ID = Util.decrypt(SESSION_ID,keyPhrase,initVector);
            SESSION_ID = "sessionid=" + SESSION_ID;
            checkDecryptionString = decryptedTestMD5;
            Toast.makeText(this,"KeyPhrase correct",Toast.LENGTH_SHORT).show();
            return 0;
        }
        return -1;
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
