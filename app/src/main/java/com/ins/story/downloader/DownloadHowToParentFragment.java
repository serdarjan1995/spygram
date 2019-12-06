package com.ins.story.downloader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.rd.PageIndicatorView;

public class DownloadHowToParentFragment extends Fragment {
    private final int pageSize = 8;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.content_download_how_to, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ViewPager pager = view.findViewById(R.id.ViewPagerHowTo);
        HowToDownloadFragmentAdapter adapter = new HowToDownloadFragmentAdapter(getChildFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        pager.setAdapter(adapter);
        PageIndicatorView indicatorView = view.findViewById(R.id.indicatorViewHowTo);
        indicatorView.setCount(pageSize);
    }


    public class HowToDownloadFragmentAdapter extends FragmentPagerAdapter {
        public HowToDownloadFragmentAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0: return HowToDownloadFragment.newInstance(R.drawable.download_how_to_1);
                case 1: return HowToDownloadFragment.newInstance(R.drawable.download_how_to_2);
                case 2: return HowToDownloadFragment.newInstance(R.drawable.download_how_to_3);
                case 3: return HowToDownloadFragment.newInstance(R.drawable.download_how_to_4);
                case 4: return HowToDownloadFragment.newInstance(R.drawable.download_how_to_5);
                case 5: return HowToDownloadFragment.newInstance(R.drawable.download_how_to_6);
                case 6: return HowToDownloadFragment.newInstance(R.drawable.download_how_to_7);
                case 7: return HowToDownloadFragment.newInstance(R.drawable.download_how_to_8);
                default: return null;
            }

        }

        @Override
        public int getCount() {
            return pageSize;
        }
    }

}
