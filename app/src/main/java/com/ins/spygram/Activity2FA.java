package com.ins.spygram;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class Activity2FA extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_2fa);
        Bundle b = getIntent().getExtras();
        if (b != null) {
            String phone_number = b.getString("phone_number");
            if (!phone_number.equals("")) {
                TextView phoneNumberTextView = findViewById(R.id.textView2fa_phonenumber);
                phoneNumberTextView.setText(String.format(getString(R.string.phone_number), phone_number));
            }
            final String identifier = b.getString("identifier");
            final String username = b.getString("username");
            if (!identifier.equals("")) {
                final EditText code_2faEditText = findViewById(R.id.editText_2fa);
                Button send2FaButton = findViewById(R.id.button2FaSendCode);
                send2FaButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String code2fa = code_2faEditText.getText().toString();
                        if (code2fa.equals("")){
                            toastMsg(getString(R.string.enter_ver_code));
                        }
                        else{
                            String urlLogin2Fa = getString(R.string.url_host) + getString(R.string.path_login_2fa);
                            OkHttpClient client = Util.getHttpClient();
                            JSONObject json = new JSONObject();
                            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                            try {
                                json.put("username", username);
                                json.put("two_factor_identifier",identifier);
                                json.put("device_id",androidId);
                                json.put("verification_code",code2fa);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            RequestBody requestBody = Util.getRequestBody(json);
                            final Request request = Util.getRequestHeaderBuilder(urlLogin2Fa, "",
                                    getString(R.string.user_agent),
                                    getString(R.string.content_type))
                                    .post(requestBody)
                                    .build();

                            client.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                    e.printStackTrace();
                                    backgroundThreadShortToast(getString(R.string.net_err));
                                }

                                @Override
                                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                    if(response.isSuccessful()) {
                                        JSONObject r_response = Util.getSuccessfulLoginParams(response);
                                        try {
                                            if(r_response.getBoolean("is_success")){
                                                Intent resultIntent = new Intent();
                                                resultIntent.putExtra("sessionid", r_response.getString("sessionid"));
                                                resultIntent.putExtra("userid", r_response.getString("userid"));
                                                setResult(Activity.RESULT_OK, resultIntent);
                                                finish();
                                            }
                                            else{
                                                backgroundThreadShortToast(getString(R.string.smth_wrong)
                                                        + " Error code: 113");
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
                                                        responseJson.getString("status").equals("fail")){
                                                    backgroundThreadShortToast(responseJson.getString("message"));
                                                }
                                                else{
                                                    backgroundThreadShortToast(getString(R.string.smth_wrong)
                                                            + " Error code: 401");
                                                }
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                }
                            });
                        }
                    }
                });
            }
            else{
                toastMsg(getString(R.string.smth_wrong) + " Error code: 114");
            }

        }


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
}
