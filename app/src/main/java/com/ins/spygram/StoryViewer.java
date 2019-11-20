package com.ins.spygram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.VideoView;

import com.rd.PageIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class StoryViewer extends FragmentActivity {

    private ArrayList<StoryEntity> storyEntities = new ArrayList<>();
    private ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_layout);
        Bundle b = getIntent().getExtras();
        String response;

        pager = findViewById(R.id.ViewPagerStory);
        if(b != null) {
            response = b.getString("response");
            if (response != null) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.has("reel") && json.isNull("reel")) {
                        toastMsg(getString(R.string.no_story));
                        finish();
                        return;
                    }
                    JSONArray items = json.getJSONObject("reel").getJSONArray("items");
                    String username = json.getJSONObject("reel").getJSONObject("user").getString("username");

                    for (int i = 0; i < items.length(); i++) {
                        storyEntities.add(new StoryEntity(items.getJSONObject(i),username));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }


        final List<MediaFragment> fragments = new ArrayList<>();
        for (int i=0; i<storyEntities.size(); i++){
            fragments.add(MediaFragment.newInstance(storyEntities.get(i)));
        }

        final StoryFragmentAdapter adapter = new StoryFragmentAdapter(getSupportFragmentManager(),
                FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT, storyEntities);
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        PageIndicatorView indicatorView = findViewById(R.id.indicatorView);
        indicatorView.setCount(storyEntities.size());

    }

    public void toastMsg(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onBackPressed() {
        finish();
    }


}
