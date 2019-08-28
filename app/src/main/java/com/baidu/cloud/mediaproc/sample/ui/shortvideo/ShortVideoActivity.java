package com.baidu.cloud.mediaproc.sample.ui.shortvideo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter.VideoAdapter;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityShortvideoBinding;
import com.baidu.cloud.mediaproc.sample.util.FileUtils;
import com.baidu.cloud.mediaproc.sample.util.ResourceUtil;
import com.baidu.cloud.mediaproc.sample.util.model.VideoInfo;
import com.baidu.cloud.mediaproc.sample.util.rx.RxBusHelper;
import com.baidu.cloud.mediaproc.sample.util.rx.RxNetworking;
import com.baidu.cloud.mediaproc.sample.util.rx.event.FullscreenEvent;
import com.baidu.cloud.mediaproc.sample.widget.video.BDCloudVideoView;
import com.baidu.cloud.mediaproc.sample.widget.video.CustomMediaController;
import com.jakewharton.rxbinding2.support.v4.widget.RxSwipeRefreshLayout;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class ShortVideoActivity extends AppCompatActivity implements RxBusHelper.OnEventListener<FullscreenEvent> {
    private static final String TAG = "ShortVideoActivity";

    private static final int REQUEST_TAKE_GALLERY_VIDEO = 0x23;
    private static final int REQUEST_TAKE_GALLERY_VIDEO_KITKAT = 0x24;

    private ActivityShortvideoBinding binding;

    private List<VideoInfo> infoList = new ArrayList<>();
    private VideoAdapter videoAdapter;

    private BDCloudVideoView bdCloudVideoView;
    private CustomMediaController mediaController;

    private Observable<List<VideoInfo>> loadData, moreData;
    private CompositeDisposable compositeDisposable;

    private BottomSheetBehavior<LinearLayout> behavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_shortvideo);

        new RxPermissions(this)
                .request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(@NonNull Boolean aBoolean) throws Exception {
                        if (!aBoolean) {
                            finish();
                        }
                    }
                });

        bdCloudVideoView = new BDCloudVideoView(this, true);
        bdCloudVideoView.showCacheInfo(false);
        mediaController = new CustomMediaController(this);
        mediaController.setMediaPlayer(bdCloudVideoView);
        mediaController.setAnchorView(bdCloudVideoView);

        videoAdapter = new VideoAdapter(infoList, bdCloudVideoView, mediaController);
        binding.recyclerView.setAdapter(videoAdapter);

        behavior = BottomSheetBehavior.from(binding.designBottomSheet);

        initObservables();
        Disposable disposable = loadData.subscribe(new Consumer<List<VideoInfo>>() {
            @Override
            public void accept(@NonNull List<VideoInfo> videoInfos) throws Exception {
                infoList.addAll(videoInfos);
                videoAdapter.notifyDataSetChanged();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) throws Exception {
                Log.d(TAG, "accept: " + throwable.getMessage());
            }
        });
        compositeDisposable.add(disposable);
        RxBusHelper.doOnMainThread(FullscreenEvent.class, compositeDisposable, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Disposable disposable = moreData.subscribe(new Consumer<List<VideoInfo>>() {
            @Override
            public void accept(@NonNull List<VideoInfo> videoInfos) throws Exception {
                List<VideoInfo> toAdd = new ArrayList<>();
                for (VideoInfo info : videoInfos) {
                    if (!infoList.contains(info)) {
                        toAdd.add(info);
                    }
                }
                infoList.addAll(toAdd);
                videoAdapter.notifyItemRangeInserted(infoList.size(), toAdd.size());
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) throws Exception {
                Log.d(TAG, "OnResume load more data: " + throwable.getMessage());
            }
        });
        compositeDisposable.add(disposable);
        if (bdCloudVideoView != null) {
            bdCloudVideoView.start();
        }
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && null != data) {
            Uri originalUri = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                originalUri = data.getData();
            } else if (requestCode == REQUEST_TAKE_GALLERY_VIDEO_KITKAT) {
                originalUri = data.getData();
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                // Check for the freshest data.
                getContentResolver().takePersistableUriPermission(originalUri, takeFlags);
            }
            Intent intent = new Intent(this, PreviewActivity.class);
            String videoPath = FileUtils.getPath(this, originalUri);
            // FIXME   FileUtils.isAudioTrackProcessable(videoPath)
            if (true) {
                intent.putExtra(ConfigProcessActivity.EXTRA_VIDEO_PATH, videoPath);
                startActivity(intent);
            } else {
                Toast.makeText(this, "视频的音轨不符合，应保持44100的采样率和双声道", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bdCloudVideoView != null) {
            bdCloudVideoView.pause();
        }
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bdCloudVideoView != null) {
            bdCloudVideoView.stopPlayback();
        }
        if (mediaController != null && mediaController.viewModel != null) {
            mediaController.viewModel.onDestroy();
        }
        compositeDisposable.dispose();
        compositeDisposable = null;
    }

    @Override
    public void onBackPressed() {
        if (bdCloudVideoView.getParent() == binding.videoHolder) {
            RxBusHelper.post(new FullscreenEvent(false));
        } else if (behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            super.onBackPressed();
        }
    }

    private void initObservables() {
        compositeDisposable = new CompositeDisposable();
        ObservableTransformer<List<VideoInfo>, List<VideoInfo>> transformer = RxNetworking.bindRefreshing(binding.refreshLayout);

        //// TODO: 17/04/2017 use rxlifecycle to prevent app from memory leak
        loadData = Observable.fromCallable(new Callable<List<VideoInfo>>() {
            @Override
            public List<VideoInfo> call() throws Exception {
                // 模拟后台获取数据
                return getMainSampleData();
            }
        })
                .compose(transformer)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        moreData = Observable.fromCallable(new Callable<List<VideoInfo>>() {
            @Override
            public List<VideoInfo> call() throws Exception {
                // 获取用户已上传的数据
                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(ShortVideoActivity.this);
                Set<String> medias = sharedPreferences.getStringSet("media", new HashSet<String>());
                return ResourceUtil.INSTANCE.getMediaResources(medias);
            }
        })
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        binding.refreshLayout.setRefreshing(false);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        Disposable disposable = RxSwipeRefreshLayout.refreshes(binding.refreshLayout)
                .flatMap(new Function<Object, ObservableSource<List<VideoInfo>>>() {
                    @Override
                    public ObservableSource<List<VideoInfo>> apply(@NonNull Object o) throws Exception {
                        return moreData;
                    }
                })
                .subscribe(new Consumer<List<VideoInfo>>() {
                    @Override
                    public void accept(@NonNull List<VideoInfo> videoInfos) throws Exception {
                        List<VideoInfo> toAdd = new ArrayList<>();
                        for (VideoInfo info : videoInfos) {
                            if (!infoList.contains(info)) {
                                toAdd.add(info);
                            }
                        }
                        infoList.addAll(toAdd);
                        videoAdapter.notifyItemRangeInserted(infoList.size(), toAdd.size());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Log.d(TAG, "swipeRefreshLayout load more data: " + throwable.getMessage());
                    }
                });
        compositeDisposable.add(disposable);
    }

    /**
     * 初次进入应用，SP无数据时，准备样例数据
     *
     * @return
     */
    public static List<VideoInfo> getMainSampleData() {
        List<VideoInfo> list = new ArrayList<>();
        VideoInfo info = new VideoInfo("百度云宣传视频", "http://hi2mjn97mjn2tc40unn.exp"
                + ".bcevod.com/mda-hkzib2fjdgq24cvu/mp41080p/mda-hkzib2fjdgq24cvu.mp4");
        info.imageUrl = "http://hi2mjn97mjn2tc40unn.exp.bcevod.com/mda-hkzib2fjdgq24cvu/mda-hkzib2fjdgq24cvu.jpg";
        info.duration = 110000;
        info.description = "百度云，技术的力量";
        info.stars = 110;
        list.add(info);
        return list;
    }

    public void onClickAddVideo(View view) {
        startActivity(new Intent(this, CaptureActivity.class));
    }

    public void onClickImport(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Intent intent = new Intent();
            intent.setType("video/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "选择视频文件"), REQUEST_TAKE_GALLERY_VIDEO);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
            startActivityForResult(intent, REQUEST_TAKE_GALLERY_VIDEO_KITKAT);
        }
    }

    public void onClickClose(View view) {
        finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    public void showBottomSheet(View view) {
        if (behavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    @Override
    public void onEvent(FullscreenEvent fullscreenEvent) {
        Log.d(TAG, "onEvent: " + fullscreenEvent.isFullscreen());
        if (fullscreenEvent.isFullscreen()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            toggleHideyBar(this);
            videoAdapter.clearVideoView();
            ViewGroup.LayoutParams params = new FrameLayout
                    .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            binding.videoHolder.addView(bdCloudVideoView, params);
        } else {
            toggleHideyBar(this);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            binding.videoHolder.removeView(bdCloudVideoView);
            videoAdapter.addVideoView();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    /**
     * 隐藏或显示：状态栏、系统虚拟按钮栏
     * Detects and toggles immersive mode (also known as "hidey bar" mode).
     * <p>
     * code is from:
     * https://d.android.com/samples/ImmersiveMode/src/com.example.android.immersivemode/ImmersiveModeFragment.html
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void toggleHideyBar(Activity activity) {
        if (Build.VERSION.SDK_INT < 11) {
            return;
        }
        int uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled = ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.i(TAG, "Turning immersive mode mode off. ");
        } else {
            Log.i(TAG, "Turning immersive mode mode on.");
        }

        // Navigation bar hiding: Backwards compatible to ICS.
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }
        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

}
