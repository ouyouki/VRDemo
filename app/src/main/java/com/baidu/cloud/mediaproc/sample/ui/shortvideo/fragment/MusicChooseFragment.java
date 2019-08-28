package com.baidu.cloud.mediaproc.sample.ui.shortvideo.fragment;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter.MusicAdapter;
import com.baidu.cloud.mediaproc.sample.databinding.LayoutMusicChooseBinding;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnMusicChoseListener;
import com.baidu.cloud.mediaproc.sample.util.MusicTool;
import com.baidu.cloud.mediaproc.sample.util.model.Music;
import com.baidu.cloud.mediaproc.sample.widget.SpacesItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MusicChooseFragment extends Fragment {

    private LayoutMusicChooseBinding binding;
    private List<Music> musics = new ArrayList<>();
    private MusicAdapter musicAdapter;
    private OnMusicChoseListener musicChoseListener;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        if (getActivity() instanceof OnMusicChoseListener) {
            musicChoseListener = (OnMusicChoseListener) getActivity();
        }
        if (musicAdapter == null) {
            musics.add(new Music(null, null));
            musicAdapter = new MusicAdapter(musics);
            Flowable.fromCallable(new Callable<List<Music>>() {
                @Override
                public List<Music> call() throws Exception {
                    return MusicTool.scanMusic(context);
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<List<Music>>() {
                        @Override
                        public void accept(@NonNull List<Music> musics) throws Exception {
                            MusicChooseFragment.this.musics.addAll(musics);
                            musicAdapter.notifyDataSetChanged();
                        }
                    });
            musicAdapter.setMusicChoseListener(musicChoseListener);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null && musicAdapter != null) {
            Music music = args.getParcelable("music");
            if (music != null) {
                musicAdapter.setLastCheckMusic(music);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_music_choose, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = DataBindingUtil.bind(view);
        int spacingInPixels = getResources().getDimensionPixelOffset(R.dimen.music_list_spacing);
        binding.recyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
        binding.recyclerView.setAdapter(musicAdapter);
        if (getParentFragment() instanceof View.OnClickListener) {
            binding.button.setOnClickListener((View.OnClickListener) getParentFragment());
        }
    }
}

   