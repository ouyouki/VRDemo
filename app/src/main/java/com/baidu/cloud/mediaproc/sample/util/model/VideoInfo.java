package com.baidu.cloud.mediaproc.sample.util.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.baidubce.services.vod.model.GetMediaResourceResponse;

/**
 * 视频信息类
 *
 * @author baidu
 */
public class VideoInfo implements Parcelable {
    private static final String TAG = "VideoInfo";

    public String title = "";
    public String url = "";
    public String imageUrl = "";
    public boolean like = true;
    public int stars;
    public String description;
    public int duration;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VideoInfo) {
            VideoInfo videoInfo = (VideoInfo) obj;
            return this.title.equals(videoInfo.title) && this.url.equals(videoInfo.url);
        }
        return super.equals(obj);
    }

    public VideoInfo(String title, String url) {
        super();
        this.title = title;
        this.url = url;
    }

    public VideoInfo(GetMediaResourceResponse response) {
        title = response.getAttributes().getTitle();
        url = response.getPlayableUrlList().get(0).getUrl();
        imageUrl = response.getThumbnailList().get(0);
        like = false;
        stars = response.getMeta().getDurationInSeconds().intValue();
        description = response.getAttributes().getDescription();
        duration = (int) (response.getMeta().getDurationInSeconds() * 1000);
        Log.d(TAG, "VideoInfo: " + response);
    }

    protected VideoInfo(Parcel in) {
        title = in.readString();
        url = in.readString();
        imageUrl = in.readString();
        like = in.readByte() != 0;
        stars = in.readInt();
        description = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(url);
        dest.writeString(imageUrl);
        dest.writeByte((byte) (like ? 1 : 0));
        dest.writeInt(stars);
        dest.writeString(description);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VideoInfo> CREATOR = new Creator<VideoInfo>() {
        @Override
        public VideoInfo createFromParcel(Parcel in) {
            return new VideoInfo(in);
        }

        @Override
        public VideoInfo[] newArray(int size) {
            return new VideoInfo[size];
        }
    };
}
