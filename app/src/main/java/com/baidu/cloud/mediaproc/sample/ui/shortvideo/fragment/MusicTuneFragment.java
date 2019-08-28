package com.baidu.cloud.mediaproc.sample.ui.shortvideo.fragment;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.adapters.SeekBarBindingAdapter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.LayoutMusicTuneBinding;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnMusicChoseListener;
import com.baidu.cloud.mediaproc.sample.util.model.Music;
import com.baidu.cloud.mediaproc.sample.util.rx.RxBusHelper;
import com.baidu.cloud.mediaproc.sample.util.rx.event.MusicChooseEvent;
import com.baidu.cloud.mediaproc.sample.widget.RangeSeekBar;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.disposables.CompositeDisposable;

import static com.baidu.cloud.mediaproc.sample.util.MusicTool.stringForTime;

public class MusicTuneFragment extends Fragment implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener {

    private static final String TAG = "MusicTuneFragment";
    private LayoutMusicTuneBinding binding;
    private OnMusicChoseListener musicChoseListener;
    private int backgroundMusicStartTime;
    private int musicIntervalTimeInMs;

    private Timer timerForPosition;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Music currMusic;
    private float volume;
    private volatile MediaPlayer mediaPlayer;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof OnMusicChoseListener) {
            musicChoseListener = (OnMusicChoseListener) getActivity();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startTimer();
        if (getArguments() != null) {
            musicIntervalTimeInMs = getArguments().getInt("time", 15 * 1000);
            currMusic = getArguments().getParcelable("music");
            volume = getArguments().getFloat("volume", 1.0f);
            backgroundMusicStartTime = getArguments().getInt("startTime", 0);
        }
        RxBusHelper.doOnMainThread(MusicChooseEvent.class, compositeDisposable, new RxBusHelper.OnEventListener<MusicChooseEvent>() {
            @Override
            public void onEvent(MusicChooseEvent musicChooseEvent) {
                setCurrMusic(musicChooseEvent.getMusic());
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setVolume(1.0f, 1.0f);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimer();
        mediaPlayer.release();
        compositeDisposable.dispose();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_music_tune, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = DataBindingUtil.bind(view);

        setCurrMusic(currMusic);
        binding.setOnParamsChange(onParamsChange);
        binding.textView2.setText(stringForTime(backgroundMusicStartTime));
        binding.textView.setText(stringForTime(backgroundMusicStartTime + musicIntervalTimeInMs));

        binding.musicSeekVolume.setProgress((int) (volume * 100));
        binding.musicTvVolume.setText((int) (volume * 100) + "%");
        binding.bgmTimeInterval.setTimeInterval(musicIntervalTimeInMs);
        binding.bgmTimeInterval.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, int minValue, int maxValue) {
                musicChoseListener.onIntervalChose(minValue);
                backgroundMusicStartTime = minValue;
                mediaPlayer.seekTo(minValue);
                binding.textView2.setText(stringForTime(minValue));
                binding.textView.setText(stringForTime(maxValue));
            }
        });
        if (getParentFragment() instanceof View.OnClickListener) {
            binding.button.setOnClickListener((View.OnClickListener) getParentFragment());
        }
    }

    public void setCurrMusic(Music music) {
        currMusic = music;
        if (currMusic != null && currMusic.id != -1) {
            binding.bgmTimeInterval.setEnabled(true);
            binding.bgmTimeInterval.setRangeValues(0, (int) currMusic.duration);
            binding.bgmTimeInterval.setTimeInterval(musicIntervalTimeInMs);
            binding.bgmTimeInterval.setSelectedMinValue(backgroundMusicStartTime);
            resetMediaPlayer();
        } else {
            binding.bgmTimeInterval.setEnabled(false);
            binding.bgmTimeInterval.setRangeValues(0, 100);
            binding.bgmTimeInterval.setTimeInterval(15);
            backgroundMusicStartTime = 0;
            binding.bgmTimeInterval.setSelectedMinValue(backgroundMusicStartTime);
            mediaPlayer.stop();
        }
        binding.bgmTimeInterval.invalidate();
    }

    private final SeekBarBindingAdapter.OnProgressChanged onParamsChange = new SeekBarBindingAdapter.OnProgressChanged() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (musicChoseListener != null && fromUser) {
                switch (seekBar.getId()) {
                    case R.id.music_seek_volume:
                        volume = progress / 100f;
                        musicChoseListener.onMusicVolumeChange(volume);
                        mediaPlayer.setVolume(volume, volume);
                        binding.musicTvVolume.setText(progress + "%");
                        break;
                }
            }
        }
    };

    private void resetMediaPlayer() {
        try {
            mediaPlayer.reset();
            if (currMusic != null) {
                mediaPlayer.setDataSource(getContext(), Uri.parse(currMusic.uri));
                mediaPlayer.prepareAsync();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (backgroundMusicStartTime > 0) {
            mp.seekTo(backgroundMusicStartTime);
        }
        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        resetMediaPlayer(); // for looping manually
    }

    private void startTimer() {
        if (timerForPosition != null) {
            timerForPosition.cancel();
            timerForPosition = null;
        }
        timerForPosition = new Timer();
        timerForPosition.schedule(new TimerTask() {
            @Override
            public void run() {
                // check music position
                try {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        long currentPlayPosition = mediaPlayer.getCurrentPosition();
                        if (currentPlayPosition > 0 && currentPlayPosition >= backgroundMusicStartTime + musicIntervalTimeInMs) {
                            Log.d(TAG, "timer.schedule; will reset mediaplayer");
                            resetMediaPlayer();
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, Log.getStackTraceString(e));
                }

            }
        }, 200, 200);
    }

    private void stopTimer() {
        if (timerForPosition != null) {
            timerForPosition.cancel();
            timerForPosition = null;
        }
    }

}