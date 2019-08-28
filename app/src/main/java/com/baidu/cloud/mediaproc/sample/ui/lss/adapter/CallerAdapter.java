package com.baidu.cloud.mediaproc.sample.ui.lss.adapter;

import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.baidu.cloud.mediaproc.sample.R;

/**
 * 用于显示呼叫人列表
 */
public class CallerAdapter extends RecyclerView.Adapter<CallerAdapter.ViewHolder> {

    private static final String[] AB = new String[]{"A", "B"};
    private final ArrayMap<String, String> callers;

    private ViewHolder lastCheckHolder = null;

    private int lastCheckPos = -1;
    private OnCallerListener onCallerListener;

    public CallerAdapter(ArrayMap<String, String> callers, OnCallerListener onCallerListener) {
        this.callers = callers;
        this.onCallerListener = onCallerListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_caller_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.guestName.setText(AB[position % AB.length]);
        if (position == lastCheckPos) {
            lastCheckHolder = holder;
            holder.edge.setVisibility(View.VISIBLE);
        } else {
            holder.edge.setVisibility(View.INVISIBLE);
        }
        // 确保当前至少有一个项目被选中并且选中的项目合法
        if (lastCheckPos == -1 || lastCheckPos >= getItemCount()) {
            holder.itemView.performClick();
        }
    }

    @Override
    public int getItemCount() {
        return callers.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView guestName;
        View edge;

        public ViewHolder(View itemView) {
            super(itemView);
            guestName = (TextView) itemView.findViewById(R.id.text_guest_name);
            edge = itemView.findViewById(R.id.item_filter_edge);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (lastCheckPos == getAdapterPosition()) {
                        return;
                    }
                    if (lastCheckHolder != null) {
                        lastCheckHolder.edge.setVisibility(View.INVISIBLE);
                    }
                    edge.setVisibility(View.VISIBLE);
                    lastCheckPos = getAdapterPosition();
                    lastCheckHolder = ViewHolder.this;
                    if (onCallerListener != null) {
                        onCallerListener.onSelectCaller(callers.keyAt(lastCheckPos), guestName.getText().toString());
                    }
                }
            });
        }
    }

    public int getLastCheckPos() {
        return lastCheckPos;
    }

    public interface OnCallerListener {
        void onSelectCaller(String userId, String character);
    }

}
