package com.inview.instagram.story.downloader;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Base64;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
    static String URL_HOST = "https://i.instagram.com";
    static String PATH_GET_FOLLOWERS = "/api/v1/friendships/%s/followers/";
    static String PATH_GET_FOLLOWINGS = "/api/v1/friendships/%s/following/";
    static String PATH_GET_STORY = "/api/v1/feed/user/%s/story/";
    static String PATH_USER_INFO = "/api/v1/users/%s/info/";
    static String PATH_USER_SEARCH = "/api/v1/users/search/";
    static String PATH_LOGOUT = "/api/v1/accounts/logout/";
    static String PATH_LOGIN = "/api/v1/accounts/login/";
    static String PATH_LOGIN_2FA = "/api/v1/accounts/two_factor_login/";
    static String PATH_REELS_TRAY = "/api/v1/feed/reels_tray/";
    static String PATH_REELS_MEDIA = "/api/v1/feed/reels_media/";
    static String CONTENT_TYPE = "Application/x-www-form-urlencoded";
    static String USER_AGENT = "Instagram 99.0.0.32.182 Android";
    static String URL_PP_DOWNLOAD = "https://instadp-cors-222621.appspot.com/get-hd?id=";
    static String URL_PP_DOWNLOAD_IZ = "https://izuum.com/index.php";
    static String BANNER_UNIT_ID = "ca-app-pub-2181561381492488/3718457776";
    static String NATIVE_AD_UNIT_ID = "ca-app-pub-2181561381492488/2213804414";


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
        byte[] keyPhraseInBytes;
        try {
            keyPhraseInBytes = md5hash(keyphrase.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyPhraseInBytes, "AES");
            instance = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            instance.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(initVector));
            byte[] encrypted = instance.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encrypted,Base64.NO_WRAP);

        } catch (BadPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


    public static String decrypt(String encrypted, String keyphrase, String initVector){
        Cipher instance;
        byte[] keyPhraseInBytes;
        byte[] initVectorInBytes = Base64.decode(initVector,Base64.NO_WRAP);
        try {
            keyPhraseInBytes = md5hash(keyphrase.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyPhraseInBytes, "AES");
            instance = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            instance.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(initVectorInBytes));
            byte[] decrypted = instance.doFinal(Base64.decode(encrypted, Base64.NO_WRAP));
            return new String(decrypted);
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


    public static OkHttpClient getHttpClient(){
        String hostname = "i.instagram.com";
        CertificatePinner certificatePinner = new CertificatePinner.Builder()
                //.add(hostname, "sha256/mreKTxeq4bRmIPe8oiojs3P40B5t0z49e9E7lA7besM=")
                //.add(hostname, "sha256/k2v657xBsOVe1PQRwOsHsw3bsGT2VzIqz5K+59sNQws=")
                //.add(hostname, "sha256/WoiWRyIOVNa9ihaBciRSC7XHjliYS9VwUGOIud4PB18=")
                .build();

        return new OkHttpClient.Builder()
                .certificatePinner(certificatePinner)
                .build();
    }


    public static Request.Builder getRequestHeaderBuilder(String url,
                                                          String sessionId,
                                                          String userAgent,
                                                          String contentType){
        Request.Builder request = new Request.Builder()
                .url(url)
                .header("User-Agent", userAgent);
        if (!contentType.equals("")){
            request.addHeader("Content-Type", contentType);
        }

        if (!sessionId.equals("")){
            request.addHeader("Cookie", sessionId);
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
        JSONObject returnJson = new JSONObject();
        boolean isSuccess = false;
        ResponseBody responseBody = response.body();
        try {
            if (responseBody != null) {
                JSONObject responseJson = new JSONObject(responseBody.string());
                if (responseJson.has("logged_in_user")) {
                    JSONObject jsonLoggedInUser = responseJson.getJSONObject("logged_in_user");
                    String sessionid;
                    if (!response.headers("Set-Cookie").isEmpty()) {

                        for (String cookies : response.headers("Set-Cookie")) {
                            if (cookies.contains("sessionid=")) {
                                Pattern pattern = Pattern.compile("sessionid=(.*?);");
                                Matcher matcher = pattern.matcher(cookies);
                                if (matcher.find()) {
                                    sessionid = matcher.group().replace("sessionid=", "");
                                    sessionid = sessionid.replace(";", "");
                                    returnJson.put("sessionid", sessionid);
                                    returnJson.put("userid", jsonLoggedInUser.getString("pk"));
                                    isSuccess = true;
                                }

                            }

                        }
                    }
                }
            }
            returnJson.put("is_success", isSuccess);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return returnJson;
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


    public static ArrayList<MediaDownloadEntity> parseDownloadLinkResponse(String response){
        ArrayList<MediaDownloadEntity> mediaDownloadEntities = new ArrayList<>();
        try{
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.has("graphql") &&
                    responseJson.getJSONObject("graphql").has("shortcode_media")){
                JSONObject shortCodeMedia = responseJson.getJSONObject("graphql")
                        .getJSONObject("shortcode_media");
                String configWidth;
                String configHeight;
                String url;
                if (shortCodeMedia.has("__typename")
                        && shortCodeMedia.getString("__typename").equals("GraphImage")
                        && !shortCodeMedia.getBoolean("is_video")){
                    //this is image
                    JSONArray displayResources = shortCodeMedia
                            .getJSONArray("display_resources");
                    for (int i=displayResources.length()-1; i>=0; i--){
                        url = displayResources.getJSONObject(i).getString("src");
                        configHeight = displayResources.getJSONObject(i).getString("config_height");
                        configWidth = displayResources.getJSONObject(i).getString("config_width");
                        mediaDownloadEntities.add(new MediaDownloadEntity(url,
                                configHeight,
                                configWidth,
                                1,
                                shortCodeMedia.getString("id")));
                    }
                }
                else if (shortCodeMedia.has("__typename")
                        && shortCodeMedia.getString("__typename").equals("GraphVideo")
                        && shortCodeMedia.getBoolean("is_video")){
                    //this is video
                    url = shortCodeMedia.getString("video_url");
                    configHeight = shortCodeMedia.getJSONObject("dimensions").getString("height");
                    configWidth = shortCodeMedia.getJSONObject("dimensions").getString("width");
                    mediaDownloadEntities.add(new MediaDownloadEntity(url,
                            configHeight,
                            configWidth,
                            2,
                            shortCodeMedia.getString("id")));
                }
                else if (shortCodeMedia.has("__typename")
                        && shortCodeMedia.getString("__typename").equals("GraphSidecar")
                        && !shortCodeMedia.getBoolean("is_video")){
                    //this is slide media
                    JSONArray slide_edges = shortCodeMedia.getJSONObject("edge_sidecar_to_children")
                            .getJSONArray("edges");
                    mediaDownloadEntities.add(null);
                    for (int k=0; k<slide_edges.length(); k++) {
                        JSONObject node = slide_edges.getJSONObject(k).getJSONObject("node");
                        if (node.has("__typename")
                                && node.getString("__typename").equals("GraphImage")
                                && !node.getBoolean("is_video")){
                            //this is image
                            JSONArray displayResources = node.getJSONArray("display_resources");
                            for (int i=displayResources.length()-1; i>=0; i--){
                                url = displayResources.getJSONObject(i).getString("src");
                                configHeight = displayResources.getJSONObject(i).getString("config_height");
                                configWidth = displayResources.getJSONObject(i).getString("config_width");
                                mediaDownloadEntities.add(new MediaDownloadEntity(url,
                                        configHeight,
                                        configWidth,
                                        1,
                                        node.getString("id")));
                            }

                        }
                        else if (node.has("__typename")
                                && node.getString("__typename").equals("GraphVideo")
                                && node.getBoolean("is_video")){
                            //this is video
                            url = node.getString("video_url");
                            configHeight = node.getJSONObject("dimensions").getString("height");
                            configWidth = node.getJSONObject("dimensions").getString("width");
                            mediaDownloadEntities.add(new MediaDownloadEntity(url,
                                    configHeight,
                                    configWidth,
                                    2,
                                    node.getString("id")));
                        }
                        if (k != slide_edges.length()-1){
                            mediaDownloadEntities.add(null);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mediaDownloadEntities;
    }


    public static ArrayList<MediaDownloadEntity> getHighlightMediaEntities(String response,
                                                                           String highlight_id,
                                                                           String media_id){
        ArrayList<MediaDownloadEntity> mediaDownloadEntities = new ArrayList<>();
        try{
            JSONObject responseJson = new JSONObject(response);
            if (responseJson.has("reels") &&
                    responseJson.getJSONObject("reels").has(highlight_id)){
                JSONObject highlight = responseJson.getJSONObject("reels").getJSONObject(highlight_id);
                JSONArray highlight_items = highlight.getJSONArray("items");
                JSONObject item;
                String reelItemId;
                for (int i=0; i<highlight_items.length(); i++){
                    item = highlight_items.getJSONObject(i);
                    reelItemId = item.getString("id");
                    if(reelItemId.equals(media_id)){
                        int media_type = item.getInt("media_type");
                        if (media_type == 1){
                            JSONArray imgCandidates = item.getJSONObject("image_versions2")
                                    .getJSONArray("candidates");
                            JSONObject candidate;
                            for (int j=0; j<imgCandidates.length(); j++){
                                candidate = imgCandidates.getJSONObject(j);
                                mediaDownloadEntities.add(new MediaDownloadEntity(
                                        candidate.getString("url"),
                                        candidate.getString("height"),
                                        candidate.getString("width"),
                                        1,
                                        media_id));
                            }
                        }
                        else if (media_type == 2){
                            JSONObject videoVersion = item.getJSONArray("video_versions")
                                    .getJSONObject(0);
                            mediaDownloadEntities.add(new MediaDownloadEntity(
                                    videoVersion.getString("url"),
                                    videoVersion.getString("height"),
                                    videoVersion.getString("width"),
                                    2,
                                    media_id));
                            if (item.has("image_versions2")){
                                JSONArray imgCandidates = item.getJSONObject("image_versions2")
                                        .getJSONArray("candidates");
                                mediaDownloadEntities.add(new MediaDownloadEntity(
                                        imgCandidates.getJSONObject(0).getString("url"),
                                        imgCandidates.getJSONObject(0).getString("height"),
                                        imgCandidates.getJSONObject(0).getString("width"),
                                        1,
                                        media_id));
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mediaDownloadEntities;
    }
}
