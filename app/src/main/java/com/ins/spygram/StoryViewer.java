package com.ins.spygram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import android.os.Bundle;
import android.view.ViewManager;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import steelkiwi.com.library.view.IndicatorView;

public class StoryViewer extends AppCompatActivity {

    private ArrayList<String> arraylisturl = new ArrayList<String>();
    private ArrayList<Integer> arraylistmediaType = new ArrayList<Integer>();
    private ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_layout);
        Bundle b = getIntent().getExtras();
        String response = "";

        pager = findViewById(R.id.ViewPagerStoryImages);
        if(b != null) {
            response = b.getString("response");
            try {
                JSONObject json = new JSONObject(response);
                if(json.has("reel") && json.isNull("reel")){
                    Toast.makeText(this,"No stories for this user", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                JSONArray items  = json.getJSONObject("reel").getJSONArray("items");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    int mediaType = item.getInt("media_type");
                    if (mediaType == 1){
                        JSONArray imgCandidates  = item.getJSONObject("image_versions2").getJSONArray("candidates");
                        String img_url = imgCandidates.getJSONObject(0).getString("url");
                        arraylisturl.add(img_url);
                        arraylistmediaType.add(mediaType);
                    }
                    else if(mediaType == 2){
                        JSONObject videoversion  = item.getJSONArray("video_versions").getJSONObject(0);
                        String video_url = videoversion.getString("url");
                        arraylisturl.add(video_url);
                        arraylistmediaType.add(mediaType);
                    }


                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        ImageAdapter adapter = new ImageAdapter(this,arraylisturl,arraylistmediaType);
        pager.setAdapter(adapter);
        IndicatorView indicatorView = (IndicatorView) findViewById(R.id.indicatorView);
        indicatorView.attachViewPager(pager);
        if (arraylisturl.size() <= 2){
            ((ViewManager)indicatorView.getParent()).removeView(indicatorView);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }


}
