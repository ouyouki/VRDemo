package com.baidu.cloud.mediaproc.sample.ui.lss.viewmodel;

import android.content.Intent;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.view.View;

import com.baidu.cloud.mediaproc.sample.ui.base.BaseModel;

/**
 * Created by wenyiming on 02/05/2017.
 */

public class GuestStreamViewModel extends BaseModel {

    public ObservableField<String> mRoomName = new ObservableField<>();
    public ObservableField<String> mRoomNameFirstChar = new ObservableField<>();
    public ObservableField<String> mConversationTime = new ObservableField<>();

    public ObservableInt callVisibility = new ObservableInt(View.INVISIBLE);
    public ObservableBoolean calling = new ObservableBoolean(false);
    public ObservableBoolean waiting = new ObservableBoolean(false);
    public ObservableBoolean timeShowing = new ObservableBoolean(false);

    public GuestStreamViewModel(Intent intent) {
        callVisibility.set(intent.getIntExtra("role", 0) == 1 ? View.INVISIBLE : View.VISIBLE);
        mRoomName.set(intent.getStringExtra("room"));
        mRoomNameFirstChar.set(String.valueOf(intent.getStringExtra("room").charAt(0)));
    }

    @Override
    protected void onResume() {

    }

    @Override
    protected void onPause() {

    }

    @Override
    protected void onDestroy() {

    }


}
