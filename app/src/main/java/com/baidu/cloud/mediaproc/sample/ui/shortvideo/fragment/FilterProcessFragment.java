package com.baidu.cloud.mediaproc.sample.ui.shortvideo.fragment;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.LayoutFilterProcessBinding;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter.FiltersAdapter;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnFilterChoseListener;


public class FilterProcessFragment extends Fragment {

    private LayoutFilterProcessBinding binding;
    private FiltersAdapter filtersAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (filtersAdapter == null) {
            filtersAdapter = new FiltersAdapter(context,
                    Uri.parse("android.resource://com.baidu.cloud.mediaproc.sample/" + R.drawable.lena));
        }
        if (getActivity() instanceof OnFilterChoseListener) {
            filtersAdapter.setFilterChoseListener((OnFilterChoseListener) getActivity());
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.bind(view);
        if (getArguments() != null) {
            filtersAdapter.setCheckedFilter(getArguments().getString("filter",
                    FiltersAdapter.FILTER_NAMES[0]));
        }
        binding.dialogFilterList.setAdapter(filtersAdapter);
        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });
        binding.dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });
        super.onViewCreated(view, savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.layout_filter_process, container, false);
        return v;
    }

}
