package com.ins.story.downloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import androidx.appcompat.app.AppCompatActivity;

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

public class ChallengeActivity extends AppCompatActivity {
    private String challenge_path;
    private String mid;
    private String androidId;
    private TextView phoneNumberTextView;
    private EditText codeChallengeEditText;
    private Button sendChallengeButton;
    private Handler handler;
    private ViewAnimator progressview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_challenge);
        handler = new Handler(ChallengeActivity.this.getMainLooper());
        progressview = findViewById(R.id.progress_view_challenge);
        progressview.setVisibility(ViewAnimator.INVISIBLE);
        codeChallengeEditText = findViewById(R.id.editText_challenge);
        sendChallengeButton = findViewById(R.id.buttonChallengeCode);
        sendChallengeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String codeChallenge = codeChallengeEditText.getText().toString();
                if (codeChallenge.equals("")){
                    toastMsg(getString(R.string.enter_ver_code));
                }
                else {
                    sendCodeChallenge(codeChallenge);
                }
            }
        });
        Bundle b = getIntent().getExtras();
        if (b != null) {
            String phone_number = b.getString("phone_number");
            if (!phone_number.equals("")) {
                phoneNumberTextView = findViewById(R.id.textViewchallenge_phonenumber);
                phoneNumberTextView.setText(phone_number);
            }
            androidId = b.getString("androidId");
            challenge_path = b.getString("challenge_api_path");
            mid = b.getString("mid");
            String step_name = b.getString("step_name");
            if (step_name.equals("select_verify_method")){
                JSONObject json = new JSONObject();
                try {
                    json.put("choice","0");
                    json.put("device_id",androidId);
                    getCodeChallenge(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else if (step_name.equals("verify_code")) {
                toastMsg(getString(R.string.enter_ver_code));
            }
            else{
                toastMsg(getString(R.string.smth_wrong) + " Error code: 214");
            }
        }
    }

    public void getCodeChallenge(JSONObject json){
        progressShow();
        String urlChallenge = Util.URL_HOST + challenge_path;
        OkHttpClient client;
        RequestBody requestBody;
        try {
            client = Util.getHttpClient();
            requestBody = Util.getRequestBody(json);
        }
        catch (Exception e){
            backgroundThreadShortToast(getString(R.string.net_err));
            return;
        }
        final Request request = Util.getRequestHeaderBuilder(urlChallenge, "mid=" + mid,
                Util.USER_AGENT,Util.CONTENT_TYPE)
                .post(requestBody)
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
                                r_response.has("step_data") && r_response.has("step_name") &&
                                r_response.getString("step_name").equals("verify_code")){
                            //Done
                        }
                        else{
                            backgroundThreadShortToast(getString(R.string.fail_login) + " 2");
                        }
                    }
                    catch(JSONException e){
                        e.printStackTrace();
                        backgroundThreadShortToast(getString(R.string.fail_login) + " 3");
                    }
                }
                else{
                    backgroundThreadShortToast(getString(R.string.fail_login) + " 4");
                }
                progressHide();
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                backgroundThreadShortToast(getString(R.string.net_err));
                progressHide();
            }
        });
    }


    public void sendCodeChallenge(String codeChallenge){
        progressShow();
        JSONObject json = new JSONObject();
        try {
            json.put("security_code",codeChallenge);
            json.put("device_id",androidId);
        } catch (JSONException e) {
            e.printStackTrace();
            backgroundThreadShortToast(getString(R.string.gen_error));
            progressHide();
            return;
        }
        String urlChallenge = Util.URL_HOST + challenge_path;
        OkHttpClient client;
        RequestBody requestBody;
        try {
            client = Util.getHttpClient();
            requestBody = Util.getRequestBody(json);
        }
        catch (Exception e){
            backgroundThreadShortToast(getString(R.string.net_err));
            progressHide();
            return;
        }
        final Request request = Util.getRequestHeaderBuilder(urlChallenge, "mid=" + mid,
                Util.USER_AGENT,Util.CONTENT_TYPE)
                .post(requestBody)
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
                                r_response.has("action") &&
                                r_response.getString("action").equals("close")){
                            Intent resultIntent = new Intent();
                            setResult(Activity.RESULT_OK, resultIntent);
                            progressHide();
                            finish();
                        }
                        else{
                            backgroundThreadShortToast(getString(R.string.fail_login) + " 5");
                        }
                    }
                    catch(JSONException e){
                        e.printStackTrace();
                        backgroundThreadShortToast(getString(R.string.fail_login) + " 6");
                    }
                }
                else{
                    backgroundThreadShortToast(getString(R.string.fail_login) + " 7");
                }
                progressHide();
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
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

    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.exit_confirm_msg))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finishAffinity();
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();

    }

    public void toastMsg(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void progressHide(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                sendChallengeButton.setEnabled(true);
                progressview.setVisibility(ViewAnimator.INVISIBLE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });
    }

    public void progressShow(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                sendChallengeButton.setEnabled(false);
                progressview.setVisibility(ViewAnimator.VISIBLE);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });
    }
}
