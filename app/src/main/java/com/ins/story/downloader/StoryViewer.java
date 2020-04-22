package com.ins.story.downloader;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.rd.PageIndicatorView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class StoryViewer extends FragmentActivity {

    private ArrayList<StoryEntity> storyEntities = new ArrayList<>();
    private ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_layout);
        Bundle b = getIntent().getExtras();
        String username;
        ImageView backButton = findViewById(R.id.story_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        pager = findViewById(R.id.ViewPagerStory);
        if(b != null) {
            username = b.getString("username");
            if (username != null) {
                storyEntities = b.getParcelableArrayList("storyEntities");
            }
            else{
                toastMsg(getString(R.string.gen_error));
                finish();
            }
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
