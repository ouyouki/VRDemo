package com.baidu.cloud.mediaproc.sample.util.model;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.baidu.cloud.mediaproc.sample.util.MusicTool;

/**
 * Created by wenyiming on 20/04/2017.
 */

public class Music implements Parcelable {

    public final long id;
    public final String title;
    public final String artist;
    public final String album;
    public final long duration;
    public final String uri;
    public final long albumId;
    public final String coverUri;
    public final String fileName;
    public final long fileSize;
    public final String year;

    public Music(Context context, Cursor cursor) {
        if (context == null || cursor == null) {
            id = -1;
            title = "无音乐";
            artist = "";
            album = "";
            duration = -1;
            uri = "";
            albumId = -1;
            coverUri = "";
            fileName = "";
            fileSize = 0;
            year = "";
            return;
        }
        id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
        // 标题
        title = cursor.getString((cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
        // 艺术家
        artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
        // 专辑
        album = cursor.getString((cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
        // 持续时间
        duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
        // 音乐uri
        uri = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
        // 专辑封面id，根据该id可以获得专辑图片uri
        albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
        coverUri = MusicTool.getCoverUri(context, albumId);
        // 音乐文件名
        fileName = cursor.getString((cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)));
        // 音乐文件大小
        fileSize = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
        // 发行时间
        year = cursor.getString((cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)));
    }

    protected Music(Parcel in) {
        id = in.readLong();
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        duration = in.readLong();
        uri = in.readString();
        albumId = in.readLong();
        coverUri = in.readString();
        fileName = in.readString();
        fileSize = in.readLong();
        year = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeLong(duration);
        dest.writeString(uri);
        dest.writeLong(albumId);
        dest.writeString(coverUri);
        dest.writeString(fileName);
        dest.writeLong(fileSize);
        dest.writeString(year);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Music> CREATOR = new Creator<Music>() {
        @Override
        public Music createFromParcel(Parcel in) {
            return new Music(in);
        }

        @Override
        public Music[] newArray(int size) {
            return new Music[size];
        }
    };

    @Override
    public String toString() {
        return "Music{"
                + "id=" + id
                + ", title='" + title + '\''
                + ", artist='" + artist + '\''
                + ", album='" + album + '\''
                + ", duration=" + duration
                + ", uri='" + uri + '\''
                + ", albumId=" + albumId
                + ", coverUri='" + coverUri + '\''
                + ", fileName='" + fileName + '\''
                + ", fileSize=" + fileSize
                + ", year='" + year + '\''
                + '}';
    }
}
