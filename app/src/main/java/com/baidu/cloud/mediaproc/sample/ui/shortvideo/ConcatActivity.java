package com.baidu.cloud.mediaproc.sample.ui.shortvideo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediastream.config.ProcessConfig;
import com.baidu.cloud.mediastream.listener.ProcessStateListener;
import com.baidu.cloud.mediastream.session.MediaConcatSession;
import com.baidu.cloud.mediastream.session.MediaQuickConcatSession;

import java.util.ArrayList;
import java.util.List;

public class ConcatActivity extends Activity {
    private static final String TAG = "ConcatActivity";

    private boolean isUsingQuickConcat = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_concat);
        Button btnConcat = (Button) findViewById(R.id.btnStart);
        btnConcat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread() {
                    @Override
                    public void run() {
                        final long startTimeInMSec = System.currentTimeMillis();
                        Log.d(TAG, "start Concat");

                        if (isUsingQuickConcat) {
                            /******************** MediaQuickConcatSession !合并极快! *******************/

                            // 当输入视频之间的编码参数完全一致时，才可以使用MediaQuickConcatSession
                            MediaQuickConcatSession quickConcatSession = new MediaQuickConcatSession();
                            List<String> localFilePathArrayList = new ArrayList<>();
                            for (int i = 0; i < 3; ++i) {
                                localFilePathArrayList.add("/sdcard/5808.mp4");
                            }
                            // concat速度比较快，采用同步调用的方式处理，及时返回结果
                            final boolean success = quickConcatSession.concat(localFilePathArrayList,
                                    "/sdcard/quickConcat" + System.currentTimeMillis() + ".mp4");
                            Log.d(TAG, "Concat over");
                            toastResult("QuickConcat success?=" + success + ";timeConsume="
                                    + (System.currentTimeMillis() - startTimeInMSec) + "ms");
                        } else {
                            /******************** MediaConcatSession ********************************/
                            ProcessStateListener listener =
                                    new ProcessStateListener() {
                                @Override
                                public void onProgress(int progress) {
                                    Log.d(TAG, "Concat progress=" + progress);
                                }

                                @Override
                                public void onFinish(boolean isSuccess, int what) {
                                    Log.d(TAG, "Concat over");
                                    toastResult("NormalConcat success?=" + isSuccess + ";timeConsume="
                                            + (System.currentTimeMillis() - startTimeInMSec) + "ms");
                                }
                            };
                            // start concat
                            ProcessConfig config = new ProcessConfig.Builder()
                                    .setVideoWidth(480)
                                    .setVideoHeight(360)
                                    .setInitVideoBitrate(800 * 1024)
                                    .build();
                            MediaConcatSession concatSession = new MediaConcatSession(ConcatActivity.this, config);
                            concatSession.setProcessStateListener(listener);
                            // config output path
                            concatSession.configMp4Saver(true, "/sdcard/media-concat-"
                                    + System.currentTimeMillis() + ".mp4");
                            // config input list
                            List<MediaConcatSession.ConcatMp4File> inputList = new ArrayList<>();
                            for (int i = 0; i < 3; ++i) {
                                inputList.add(new MediaConcatSession.ConcatMp4File("/sdcard/5808.mp4"));
                            }
//                            inputList.add(new MediaConcatSession.ConcatMp4File("/sdcard/festival.mp4"));

                            concatSession.setSrcFileList(inputList);
                            concatSession.start();
                        }
                    }
                }.start();
            }
        });
    }

    public void toastResult(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ConcatActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
