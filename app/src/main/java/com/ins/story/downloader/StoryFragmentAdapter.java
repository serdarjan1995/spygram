package com.ins.story.downloader;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

public class StoryFragmentAdapter extends FragmentStatePagerAdapter {
    private ArrayList<StoryEntity> storyEntities;

    public StoryFragmentAdapter(@NonNull FragmentManager fm, int behavior, ArrayList<StoryEntity> storyEntities) {
        super(fm, behavior);
        this.storyEntities = storyEntities;
    }

    @NonNull
    @Override
    public MediaFragment getItem(int position) {
        return MediaFragment.newInstance(storyEntities.get(position));
    }

    @Override
    public int getCount() {
        return storyEntities.size();
    }
}
