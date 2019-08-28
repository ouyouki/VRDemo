package com.baidu.cloud.mediaproc.sample.ui.shortvideo;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityConfigProcessBinding;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.fragment.BackgroundMusicProcessFragment;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.fragment.BeautyProcessFragment;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.fragment.FilterProcessFragment;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.fragment.TuneProcessFragment;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnFilterChoseListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnMusicChoseListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnProcessTuneListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnSkinBeautyListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.viewmodel.ConfigProcessViewModel;
import com.baidu.cloud.mediaproc.sample.util.model.Music;
import com.baidu.cloud.mediaproc.sample.util.rx.RxBusHelper;
import com.baidu.cloud.mediaproc.sample.util.rx.event.MusicChooseEvent;
import com.bumptech.glide.Glide;

public class ConfigProcessActivity extends AppCompatActivity implements OnProcessTuneListener,
        OnSkinBeautyListener, OnFilterChoseListener, OnMusicChoseListener {
    private static final String TAG = "ConfigProcessActivity";
    public static final String EXTRA_VIDEO_PATH = "video_path";
    private ActivityConfigProcessBinding binding;
    private ConfigProcessViewModel viewModel;

    private Bundle beautyFragmentArgs;
    private Bundle tuneFragmentArgs;
    private Bundle musicFragmentArgs;
    private Bundle filterFragmentArgs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        if (TextUtils.isEmpty(videoPath)) {
            Toast.makeText(this, "必须传入视频文件的路径", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_config_process);
            viewModel = new ConfigProcessViewModel(this, binding.surfaceView,
                    binding.frameLayout1, videoPath);
            binding.setModel(viewModel);

            beautyFragmentArgs = new Bundle();
            tuneFragmentArgs = new Bundle();
            musicFragmentArgs = new Bundle();
            filterFragmentArgs = new Bundle();

            Glide.with(this)
                    .load(videoPath)
                    .fitCenter()
                    .into(binding.imageView);
            getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
                @Override
                public void onBackStackChanged() {
                    viewModel.isFragmentAdd.set(getSupportFragmentManager().getBackStackEntryCount() != 0);
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewModel.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else {
            FragmentManager fm = getSupportFragmentManager();
            for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
                fm.popBackStack();
            }
        }
    }

    public void onClickTune(View view) {
        Fragment tune = getSupportFragmentManager().findFragmentByTag("tune");
        if (tune == null) {
            tune = new TuneProcessFragment();
        }
        if (!tune.isVisible()) {
            if (tune.getArguments() != null) {
                tune.getArguments().putAll(tuneFragmentArgs);
            } else {
                tune.setArguments(tuneFragmentArgs);
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, tune, "tune")
                    .addToBackStack("tune")
                    .commit();
        }
    }

    public void onClickBGM(View view) {
        Fragment bgm = getSupportFragmentManager().findFragmentByTag("bgm");
        if (bgm == null) {
            bgm = new BackgroundMusicProcessFragment();
        }
        if (!bgm.isVisible()) {
            musicFragmentArgs.putInt("time", viewModel.durationInMilliSec);
            if (bgm.getArguments() != null) {
                bgm.getArguments().putAll(musicFragmentArgs);
            } else {
                bgm.setArguments(musicFragmentArgs);
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, bgm, "bgm")
                    .addToBackStack("bgm")
                    .commit();
        }
    }

    public void onClickFilter(View view) {
        Fragment filterDialogFragment = getSupportFragmentManager().findFragmentByTag("filter");
        if (filterDialogFragment == null) {
            filterDialogFragment = new FilterProcessFragment();
        }
        if (!filterDialogFragment.isVisible()) {
            if (filterDialogFragment.getArguments() != null) {
                filterDialogFragment.getArguments().putAll(filterFragmentArgs);
            } else {
                filterDialogFragment.setArguments(filterFragmentArgs);
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, filterDialogFragment, "filter")
                    .addToBackStack("filter")
                    .commit();
        }
    }

    public void onClickBeauty(View view) {
        Fragment beautyFragment = getSupportFragmentManager().findFragmentByTag("beauty");
        if (beautyFragment == null) {
            beautyFragment = new BeautyProcessFragment();
        }
        if (!beautyFragment.isVisible()) {
            if (beautyFragment.getArguments() != null) {
                beautyFragment.getArguments().putAll(beautyFragmentArgs);
            } else {
                beautyFragment.setArguments(beautyFragmentArgs);
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, beautyFragment, "beauty")
                    .addToBackStack("filter")
                    .commit();
        }
    }

    @Override
    public void onBrightnessChange(float brightness) {
        tuneFragmentArgs.putFloat("brightness", brightness);
        viewModel.onBrightnessChange(brightness);
    }

    @Override
    public void onContrastChange(float contrast) {
        tuneFragmentArgs.putFloat("contrast", contrast);
        viewModel.onContrastChange(contrast);
    }

    @Override
    public void onSaturationChange(float saturation) {
        tuneFragmentArgs.putFloat("saturation", saturation);
        viewModel.onSaturationChange(saturation);
    }

    @Override
    public void onHueChange(float hue) {
        tuneFragmentArgs.putFloat("hue", hue);
        viewModel.onHueChange(hue);
    }

    @Override
    public void onSharpnessChange(float sharpness) {
        tuneFragmentArgs.putFloat("sharpness", sharpness);
        viewModel.onSharpnessChange(sharpness);
    }

    @Override
    public void onSmoothChange(float smooth) {
        beautyFragmentArgs.putInt("smooth", (int) (smooth * 100));
        viewModel.onSmoothChange(smooth);
    }

    @Override
    public void onBrightChange(float bright) {
        beautyFragmentArgs.putInt("bright", (int) (bright * 100));
        viewModel.onBrightChange(bright);
    }

    @Override
    public void onPinkChange(float pink) {
        beautyFragmentArgs.putInt("pink", (int) (pink * 100));
        viewModel.onPinkChange(pink);
    }

    @Override
    public void onFilterChose(String filter) {
        filterFragmentArgs.putString("filter", filter);
        viewModel.onFilterChose(filter);
    }

    @Override
    public void onMusicChose(Music music) {
        musicFragmentArgs.putParcelable("music", music);
        viewModel.onMusicChose(music);
        RxBusHelper.post(new MusicChooseEvent(music));
    }

    @Override
    public void onMusicVolumeChange(float volume) {
        musicFragmentArgs.putFloat("volume", volume);
        viewModel.onMusicVolumeChange(volume);
    }

    @Override
    public void onMusicSetDone() {
        viewModel.onMusicSetDone();
    }

    @Override
    public void onIntervalChose(int start) {
        musicFragmentArgs.putInt("startTime", start);
        viewModel.onIntervalChose(start);
    }

    public void onClickTopLeft(View view) {
        finish();
    }
}
