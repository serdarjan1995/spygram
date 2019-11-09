package com.ins.spygram;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SetUpKeyphraseActivity extends AppCompatActivity {
    private EditText keyphraseEditText;
    private String userid;
    private String sessionid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.encryption_layout);
        Bundle b = getIntent().getExtras();
        if (b != null){
            userid = b.getString("userid");
            sessionid = b.getString("sessionid");
        }
        keyphraseEditText = findViewById(R.id.encryption_credential_text);
        Button keyphraseButton = findViewById(R.id.buttonsetkeyphraseEnc);
        keyphraseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String keyphrase = keyphraseEditText.getText().toString();
                if (keyphrase.equals("")){
                    Toast.makeText(getApplicationContext(), "Please enter your passphrase",
                            Toast.LENGTH_SHORT).show();
                }
                else{
                    Random random = new Random();
                    byte[] initvector = new byte[16];
                    random.nextBytes(initvector);
                    String encryptedUserid = Util.encrypt("" + userid, keyphrase, initvector);
                    String encryptedSessionid = Util.encrypt("" + sessionid, keyphrase, initvector);
                    String initvectorEncoded = Base64.encodeToString(initvector,Base64.NO_WRAP);
                    byte[] randomByte = new byte[64];
                    new Random().nextBytes(randomByte);
                    String generatedString = Util.toHexString(randomByte);
                    randomByte = generatedString.getBytes(StandardCharsets.UTF_8);
                    byte[] randomByteMD5 = Util.md5hash(Util.md5hash(randomByte));
                    String randomByteMD5String = Util.toHexString(randomByteMD5);
                    String generatedStringEncrypted = Util.encrypt(generatedString, keyphrase, initvector);

                    SharedPreferences sharedPreferences = getSharedPreferences(getApplicationContext()
                            .getPackageName(), Context.MODE_PRIVATE);
                    SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                    sharedPreferencesEditor.putInt("logged_in",1);
                    sharedPreferencesEditor.putString("user_id", encryptedUserid);
                    sharedPreferencesEditor.putString("session_id", encryptedSessionid);
                    sharedPreferencesEditor.putString("init_vector", initvectorEncoded);
                    sharedPreferencesEditor.putString("check", randomByteMD5String);
                    sharedPreferencesEditor.putString("checkEnc", generatedStringEncrypted);
                    sharedPreferencesEditor.apply();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("keyphrase", keyphrase);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }
            }
        });
    }

    public void onBackPressed() {
        String urlLogout = getString(R.string.url_host) + getString(R.string.path_logout);
        OkHttpClient client = Util.getHttpClient();
        final Request request = new Request.Builder()
                .url(urlLogout)
                .header("User-Agent", getString(R.string.user_agent))
                .addHeader("Cookie", sessionid)
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
                                Toast.makeText(getApplicationContext(), "Your session invalidated",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        finishAffinity();
    }

}
