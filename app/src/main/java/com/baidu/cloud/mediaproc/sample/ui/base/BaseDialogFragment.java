package com.baidu.cloud.mediaproc.sample.ui.base;

import android.app.Dialog;
import android.databinding.ObservableBoolean;
import android.databinding.adapters.SeekBarBindingAdapter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;

/**
 * Created by wenyiming on 19/04/2017.
 */

public class BaseDialogFragment extends AppCompatDialogFragment {

    protected ObservableBoolean isSeeking = new ObservableBoolean(false);
    private boolean isProcess = false;

    protected final SeekBarBindingAdapter.OnStartTrackingTouch startTrackingTouch = new SeekBarBindingAdapter.OnStartTrackingTouch() {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeeking.set(true);
        }
    };

    protected final SeekBarBindingAdapter.OnStopTrackingTouch stopTrackingTouch = new SeekBarBindingAdapter.OnStopTrackingTouch() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeeking.set(false);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            isProcess = args.getBoolean("process", false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams wlp = window.getAttributes();
                wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
                wlp.height = WindowManager.LayoutParams.MATCH_PARENT;
                wlp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                window.setAttributes(wlp);
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }
        }
    }
}
