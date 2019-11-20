package com.ins.spygram;


import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoryFragmentAdapter extends FragmentStatePagerAdapter {
    private ArrayList<StoryEntity> storyEntities;
    private Map<String,MediaFragment> fragments;

    public StoryFragmentAdapter(@NonNull FragmentManager fm, int behavior, ArrayList<StoryEntity> storyEntities) {
        super(fm, behavior);
        this.storyEntities = storyEntities;
        fragments = new HashMap<>();
    }

    @NonNull
    @Override
    public MediaFragment getItem(int position) {
        if(!fragments.containsKey(""+position)){
            fragments.put(""+position,MediaFragment.newInstance(storyEntities.get(position)));
        }

        return fragments.get(""+position);
    }

    @Override
    public int getCount() {
        return storyEntities.size();
    }
}
