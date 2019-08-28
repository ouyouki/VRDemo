package com.baidu.cloud.mediaproc.sample.ui.shortvideo.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.util.model.Music;

public class BackgroundMusicProcessFragment extends Fragment implements View.OnClickListener {

    private MusicPagerAdapter pagerAdapter;
    private ViewPager pager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pagerAdapter = new MusicPagerAdapter(getChildFragmentManager());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pager = (ViewPager) view.findViewById(R.id.viewPager);
        pagerAdapter.setArguments(getArguments());
        pager.setAdapter(pagerAdapter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_music, container, false);
        return v;
    }

    @Override
    public void onClick(View v) {
        pager.setCurrentItem((pager.getCurrentItem() + 1) % pagerAdapter.getCount(), true);
    }

    private class MusicPagerAdapter extends FragmentPagerAdapter {

        private final Fragment chooseFragment;
        private final Fragment tuneFragment;

        MusicPagerAdapter(FragmentManager fm) {
            super(fm);
            chooseFragment = new MusicChooseProcessFragment();
            tuneFragment = new MusicTuneProcessFragment();
        }

        void setArguments(Bundle arguments) {
            if (chooseFragment.getArguments() != null) {
                chooseFragment.getArguments().putAll(arguments);
            } else {
                chooseFragment.setArguments(arguments);
            }
            if (tuneFragment.getArguments() != null) {
                tuneFragment.getArguments().putAll(arguments);
            } else {
                tuneFragment.setArguments(arguments);
            }
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return chooseFragment;
            } else {
                return tuneFragment;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
