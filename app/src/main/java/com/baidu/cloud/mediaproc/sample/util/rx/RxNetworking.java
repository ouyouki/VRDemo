package com.baidu.cloud.mediaproc.sample.util.rx;

import android.support.v4.widget.SwipeRefreshLayout;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;


public class RxNetworking {

    public static <T> ObservableTransformer<T, T> bindRefreshing(final SwipeRefreshLayout indicator) {

        return new ObservableTransformer<T, T>() {
            @Override
            public ObservableSource<T> apply(@NonNull Observable<T> upstream) {
                return upstream
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(@NonNull Disposable disposable) throws Exception {
                                indicator.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        indicator.setRefreshing(true);
                                    }
                                });
                            }
                        })
                        .doOnError(new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                indicator.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        indicator.setRefreshing(false);
                                    }
                                });
                            }
                        })
                        .doOnComplete(new Action() {
                            @Override
                            public void run() throws Exception {
                                indicator.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        indicator.setRefreshing(false);
                                    }
                                });
                            }
                        });
            }
        };
    }


}