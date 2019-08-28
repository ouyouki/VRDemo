package com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter;

import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.ItemMusicListBinding;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnMusicChoseListener;
import com.baidu.cloud.mediaproc.sample.util.model.Music;
import com.bumptech.glide.Glide;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {

    private final List<Music> musics;
    private ItemMusicListBinding lastCheckBinding = null;
    private long lastCheckMusicId = -1;
    private OnMusicChoseListener musicChoseListener;

    public MusicAdapter(List<Music> musics) {
        this.musics = musics;
    }

    public void setLastCheckMusic(@NonNull Music lastCheckMusic) {
        lastCheckMusicId = lastCheckMusic.id;
    }

    public void setMusicChoseListener(OnMusicChoseListener musicChoseListener) {
        this.musicChoseListener = musicChoseListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final Music music = musics.get(position);
        final ItemMusicListBinding binding = holder.binding;
        binding.itemMusicName.setText(music.title);
        Glide.with(binding.imageView.getContext())
                .load(music.coverUri)
                .placeholder(R.drawable.baidu_cloud_bigger)
                .centerCrop()
                .into(binding.imageView);
        if (music.id == lastCheckMusicId) {
            lastCheckBinding = binding;
            binding.getChoose().set(true);
        } else {
            binding.getChoose().set(false);
        }
    }

    @Override
    public int getItemCount() {
        return musics.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ItemMusicListBinding binding;

        public ViewHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);
            binding.setChoose(new ObservableBoolean(false));
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (lastCheckMusicId == musics.get(getAdapterPosition()).id) {
                        return;
                    }
                    if (lastCheckBinding != null)
                        lastCheckBinding.getChoose().set(false);
                    binding.getChoose().set(true);
                    lastCheckMusicId = musics.get(getAdapterPosition()).id;
                    lastCheckBinding = binding;
                    if (musicChoseListener != null) {
                        musicChoseListener.onMusicChose(musics.get(getAdapterPosition()));
                    }
                }
            });
        }
    }

}
