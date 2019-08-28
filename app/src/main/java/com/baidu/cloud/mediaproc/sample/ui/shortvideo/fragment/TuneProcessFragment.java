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
import com.baidu.cloud.mediaproc.sample.databinding.DialogBaseTuneProcessBinding;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnProcessTuneListener;


public class TuneProcessFragment extends Fragment {

    private static final String TAG = "TuneProcessFragment";
    private DialogBaseTuneProcessBinding binding;
    private OnProcessTuneListener onTuneListener;
    private ObservableMap<String, String> map;
    private ObservableMap<String, Integer> progressMap;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof OnProcessTuneListener) {
            onTuneListener = (OnProcessTuneListener) getActivity();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (map == null) {
            map = new ObservableArrayMap<>();
            progressMap = new ObservableArrayMap<>();
            progressMap.put("brightness", 50);
            progressMap.put("contrast", 25);
            progressMap.put("saturation", 50);
            progressMap.put("hue", 50);
            progressMap.put("sharpness", 50);
        }
        if (args != null) {
            float brightness = args.getFloat("brightness", 0.0f);
            float contrast = args.getFloat("contrast", 1.0f);
            float saturation = args.getFloat("saturation", 1.0f);
            float hue = args.getFloat("hue", 0.0f);
            float sharpness = args.getFloat("sharpness", 0.0f);
            map.put("brightness", String.format("%.1f", brightness));
            map.put("contrast", String.format("%.1f", contrast));
            map.put("saturation", String.format("%.1f", saturation));
            map.put("hue", String.format("%.1f", hue));
            map.put("sharpness", String.format("%.1f", sharpness));
            progressMap.put("brightness", (int) (brightness * 50 + 50));
            progressMap.put("contrast", (int) (contrast * 25));
            progressMap.put("saturation", (int) (saturation * 50));
            progressMap.put("hue", (int) ((hue + 180) / 3.6));
            progressMap.put("sharpness", (int) ((sharpness + 4) * 12.5));
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.bind(view);
        binding.setOnParamsChange(onParamsChange);
        binding.dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });
        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });
        binding.setParamMap(map);
        binding.setProgressMap(progressMap);
        super.onViewCreated(view, savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_base_tune_process, container, false);
        return v;
    }

    private final SeekBarBindingAdapter.OnProgressChanged onParamsChange =
            new SeekBarBindingAdapter.OnProgressChanged() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (onTuneListener != null && fromUser) {
                        float strength;
                        switch (seekBar.getId()) {
                            case R.id.tune_seek_brightness:
                                strength = (progress - 50) / 50f;
                                onTuneListener.onBrightnessChange(strength);
                                map.put("brightness", String.format("%.1f", strength));
                                break;
                            case R.id.tune_seek_contrast:
                                strength = progress / 25f;
                                onTuneListener.onContrastChange(strength);
                                map.put("contrast", String.format("%.1f", strength));
                                break;
                            case R.id.tune_seek_saturation:
                                strength = progress / 50f;
                                onTuneListener.onSaturationChange(strength);
                                map.put("saturation", String.format("%.1f", strength));
                                break;
                            case R.id.tune_seek_hue:
                                strength = progress * 3.6f - 180;
                                onTuneListener.onHueChange(strength);
                                map.put("hue", String.format("%.1f", strength));
                                break;
                            case R.id.tune_seek_sharpness:
                                strength = progress / 12.5f - 4;
                                onTuneListener.onSharpnessChange(strength);
                                map.put("sharpness", String.format("%.1f", strength));
                                break;
                            default:
                                break;
                        }
                    }
                }
            };
}
