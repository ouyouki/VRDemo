package com.baidu.cloud.mediaproc.sample.http;


import com.baidu.cloud.mediaproc.sample.http.request.GetStatusModel;
import com.baidu.cloud.mediaproc.sample.http.request.PostAnswerModel;
import com.baidu.cloud.mediaproc.sample.http.response.ContestStatus;
import com.baidu.cloud.mediaproc.sample.http.response.HttpResponse;

import io.reactivex.Flowable;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 开源答题项目与后台交互的 api 定义
 * Created by wenyiming on 22/01/2018.
 */

public interface ContestApi {

    @POST("/api/idati/v1/status/detail")
    Flowable<HttpResponse<ContestStatus>> getStatus(@Body GetStatusModel model);

    @POST("/api/idati/v1/question/userAnswer")
    Flowable<HttpResponse> postUserAnswer(@Body PostAnswerModel model);

}
