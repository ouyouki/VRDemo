package com.baidu.cloud.mediaproc.sample.widget;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private int space;

    public SpacesItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent, RecyclerView.State state) {
        int index = parent.getChildAdapterPosition(view);
        if (index != 0 && index != parent.getAdapter().getItemCount() - 1) {
            outRect.left = space;
        } else {
            outRect.left = 2 * space;
        }
    }
}