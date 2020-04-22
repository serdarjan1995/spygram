package com.ins.story.downloader;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public class ViewFragment extends Fragment {

    private int layout;
    public ViewFragment(int layout) {
        this.layout = layout;
    }

    public ViewFragment() {
        // empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(this.layout, container, false);

        return rootView;
    }

}