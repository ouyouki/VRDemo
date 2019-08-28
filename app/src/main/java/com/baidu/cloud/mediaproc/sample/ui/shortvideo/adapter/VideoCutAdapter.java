package com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.util.FileUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.FileDescriptorBitmapDecoder;
import com.bumptech.glide.load.resource.bitmap.VideoBitmapDecoder;
import com.bumptech.glide.request.target.Target;


public class VideoCutAdapter extends RecyclerView.Adapter<VideoCutAdapter.ViewHolder> {


    private final Context mContext;
    private final String videoPath;
    private final BitmapPool bitmapPool;
    private final long duration;

    public VideoCutAdapter(Context context, String videoPath) {
        this.mContext = context;
        this.videoPath = videoPath;
        this.duration = FileUtils.getDurationOfVideoInUs(videoPath);
        bitmapPool = Glide.get(context.getApplicationContext()).getBitmapPool();
    }

    private int itemWidth = -1, itemHeight = -1;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_frame_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onViewAttachedToWindow(final ViewHolder holder) {
        holder.preview.post(new Runnable() {
            @Override
            public void run() {
                itemHeight = holder.preview.getHeight();
                itemWidth = holder.preview.getWidth();
            }
        });
        super.onViewAttachedToWindow(holder);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        int microSecond = position * 2500000;// 6th second as an example
        VideoBitmapDecoder videoBitmapDecoder = new VideoBitmapDecoder(microSecond);
        FileDescriptorBitmapDecoder fileDescriptorBitmapDecoder =
                new FileDescriptorBitmapDecoder(videoBitmapDecoder,
                        bitmapPool, DecodeFormat.PREFER_ARGB_8888);
        Glide.with(mContext.getApplicationContext())
                .load(videoPath)
                .asBitmap()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .override(itemWidth == -1 ? Target.SIZE_ORIGINAL : itemWidth,
                        itemHeight == -1 ? Target.SIZE_ORIGINAL : itemHeight)
                .videoDecoder(fileDescriptorBitmapDecoder)
                .into(holder.preview);
    }

    @Override
    public int getItemCount() {
        return (int) (duration / 2500000);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView preview;

        ViewHolder(View itemView) {
            super(itemView);
            preview = (ImageView) itemView.findViewById(R.id.imageView);
        }
    }
}
