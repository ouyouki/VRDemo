package com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter;

import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.ItemVideoListBinding;
import com.baidu.cloud.mediaproc.sample.util.model.VideoInfo;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.viewmodel.VideoListViewModel;
import com.baidu.cloud.mediaproc.sample.widget.video.BDCloudVideoView;
import com.baidu.cloud.mediaproc.sample.widget.video.CustomMediaController;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/**
 * Created by wenyiming on 12/04/2017.
 */

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoHolder> {
    private static final String TAG = "VideoAdapter";
    private static final int VIEW_TYPE_LAST = 1;
    private static final int VIEW_TYPE_NORMAL = 2;
    private final CustomMediaController mediaController;
    private List<VideoInfo> infoList;
    private BDCloudVideoView bdCloudVideoView;
    private FrameLayout lastHolder;
    private VideoListViewModel lastModel;

    public VideoAdapter(List<VideoInfo> videoInfos, BDCloudVideoView videoView,
                        CustomMediaController controller) {
        infoList = videoInfos;
        bdCloudVideoView = videoView;
        mediaController = controller;
    }

    @Override
    public VideoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_NORMAL) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_video_list, parent, false);
            return new VideoHolder(v, false);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_video_list_last, parent, false);
            return new VideoHolder(v, true);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1) {
            return VIEW_TYPE_LAST;
        } else {
            return VIEW_TYPE_NORMAL;
        }
    }

    @Override
    public void onBindViewHolder(final VideoHolder holder, final int position) {
        if (getItemViewType(position) == VIEW_TYPE_LAST) {
            return;
        }
        final VideoInfo info = infoList.get(position);
        final ItemVideoListBinding binding = holder.binding;
        Glide.with(holder.itemView.getContext()).load(R.drawable.baidu_cloud_bigger)
                .asBitmap()
                .centerCrop()
                .into(new BitmapImageViewTarget(binding.itemVideoAvatar) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory
                                .create(binding.itemVideoAvatar.getContext().getResources(), resource);
                        circularBitmapDrawable.setCircular(true);
                        binding.itemVideoAvatar.setImageDrawable(circularBitmapDrawable);
                    }
                });
        Glide.with(holder.itemView.getContext())
                .load(info.imageUrl)
                .placeholder(R.mipmap.background_lss)
                .fitCenter()
                .into(binding.itemVideoPreviewImage);
        binding.itemVideoLikes.setText(info.stars + "");
        binding.itemVideoTitle.setText(info.title);
        holder.viewModel.setVideoInfo(info);
        RxView.clicks(binding.itemVideoLoad)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        if (binding.itemVideoHolder.getChildCount() == 0) {
                            if (lastHolder != null) {
                                lastHolder.removeAllViews();
                                lastModel.dirty.set(false);
                            }
                            lastHolder = binding.itemVideoHolder;
                            lastModel = holder.viewModel;

                            addVideoView();
                            mediaController.setVideoInfo(info);
                        }
                    }
                });
    }

    public void clearVideoView() {
        if (lastHolder != null) {
            lastHolder.removeAllViews();
            lastModel.dirty.set(false);
        }
    }

    public void addVideoView() {
        if (lastHolder != null && lastHolder.getChildCount() == 0) {
            ViewGroup.LayoutParams params = new FrameLayout
                    .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            lastHolder.addView(bdCloudVideoView, params);
            lastModel.dirty.set(true);
        }
    }

    @Override
    public void onViewDetachedFromWindow(final VideoHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder.binding != null && lastHolder == holder.binding.itemVideoHolder) {
            lastHolder.removeAllViews();
            lastModel.dirty.set(false);
        }
    }

    @Override
    public int getItemCount() {
        return infoList.size() + 1;
    }

    static class VideoHolder extends RecyclerView.ViewHolder {

        ItemVideoListBinding binding;
        VideoListViewModel viewModel;

        VideoHolder(View itemView, boolean last) {
            super(itemView);
            if (!last) {
                binding = DataBindingUtil.bind(itemView);
                viewModel = new VideoListViewModel();
                binding.setModel(viewModel);
            }
        }
    }

}
