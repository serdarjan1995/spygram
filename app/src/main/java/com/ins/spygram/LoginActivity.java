package com.ins.spygram;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
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
                Toast.makeText(getApplicationContext(), "Clicked", Toast.LENGTH_SHORT).show();
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
        System.out.println(urlLogin);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new okhttp3.MultipartBody.Builder()
                .addFormDataPart("signed_body", "." + json.toString())
                .addFormDataPart("ig_sig_key_version","4")
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
                                String cookies = response.header("Set-Cookie");
                                System.out.println(cookies);
                                SharedPreferences sharedPreferences = getSharedPreferences(getApplicationContext()
                                        .getPackageName(), Context.MODE_PRIVATE);
                                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                                sharedPreferencesEditor.putInt("logged_in",1);
                                sharedPreferencesEditor.putInt("user_id", jsonLoggedInUser.getInt("pk"));
                                sharedPreferencesEditor.putString("session_id", cookies);
                                sharedPreferencesEditor.apply();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"Received Empty body from server",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

    }
}
