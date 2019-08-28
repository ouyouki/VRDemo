package com.baidu.cloud.mediaproc.sample.ui.shortvideo;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityCaptureBinding;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter.FiltersAdapter;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.dialog.TuneDialogFragment;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.fragment.BackgroundMusicFragment;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.fragment.BeautyFragment;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.fragment.FilterFragment;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnFilterChoseListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnMusicChoseListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnSkinBeautyListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnTuneListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.viewmodel.CaptureViewModel;
import com.baidu.cloud.mediaproc.sample.util.model.Music;
import com.baidu.cloud.mediaproc.sample.util.rx.RxBusHelper;
import com.baidu.cloud.mediaproc.sample.util.rx.event.MusicChooseEvent;

public class CaptureActivity extends AppCompatActivity implements OnTuneListener,
        OnSkinBeautyListener, OnFilterChoseListener, OnMusicChoseListener {
    private static final String TAG = "CaptureActivity";
    private ActivityCaptureBinding binding;
    private CaptureViewModel viewModel;

    private Bundle beautyFragmentArgs;
    private Bundle tuneFragmentArgs;
    private Bundle musicFragmentArgs;

    private Music currMusic;

    private BottomSheetBehavior<ConstraintLayout> behavior;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: ");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_capture);

        behavior = BottomSheetBehavior.from(binding.designBottomSheet);

        // TODO: 19/04/2017  add set default param button,none,etc.
        viewModel = new CaptureViewModel(CaptureActivity.this, binding);
        binding.setModel(viewModel);
        beautyFragmentArgs = new Bundle();
        tuneFragmentArgs = new Bundle();
        tuneFragmentArgs.putInt("width", viewModel.videoWidth);
        tuneFragmentArgs.putInt("height", viewModel.videoHeight);
        musicFragmentArgs = new Bundle();

        final TransitionDrawable transition = (TransitionDrawable) binding.imageButton8.getBackground();
        final TransitionDrawable transition1 = (TransitionDrawable) binding.imageButton7.getBackground();
        final TransitionDrawable transition2 = (TransitionDrawable) binding.imageButton9.getBackground();
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    viewModel.isFragmentAdd.set(false);
                    transition.reverseTransition(150);
                    transition1.reverseTransition(150);
                    transition2.reverseTransition(150);
                } else {
                    viewModel.isFragmentAdd.set(true);
                    transition.startTransition(150);
                    transition1.startTransition(150);
                    transition2.startTransition(150);
                }
            }
        });
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
        DialogFragment tune = (DialogFragment) getSupportFragmentManager().findFragmentByTag("tune");
        if (tune == null) {
            tune = new TuneDialogFragment();
        }
        FragmentManager fm = getSupportFragmentManager();
        for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
            fm.popBackStack();
        }
        if (!tune.isVisible()) {
            if (tune.getArguments() != null) {
                tune.getArguments().putAll(tuneFragmentArgs);
            } else {
                tune.setArguments(tuneFragmentArgs);
            }
            tune.show(getSupportFragmentManager(), "tune");
        }
    }

    public void onClickBGM(View view) {
        Fragment bgm = getSupportFragmentManager().findFragmentByTag("bgm");
        if (bgm == null) {
            bgm = new BackgroundMusicFragment();
        }
        if (!bgm.isVisible()) {
            musicFragmentArgs.putInt("time", viewModel.captureTimeInMs);
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
            filterDialogFragment = new FilterFragment();
        }
        if (!filterDialogFragment.isVisible()) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, filterDialogFragment, "filter")
                    .addToBackStack("filter")
                    .commit();
        }
        binding.textCaptureFilterName.requestLayout();
    }

    public void onClickBeauty(View view) {
        Fragment beautyFragment = getSupportFragmentManager().findFragmentByTag("beauty");
        if (beautyFragment == null) {
            beautyFragment = new BeautyFragment();
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
                    .addToBackStack("beauty")
                    .commit();
        }
    }

    @Override
    public void onResolutionChose(int width, int height) {
        tuneFragmentArgs.putInt("width", width);
        tuneFragmentArgs.putInt("height", height);
        viewModel.onResolutionChose(width, height);
    }

    @Override
    public void onTimeChose(int seconds) {
        tuneFragmentArgs.putInt("capture_time", seconds);
        viewModel.onTimeChose(seconds);
    }

    @Override
    public void onVolumeChange(float volume) {
        tuneFragmentArgs.putFloat("volume", volume);
        viewModel.onVolumeChange(volume);
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
        viewModel.onFilterChose(filter);
    }

    @Override
    public void onMusicChose(Music music) {
        musicFragmentArgs.putParcelable("music", currMusic);
        currMusic = music;
        viewModel.onMusicChose(music);
        onIntervalChose(0);
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

    public void onClickClose(View view) {
        finish();
    }
}
