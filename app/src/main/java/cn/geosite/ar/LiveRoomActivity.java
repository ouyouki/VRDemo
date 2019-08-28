package cn.geosite.ar;

import android.Manifest;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityLiveRoomBinding;
import com.baidu.cloud.mediaproc.sample.ui.lss.StreamingActivity;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

public class LiveRoomActivity extends AppCompatActivity {

    private static final String TAG = "LiveRoomActivity";

    public static int OVERLAY_PERMISSION_REQ_CODE = 101;

    private ActivityLiveRoomBinding binding;

    private static final String PUSH_URL = "rtmp://push.wicd.info/ar/input/";
    private static final String PLAY_URL = "rtmp://play.wicd.info/ar/input/fast";
    private static final String PLAY_URL_MERGE = "rtmp://play.wicd.info/ar/input/";
    private static final String ROOM = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_live_room);
        //setSupportActionBar(binding.toolbar);
        //setSupportActionBar(binding.toolbar);

        new RxPermissions(this)
                .request(Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(@NonNull Boolean aBoolean) throws Exception {
                        if (!aBoolean) {
                            finish();
                        }
                    }
                });

        if (getSupportActionBar() != null) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    // 开始推流
    public void onClickPush(View view) {
        Intent i = new Intent(LiveRoomActivity.this, StreamingActivity.class);
        i.putExtra("role", 1);
        i.putExtra("room", ROOM);
        // 大主播连麦时看的一直是小主播的低延时画面
        i.putExtra("url_play", PLAY_URL);
        i.putExtra("url_push", PUSH_URL);
        startActivity(i);
    }

    // 停止推流
    public void onClickClose(View view) {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "权限授予失败，无法开启悬浮窗", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 34:
                // 点击
                Toast.makeText(this, "推流开始", Toast.LENGTH_SHORT).show();
                binding.btnPush.performClick();
                break;
        }
        return super.onKeyUp(keyCode, event);
    }
}