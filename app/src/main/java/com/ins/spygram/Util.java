package com.ins.spygram;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Base64;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Util {

    static String NOTIFICATION_CHANNEL_ID = "10001";


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

    public static String encrypt(String input, String keyphrase, byte[] initVector){
        Cipher instance;
        byte[] keyphraseinbytes;
        try {
            keyphraseinbytes = md5hash(keyphrase.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyphraseinbytes, "AES");
            instance = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            instance.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(initVector));
            byte[] encrypted = instance.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encrypted,Base64.NO_WRAP);

        } catch (BadPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e){
            System.out.println("Error occured: ");
            e.printStackTrace();
            return null;
        }
    }

    public static String decrypt(String encrypted, String keyphrase, String initVector){
        Cipher instance;
        byte[] keyphraseinbytes;
        byte[] initvectorInBytes = Base64.decode(initVector,Base64.NO_WRAP);
        try {
            keyphraseinbytes = md5hash(keyphrase.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyphraseinbytes, "AES");
            instance = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            instance.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(initvectorInBytes));
            byte[] decrypted = instance.doFinal(Base64.decode(encrypted, Base64.NO_WRAP));
            return new String(decrypted);
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e){
            System.out.println("Error occured: ");
            e.printStackTrace();
            return null;
        }
    }

    public static OkHttpClient getHttpClient(){
        String hostname = "i.instagram.com";
        CertificatePinner certificatePinner = new CertificatePinner.Builder()
                .add(hostname, "sha256/mreKTxeq4bRmIPe8oiojs3P40B5t0z49e9E7lA7besM=")
                .add(hostname, "sha256/k2v657xBsOVe1PQRwOsHsw3bsGT2VzIqz5K+59sNQws=")
                .add(hostname, "sha256/WoiWRyIOVNa9ihaBciRSC7XHjliYS9VwUGOIud4PB18=")
                .add(hostname, "sha256/Q/ZoPwaZN6kZ0HU9LLQKBl+xx+wUuxP7jegEdu9T8WI=")
                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .certificatePinner(certificatePinner)
                .build();

        return client;
    }

    public static Request.Builder getRequestHeaderBuilder(String url,
                                                          String session_id,
                                                          String user_agent,
                                                          String content_type){
        Request.Builder request = new Request.Builder()
                .url(url)
                .header("User-Agent", user_agent)
                .addHeader("Content-Type", content_type);
        if (!session_id.equals("")){
            request.addHeader("Cookie", session_id);
        }
        return request;
    }

    public static RequestBody getRequestBody(JSONObject json){
        return new okhttp3.FormBody.Builder()
                .add("signed_body", "." + json.toString())
                .add("ig_sig_key_version","4")
                .build();
    }


    public static JSONObject getSuccessfulLoginParams(Response response) throws IOException {
        JSONObject r_json = new JSONObject();
        boolean isSuccess = false;
        ResponseBody responseBody = response.body();
        try {
            if (responseBody != null) {
                JSONObject responseJson = new JSONObject(responseBody.string());
                if (responseJson.has("logged_in_user")) {
                    JSONObject jsonLoggedInUser = responseJson.getJSONObject("logged_in_user");
                    String sessionid = "";
                    if (!response.headers("Set-Cookie").isEmpty()) {

                        for (String cookies : response.headers("Set-Cookie")) {
                            if (cookies.contains("sessionid=")) {
                                Pattern pattern = Pattern.compile("sessionid=(.*?);");
                                Matcher matcher = pattern.matcher(cookies);
                                if (matcher.find()) {
                                    sessionid = matcher.group().replace("sessionid=", "");
                                    sessionid = sessionid.replace(";", "");
                                    r_json.put("sessionid", sessionid);
                                    r_json.put("userid", jsonLoggedInUser.getString("pk"));
                                    isSuccess = true;
                                }

                            }

                        }
                    }
                }
            }
            r_json.put("is_success", isSuccess);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return r_json;
    }

    public static void checkPermission(Activity context) {

        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);


        }
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},2);
        }
    }
}