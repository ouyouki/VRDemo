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
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter.FiltersAdapter;
import com.baidu.cloud.mediaproc.sample.databinding.DialogCustomFilterBinding;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnFilterChoseListener;


public class FilterFragment extends Fragment {

    private DialogCustomFilterBinding binding;
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
        binding.dialogFilterList.setAdapter(filtersAdapter);
        super.onViewCreated(view, savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_custom_filter, container, false);
        return v;
    }

}
