package com.baidu.cloud.mediaproc.sample.util;

import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface BceBosService {

    @Multipart
    @PUT("https://{bucket}.bj.bcebos.com/{key}")
    Call<JsonObject> uploadVideo(@Path("bucket") String bucket, @Path("key") String key,
                                 @Part MultipartBody.Part file);
}