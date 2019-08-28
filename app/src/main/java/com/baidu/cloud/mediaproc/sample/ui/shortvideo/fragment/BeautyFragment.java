package com.baidu.cloud.mediaproc.sample.ui.shortvideo.fragment;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableArrayMap;
import android.databinding.ObservableMap;
import android.databinding.adapters.SeekBarBindingAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.DialogBeautyFaceBinding;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnSkinBeautyListener;

public class BeautyFragment extends Fragment {

    private static final String TAG = "BeautyFragment";
    private DialogBeautyFaceBinding binding;
    private OnSkinBeautyListener onSkinBeautyListener;
    private ObservableMap<String, Integer> map;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof OnSkinBeautyListener) {
            onSkinBeautyListener = (OnSkinBeautyListener) getActivity();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (map == null) {
            map = new ObservableArrayMap<>();
            map.put("smooth", 0);
            map.put("bright", 0);
            map.put("pink", 0);
        }
        if (getArguments() != null) {
            map.put("smooth", getArguments().getInt("smooth"));
            map.put("bright", getArguments().getInt("bright"));
            map.put("pink", getArguments().getInt("pink"));
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.bind(view);
        binding.setOnParamsChange(onParamsChange);
        binding.setParamMap(map);
        super.onViewCreated(view, savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_beauty_face, container, false);
        return v;
    }


    private final SeekBarBindingAdapter.OnProgressChanged onParamsChange = new SeekBarBindingAdapter.OnProgressChanged() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (onSkinBeautyListener != null && fromUser) {
                switch (seekBar.getId()) {
                    case R.id.beauty_seek_smooth:
                        onSkinBeautyListener.onSmoothChange(progress / 100f);
                        map.put("smooth", progress);
                        break;
                    case R.id.beauty_seek_bright:
                        onSkinBeautyListener.onBrightChange(progress / 100f);
                        map.put("bright", progress);
                        break;
                    case R.id.beauty_seek_pink:
                        onSkinBeautyListener.onPinkChange(progress / 100f);
                        map.put("pink", progress);
                        break;
                }
            }
        }
    };

}
