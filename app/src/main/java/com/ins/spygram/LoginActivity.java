package com.ins.spygram;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
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
    private boolean isSuccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);
        Button loginButton = findViewById(R.id.login_button);
        usernameEditText = findViewById(R.id.login_username);
        passwordEditText = findViewById(R.id.login_password);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = usernameEditText.getText().toString();
                password = passwordEditText.getText().toString();
                if (username.matches("") || password.matches("")) {
                    Toast.makeText(getApplicationContext(), "You did not enter mandatory fields", Toast.LENGTH_SHORT).show();
                }
                else{
                    JSONObject json = new JSONObject();
                    String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
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
    }



    private void loginTry(JSONObject json){
        String urlLogin = getString(R.string.url_host) + getString(R.string.path_login);
        OkHttpClient client = Util.getHttpClient();
        RequestBody requestBody = new okhttp3.FormBody.Builder()
                .add("signed_body", "." + json.toString())
                .add("ig_sig_key_version","4")
                .build();
        final Request request = new Request.Builder()
                .url(urlLogin)
                .header("User-Agent", getString(R.string.user_agent))
                .addHeader("Content-Type", getString(R.string.content_type))
                .post(requestBody)
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
                            if (responseJson.has("logged_in_user")){
                                JSONObject jsonLoggedInUser = responseJson.getJSONObject("logged_in_user");
                                String sessionid = "";
                                if (!response.headers("Set-Cookie").isEmpty()) {

                                    for (String cookies : response.headers("Set-Cookie")) {
                                        if (cookies.contains("sessionid=")){
                                            Pattern pattern = Pattern.compile("sessionid=(.*?);");
                                            Matcher matcher = pattern.matcher(cookies);
                                            if (matcher.find()){
                                                sessionid = matcher.group().replace("sessionid=","");
                                                sessionid = sessionid.replace(";","");
                                                isSuccess = true;
                                                backgroundThreadShortToast("Login success!");
                                            }

                                        }

                                    }

                                }
                                if (isSuccess){
                                    Intent intent = new Intent(LoginActivity.this, SetUpKeyphraseActivity.class);
                                    Bundle b = new Bundle();
                                    b.putString("sessionid", sessionid);
                                    b.putString("userid", jsonLoggedInUser.getString("pk"));
                                    intent.putExtras(b);
                                    startActivityForResult(intent, 1);
                                }

                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        backgroundThreadShortToast("Received Empty body from server");
                    }
                }
                else if (response.code() == 400 && response.body() != null){
                    JSONObject responseJson = null;
                    try {
                        responseJson = new JSONObject(response.body().string());
                        if (responseJson.has("status") && responseJson.get("status").equals("fail") ){
                            backgroundThreadShortToast(responseJson.get("message").toString());
                        }
                        else{
                            backgroundThreadShortToast("Something went wrong, status code 400");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
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

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                Intent resultIntent = new Intent();
                resultIntent.putExtra("keyphrase", data.getStringExtra("keyphrase"));
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        }
    }

    public void onBackPressed() {
        finishAffinity();
    }
}
