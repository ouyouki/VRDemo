package com.baidu.cloud.mediaproc.sample.ui.shortvideo.dialog;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableArrayMap;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableMap;
import android.databinding.adapters.SeekBarBindingAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.SeekBar;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.DialogBaseTuneBinding;
import com.baidu.cloud.mediaproc.sample.ui.base.BaseDialogFragment;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnTuneListener;

public class TuneDialogFragment extends BaseDialogFragment {

    private static final String TAG = "TuneDialogFragment";
    private DialogBaseTuneBinding binding;
    private OnTuneListener onTuneListener;
    private String currResolution = "720p";
    private int width;
    private int height;
    private int currCaptureTimeSeconds = 15;
    private ObservableMap<String, String> map;
    private ObservableMap<String, Integer> progressMap;
    private ObservableBoolean isSeeking = new ObservableBoolean(false);

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof OnTuneListener) {
            onTuneListener = (OnTuneListener) getActivity();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (map == null) {
            map = new ObservableArrayMap<>();
            progressMap = new ObservableArrayMap<>();
            progressMap.put("volume", 100);
            progressMap.put("brightness", 50);
            progressMap.put("contrast", 25);
            progressMap.put("saturation", 50);
            progressMap.put("hue", 50);
            progressMap.put("sharpness", 50);
        }
        if (args != null) {
            width = args.getInt("width", 1280);
            height = args.getInt("height", 720);
            float brightness = args.getFloat("brightness", 0.0f);
            float contrast = args.getFloat("contrast", 1.0f);
            float saturation = args.getFloat("saturation", 1.0f);
            float hue = args.getFloat("hue", 0.0f);
            float sharpness = args.getFloat("sharpness", 0.0f);
            progressMap.put("volume", (int) (args.getFloat("volume", 1) * 100));
            currCaptureTimeSeconds = args.getInt("capture_time", 15);
            map.put("volume", progressMap.get("volume") + "%");
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
        switch (currCaptureTimeSeconds) {
            case 15:
                binding.dialogTimeRb1.setChecked(true);
                break;
            case 30:
                binding.dialogTimeRb2.setChecked(true);
                break;
            case 45:
                binding.dialogTimeRb3.setChecked(true);
                break;
            case 60:
                binding.dialogTimeRb4.setChecked(true);
                break;
            default:
                break;
        }
        switch (width) {
            case 480:
                binding.tuneRs1.setChecked(true);
                break;
            case 640:
                binding.tuneRs2.setChecked(true);
                break;
            case 1280:
                binding.tuneRs3.setChecked(true);
                break;
            case 1920:
                binding.tuneRs4.setChecked(true);
                break;
            default:
                break;
        }
        binding.setOnResolutionChose(onResolutionChose);
        binding.setOnTimeChose(onTimeChose);
        binding.setOnParamsChange(onParamsChange);
        binding.setStartTrackingTouch(startTrackingTouch);
        binding.setOnStopTrackingTouch(stopTrackingTouch);
        binding.dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        binding.setParamMap(map);
        binding.setProgressMap(progressMap);
        binding.setIsSeeking(isSeeking);
        super.onViewCreated(view, savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_base_tune, container, false);
        return v;
    }

    private final View.OnClickListener onResolutionChose = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v instanceof RadioButton) {
                String resolution = ((RadioButton) v).getText().toString();
                if (resolution.equals(currResolution))
                    return;
                currResolution = resolution;
                switch (resolution) {
                    case "360p":
                        width = 480;
                        height = 360;
                        break;
                    case "480p":
                        width = 640;
                        height = 480;
                        break;
                    case "720p":
                        width = 1280;
                        height = 720;
                        break;
                    case "1080p":
                        width = 1920;
                        height = 1080;
                        break;
                }
                if (onTuneListener != null) {
                    onTuneListener.onResolutionChose(width, height);
                }
            }
        }
    };

    private final View.OnClickListener onTimeChose = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v instanceof RadioButton) {
                int time = Integer.parseInt(((RadioButton) v).getText().toString().split("s")[0]);
                if (time == currCaptureTimeSeconds)
                    return;
                currCaptureTimeSeconds = time;
                if (onTuneListener != null) {
                    onTuneListener.onTimeChose(time);
                }
            }
        }
    };

    private final SeekBarBindingAdapter.OnProgressChanged onParamsChange = new SeekBarBindingAdapter.OnProgressChanged() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (onTuneListener != null && fromUser) {
                float strength;
                switch (seekBar.getId()) {
                    case R.id.tune_seek_volume:
                        onTuneListener.onVolumeChange(progress / 100f);
                        map.put("volume", progress + "%");
                        progressMap.put("volume", progress);
                        break;
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
                }
            }
        }
    };


}
