package com.ins.spygram;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import android.view.Menu;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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

        keyFragment = new ViewFragment(R.layout.content_key);
        followersFragment = new ViewFragment(R.layout.content_followers);
        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, keyFragment).commit();
        getSharedPreferencesValues();
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

        if (id == R.id.nav_key) {
            fragmentManager.beginTransaction().replace(R.id.content_frame, keyFragment).commit();
        } else if (id == R.id.nav_followers) {
            if( checkDecryptionString.equals(checkDecryptionSuccess)){
                fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment).commit();
                followers = getFollows(USER_ID,1);
            }
            else{
                Toast.makeText(this,"Please check your keyphrase. It is incorrect",
                        Toast.LENGTH_SHORT).show();
            }
        }
        else if (id == R.id.nav_following) {
            if( checkDecryptionString.equals(checkDecryptionSuccess)){
                fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment).commit();
                followers = getFollows(USER_ID,2);
            }
            else{
                Toast.makeText(this,"Please check your keyphrase. It is incorrect",
                        Toast.LENGTH_SHORT).show();
            }
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
        if (requestCode == 0) {
            if(resultCode == Activity.RESULT_OK){
                String keyphrase = data.getStringExtra("keyphrase");
                if (keyphrase != null){
                    getSharedPreferencesValues();
                    int status = decryptArguments(keyphrase);
                    if (status == 0){
                        Toast.makeText(this,"All done",Toast.LENGTH_SHORT).show();
                        fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment)
                                .commitAllowingStateLoss();
                        followers = getFollows(USER_ID,2);
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
                                System.out.println("Response finished, list size: "+userFollowerArrayList.size());

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
        else{

        }
        return -1;
    }

}
