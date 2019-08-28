package com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.cloud.gpuimage.basefilters.GPUImageFilter;
import com.baidu.cloud.gpuimage.graphics.Rotation;
import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnFilterChoseListener;
import com.baidu.cloud.mediaproc.sample.util.gpuimage.GPUImage;
import com.baidu.cloud.stylefilter.AmaroFilter;
import com.baidu.cloud.stylefilter.BrannanFilter;
import com.baidu.cloud.stylefilter.EarlyBirdFilter;
import com.baidu.cloud.stylefilter.HefeFilter;
import com.baidu.cloud.stylefilter.NashvilleFilter;
import com.baidu.cloud.stylefilter.RiseFilter;
import com.baidu.cloud.stylefilter.SierraFilter;
import com.baidu.cloud.stylefilter.ToasterFilter;
import com.baidu.cloud.stylefilter.ValenciaFilter;
import com.baidu.cloud.stylefilter.XproIIFilter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.util.LruCache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by wenyiming on 3/7/17.
 */

public class FiltersAdapter extends RecyclerView.Adapter<FiltersAdapter.ViewHolder> {

    public static final String[] FILTER_NAMES = new String[]{
            "None", "Amaro", "Brannan", "EarlyBird",
            "Hefe", "Nashville", "Rise", "Sierra",
            "Toaster", "Valencia", "XproII"
    };
    private final Context mContext;
    private Uri image;

    private View lastCheckEdge = null;
    private int lastCheckPos = 0;
    private OnFilterChoseListener filterChoseListener;
    private GPUImage mGPUImage;
    private LruCache<String, Bitmap> bitmapLruCache;
    private Map<String, GPUImageFilter> filterMap = new HashMap<>();

    public FiltersAdapter(Context context, Uri image) {
        this.mContext = context;
        // TODO: 19/04/2017  change image to static resources
        this.image = image;
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        final int cacheSize = maxMemory / 16;
        bitmapLruCache = new LruCache<>(cacheSize);
        bitmapLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int getSize(Bitmap item) {
                return item.getByteCount() / 1024;
            }
        };
        loadImage();
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            bitmapLruCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return bitmapLruCache.get(key);
    }

    public void setImage(Uri image) {
        this.image = image;
        loadImage();
    }

    public void setCheckedFilter(String filter) {
        lastCheckPos = Arrays.asList(FILTER_NAMES).indexOf(filter);
    }

    public void setFilterChoseListener(OnFilterChoseListener filterChoseListener) {
        this.filterChoseListener = filterChoseListener;
    }

    private int itemWidth = -1, itemHeight = -1;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            notifyItemChanged(msg.what);
            return false;
        }
    });

    private void loadImage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (String filterName : FILTER_NAMES) {
                    filterMap.put(filterName, getFilterByName(mContext, filterName));
                }
                while (itemWidth == -1 && itemHeight == -1) {
                }
                Bitmap bitmap = null;
                try {
                    bitmap = Glide.with(mContext).load(image)
                            .asBitmap().centerCrop().into(itemWidth, itemHeight).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                if (bitmap == null)
                    return;
                mGPUImage = new GPUImage(mContext);
                mGPUImage.setImage(bitmap);
                for (int i = 0; i < FILTER_NAMES.length; i++) {
                    mGPUImage.setRotation(Rotation.NORMAL, false, true);
                    mGPUImage.setFilter(filterMap.get(FILTER_NAMES[i]));
                    addBitmapToMemoryCache(FILTER_NAMES[i] + image.getSchemeSpecificPart()
                            , mGPUImage.getBitmapWithFilterApplied());
                    mHandler.sendEmptyMessage(i);
                }
            }
        }).start();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_list, parent, false);
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
        String name = "";
        if (position >= 0 && position < FILTER_NAMES.length) {
            name = FILTER_NAMES[position];
        }
        holder.filterName.setText(name);
        Bitmap b = getBitmapFromMemCache(name + image.getSchemeSpecificPart());
        if (b != null)
            holder.preview.setImageBitmap(b);
        if (position == lastCheckPos) {
            lastCheckEdge = holder.edge;
            holder.edge.setVisibility(View.VISIBLE);
        } else {
            holder.edge.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return FILTER_NAMES.length;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView filterName;
        ImageView preview;
        View edge;

        public ViewHolder(View itemView) {
            super(itemView);
            filterName = (TextView) itemView.findViewById(R.id.item_filter_name);
            edge = itemView.findViewById(R.id.item_filter_edge);
            preview = (ImageView) itemView.findViewById(R.id.item_filter_preview);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (lastCheckEdge != null)
                        lastCheckEdge.setVisibility(View.INVISIBLE);
                    edge.setVisibility(View.VISIBLE);
                    lastCheckPos = getAdapterPosition();
                    lastCheckEdge = edge;
                    if (filterChoseListener != null) {
                        if (lastCheckPos >= 0 && lastCheckPos < FILTER_NAMES.length) {
                            filterChoseListener.onFilterChose(FILTER_NAMES[lastCheckPos]);
                        }
                    }
                }
            });
        }
    }


    public static GPUImageFilter getFilterByName(Context context, String name) {
        switch (name) {
            case "Amaro":
                return new AmaroFilter(context);
            case "Brannan":
                return new BrannanFilter(context);
            case "EarlyBird":
                return new EarlyBirdFilter(context);
            case "Hefe":
                return new HefeFilter(context);
            case "Nashville":
                return new NashvilleFilter(context);
            case "Rise":
                return new RiseFilter(context);
            case "Sierra":
                return new SierraFilter(context);
            case "Toaster":
                return new ToasterFilter(context);
            case "Valencia":
                return new ValenciaFilter(context);
            case "XproII":
                return new XproIIFilter(context);
            default:
                return new GPUImageFilter();
        }
    }
}
