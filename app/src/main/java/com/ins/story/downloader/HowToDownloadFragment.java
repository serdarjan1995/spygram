package com.ins.story.downloader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

public class HowToDownloadFragment extends Fragment {
    private int resId;

    public static HowToDownloadFragment newInstance(int resId) {
        HowToDownloadFragment fragment = new HowToDownloadFragment();
        Bundle args = new Bundle();
        args.putInt("resId", resId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resId = getArguments().getInt("resId", 0);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.how_to_download_fragment, container, false);
        ImageView imageView = view.findViewById(R.id.how_to_download_image_view);
        imageView.setImageResource(resId);
        return view;
    }

}
