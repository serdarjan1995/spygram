package com.ins.spygram;

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


    /*
        AES encrypt your @session_id, @user_id, @session_agent then place them in base64encoded form to related variables
        Place your init vector in bytes array to @initVector
        Randomly generate some string, get its md5hash twice and place to @checkDecryptionSuccess


     */


    private ArrayList<UserFollower> followers;
    private String USER_ID;
    private String SESSION_ID;
    private FragmentManager fragmentManager;
    private ViewFragment keyFragment;
    private ViewFragment followersFragment;
    private String keyPhrase;
    private String checkDecryptionString;
    private String checkDecryptionSuccess;
    private String checkDecryptionResult;
    private static byte[] initVector;

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
        SharedPreferences sharedPreferences = this.getSharedPreferences(getApplicationContext()
                        .getPackageName(), Context.MODE_PRIVATE);

        if (sharedPreferences.contains("logged_in") &&
                sharedPreferences.getInt("logged_in", 0) == 1){
            try{
                USER_ID = sharedPreferences.getString("user_id", null);
                SESSION_ID = sharedPreferences.getString("session_id", null);
                //decryptArguments();
            }
            catch (Exception e){
                e.printStackTrace();
            }

        }
        else{
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
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
            if( checkDecryptionResult.equals(checkDecryptionSuccess)){
                fragmentManager.beginTransaction().replace(R.id.content_frame, followersFragment).commit();
                followers = getFollows(USER_ID,1);
            }
            else{
                Toast.makeText(this,"Please check your keyphrase. It is incorrect",
                        Toast.LENGTH_SHORT).show();
            }
        }
        else if (id == R.id.nav_following) {
            if( checkDecryptionResult.equals(checkDecryptionSuccess)){
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
        this.keyPhrase = text.getText().toString();
        decryptArguments();
    }

    public ArrayList <UserFollower> getFollows(String id, int followType){
        OkHttpClient client = new OkHttpClient();
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
        OkHttpClient client = new OkHttpClient();
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

    public static byte[] md5hash(byte[] bArr) {
        MessageDigest instance;
        byte[] digest = null;
        try {
            instance = MessageDigest.getInstance("MD5");
            instance.update(bArr);
            digest = instance.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return digest;
    }

    public void decryptArguments(){
        Cipher instance;
        byte[] keyphraseinbytes;
        try {
            keyphraseinbytes = keyPhrase.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(md5hash(keyphraseinbytes), "AES");
            instance = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            instance.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(initVector));
            byte[] checksuccess = instance.doFinal(Base64.decode(checkDecryptionString, Base64.DEFAULT));
            byte[] resultcheck = md5hash(md5hash(checksuccess));
            String testcheckDecryptionResult = toHexString(resultcheck);
            if( testcheckDecryptionResult.equals(checkDecryptionSuccess)){
                USER_ID = new String(instance.doFinal(Base64.decode(USER_ID, Base64.DEFAULT)));
                SESSION_ID = new String(instance.doFinal(Base64.decode(SESSION_ID, Base64.DEFAULT)));
                checkDecryptionResult = testcheckDecryptionResult;
                Toast.makeText(this,"KeyPhrase correct",Toast.LENGTH_SHORT).show();
            }
        } catch (BadPaddingException e) {
            e.printStackTrace();
            Toast.makeText(this,"KeyPhrase not correct",Toast.LENGTH_SHORT).show();
            checkDecryptionResult = "";
            USER_ID = "";
            SESSION_ID = "";
        } catch (Exception e){
            System.out.println("Error occured: ");
            e.printStackTrace();
        }

    }


    public static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte bByte : bytes) {
            String hex = Integer.toHexString(0xFF & bByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


}
