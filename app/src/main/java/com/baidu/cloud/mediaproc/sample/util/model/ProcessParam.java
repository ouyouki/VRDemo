package com.baidu.cloud.mediaproc.sample.util.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter.FiltersAdapter;

/**
 * Created by wenyiming on 25/04/2017.
 */

public class ProcessParam implements Parcelable {

    public long bgmStart;
    public long bgmInterval;
    public float bgmTrackGain;
    public String bgmUri;
    public float pinkLevel;
    public float brightLevel;
    public float smoothLevel;
    public String customFilter;
    public float sharpness;
    public float hue;
    public float saturation;
    public float contrast;
    public float brightness;
    public String mediaFilePath;
    public int playbackRate;

    public ProcessParam() {
        bgmTrackGain = 1.0f;
        brightness = 0.0f;
        contrast = 1.0f;
        saturation = 1.0f;
        hue = 0.0f;
        sharpness = 0.0f;
        customFilter = FiltersAdapter.FILTER_NAMES[0];
        smoothLevel = 0.0f;
        brightLevel = 0.0f;
        pinkLevel = 0.0f;
        playbackRate = 1;
    }


    protected ProcessParam(Parcel in) {
        bgmStart = in.readLong();
        bgmInterval = in.readLong();
        bgmTrackGain = in.readFloat();
        bgmUri = in.readString();
        pinkLevel = in.readFloat();
        brightLevel = in.readFloat();
        smoothLevel = in.readFloat();
        customFilter = in.readString();
        sharpness = in.readFloat();
        hue = in.readFloat();
        saturation = in.readFloat();
        contrast = in.readFloat();
        brightness = in.readFloat();
        mediaFilePath = in.readString();
        playbackRate = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(bgmStart);
        dest.writeLong(bgmInterval);
        dest.writeFloat(bgmTrackGain);
        dest.writeString(bgmUri);
        dest.writeFloat(pinkLevel);
        dest.writeFloat(brightLevel);
        dest.writeFloat(smoothLevel);
        dest.writeString(customFilter);
        dest.writeFloat(sharpness);
        dest.writeFloat(hue);
        dest.writeFloat(saturation);
        dest.writeFloat(contrast);
        dest.writeFloat(brightness);
        dest.writeString(mediaFilePath);
        dest.writeInt(playbackRate);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ProcessParam> CREATOR = new Creator<ProcessParam>() {
        @Override
        public ProcessParam createFromParcel(Parcel in) {
            return new ProcessParam(in);
        }

        @Override
        public ProcessParam[] newArray(int size) {
            return new ProcessParam[size];
        }
    };

    @Override
    public String toString() {
        return "ProcessParam{" +
                "bgmStart=" + bgmStart +
                ", bgmInterval=" + bgmInterval +
                ", bgmTrackGain=" + bgmTrackGain +
                ", bgmUri='" + bgmUri + '\'' +
                ", pinkLevel=" + pinkLevel +
                ", brightLevel=" + brightLevel +
                ", smoothLevel=" + smoothLevel +
                ", customFilter='" + customFilter + '\'' +
                ", sharpness=" + sharpness +
                ", hue=" + hue +
                ", saturation=" + saturation +
                ", contrast=" + contrast +
                ", brightness=" + brightness +
                ", mediaFilePath='" + mediaFilePath + '\'' +
                ", playbackRate=" + playbackRate +
                '}';
    }
}
