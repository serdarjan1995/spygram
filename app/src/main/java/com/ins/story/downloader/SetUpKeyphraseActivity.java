package com.ins.story.downloader;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
                    toastMsg(getString(R.string.keyphrase_empty));
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
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.exit_confirm_msg))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        logout();
                        finishAffinity();
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();

    }

    public void logout(){
        String urlLogout = Util.URL_HOST + Util.PATH_LOGOUT;
        OkHttpClient client = Util.getHttpClient();
        final Request request = Util.getRequestHeaderBuilder(urlLogout, sessionid,
                                    Util.USER_AGENT,Util.CONTENT_TYPE)
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
                                backgroundThreadShortToast(getString(R.string.clear_session_logout));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
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

    public void toastMsg(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
