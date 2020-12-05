package com.inview.instagram.story.downloader;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewAnimator;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private String username;
    private String password;
    private String androidId;
    private String challengeApiPath;
    private Button loginButton;
    private ViewAnimator progressView;
    private Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);
        loginButton = findViewById(R.id.login_button);
        Button skipLoginButton = findViewById(R.id.login_skip_button);
        usernameEditText = findViewById(R.id.login_username);
        passwordEditText = findViewById(R.id.login_password);
        progressView = findViewById(R.id.progress_view_login);
        progressView.setVisibility(ViewAnimator.INVISIBLE);
        handler = new Handler(LoginActivity.this.getMainLooper());
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = usernameEditText.getText().toString();
                password = passwordEditText.getText().toString();
                if (username.matches("") || password.matches("")) {
                    toastMsg(getString(R.string.fields_empty));
                }
                else{
                    JSONObject json = new JSONObject();
                    androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                    try {
                        json.put("username", username);
                        json.put("password",password);
                        json.put("device_id",androidId);
                        loginTry(json);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        skipLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("skipped", true);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }


    private void loginTry(JSONObject json){
        String urlLogin = Util.URL_HOST + Util.PATH_LOGIN;
        OkHttpClient client;
        RequestBody requestBody;
        try {
            client = Util.getHttpClient();
            requestBody = Util.getRequestBody(json);
            progressShow();
        }
        catch (Exception e){
            backgroundThreadShortToast(getString(R.string.net_err));
            return;
        }
        final Request request = Util.getRequestHeaderBuilder(urlLogin, "",
                Util.USER_AGENT, Util.CONTENT_TYPE)
                .post(requestBody)
                .build();

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
                    JSONObject r_response = Util.getSuccessfulLoginParams(response);
                    try {
                        if(r_response.getBoolean("is_success")){
                            Intent intent = new Intent(LoginActivity.this, SetUpKeyphraseActivity.class);
                            Bundle b = new Bundle();
                            b.putString("sessionid", r_response.getString("sessionid"));
                            b.putString("userid", r_response.getString("userid"));
                            intent.putExtras(b);
                            startActivityForResult(intent, 1);
                        }
                        else{
                            backgroundThreadShortToast(getString(R.string.smth_wrong) + " Error code: 113");
                        }
                    }
                    catch(JSONException e){
                        e.printStackTrace();
                    }
                }
                else if (response.code() == 400 && response.body() != null){
                    ResponseBody responseBody = response.body();
                    try {
                        if (responseBody != null){
                            JSONObject responseJson = new JSONObject(responseBody.string());
                            if (responseJson.has("status") &&
                                    responseJson.getString("status").equals("fail") &&
                                    responseJson.has("two_factor_required") &&
                                    responseJson.getBoolean("two_factor_required")){
                                JSONObject two_factor_info = responseJson.getJSONObject("two_factor_info");
                                if (two_factor_info.getBoolean("sms_two_factor_on")){
                                    Intent intent = new Intent(LoginActivity.this, Activity2FA.class);
                                    Bundle b = new Bundle();
                                    b.putString("phone_number", two_factor_info.getString("obfuscated_phone_number"));
                                    b.putString("identifier", two_factor_info.getString("two_factor_identifier"));
                                    b.putString("username", two_factor_info.getString("username"));
                                    intent.putExtras(b);
                                    startActivityForResult(intent, 2);
                                }
                                else if (two_factor_info.getBoolean("totp_two_factor_on")){
                                    backgroundThreadShortToast(getString(R.string.totp_not_supported));
                                }

                            }
                            else if (responseJson.has("status") && responseJson.get("status").equals("fail") &&
                                    responseJson.has("message") &&
                                    responseJson.get("message").equals("challenge_required") ){
                                //TODO : Instagram Challenge Code - need to test
                                challengeApiPath = responseJson.getJSONObject("challenge").getString("api_path");
                                String mid = "";
                                if (!response.headers("Set-Cookie").isEmpty()) {
                                    for (String cookies : response.headers("Set-Cookie")) {
                                        if (cookies.contains("mid=")) {
                                            Pattern pattern = Pattern.compile("mid=(.*?);");
                                            Matcher matcher = pattern.matcher(cookies);
                                            if (matcher.find()) {
                                                mid = matcher.group().replace("mid=", "");
                                                mid = mid.replace(";", "");
                                            }

                                        }

                                    }
                                }
                                requestChallenge(challengeApiPath, mid);
                            }
                            else if (responseJson.has("status") && responseJson.get("status").equals("fail") ){
                                backgroundThreadShortToast(responseJson.getString("message"));
                            }
                            else{
                                backgroundThreadShortToast(getString(R.string.smth_wrong) + " status code 400");
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                progressHide();
            }
        });
    }


    private void requestChallenge(String url, final String mid){
        String urlLogin = Util.URL_HOST + url + "?device_id=" + androidId;
        OkHttpClient client;
        try {
            client = Util.getHttpClient();
            progressShow();
        }
        catch (Exception e){
            backgroundThreadShortToast(getString(R.string.net_err));
            return;
        }
        final Request request = Util.getRequestHeaderBuilder(urlLogin, "mid=" + mid,
                Util.USER_AGENT,"")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.isSuccessful()){
                    ResponseBody responseBody = response.body();
                    try {
                        JSONObject r_response = new JSONObject(responseBody.string());
                        if (r_response.has("status") &&
                                r_response.getString("status").equals("ok") &&
                                r_response.has("step_data")){
                            JSONObject step_data = r_response.getJSONObject("step_data");
                            Intent intent = new Intent(LoginActivity.this, ChallengeActivity.class);
                            Bundle b = new Bundle();
                            b.putString("phone_number", step_data.getString("phone_number"));
                            b.putString("androidId", androidId);
                            b.putString("mid", mid);
                            b.putString("challenge_api_path", challengeApiPath);
                            b.putString("step_name", r_response.getString("step_name"));
                            intent.putExtras(b);
                            startActivityForResult(intent, 3);
                        }
                        else{
                            backgroundThreadShortToast(getString(R.string.fail_login) + " 1");
                        }
                    }
                    catch(JSONException e){
                        e.printStackTrace();
                        backgroundThreadShortToast(getString(R.string.fail_login) + " 2");
                    }
                }
                else{
                    backgroundThreadShortToast(getString(R.string.fail_login) + " 3");
                }
                progressHide();
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                backgroundThreadShortToast(getString(R.string.net_err));
                progressHide();
            }
        });
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


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) { // success setUpKeyphrase
            if (resultCode == Activity.RESULT_OK) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("keyphrase", data.getStringExtra("keyphrase"));
                resultIntent.putExtra("skipped", false);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        } else if (requestCode == 2) {  // 2FA
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(LoginActivity.this, SetUpKeyphraseActivity.class);
                Bundle b = new Bundle();
                b.putString("sessionid", data.getStringExtra("sessionid"));
                b.putString("userid", data.getStringExtra("userid"));
                intent.putExtras(b);
                startActivityForResult(intent, 1);
            }
        }
        else if (requestCode == 3) { //challenge code
            if (resultCode == Activity.RESULT_OK) {
                JSONObject json = new JSONObject();
                try {
                    json.put("username", username);
                    json.put("password",password);
                    json.put("device_id",androidId);
                    loginTry(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void progressShow(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                loginButton.setEnabled(false);
                progressView.setVisibility(ViewAnimator.VISIBLE);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });
    }


    public void progressHide(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                loginButton.setEnabled(true);
                progressView.setVisibility(ViewAnimator.INVISIBLE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });
    }


    public void onBackPressed() {
        finishAffinity();
    }


    public void toastMsg(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
