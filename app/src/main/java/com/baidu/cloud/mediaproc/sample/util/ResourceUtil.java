package com.baidu.cloud.mediaproc.sample.util;

import com.baidu.cloud.mediaproc.sample.util.model.VideoInfo;
import com.baidubce.BceClientConfiguration;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.auth.DefaultBceSessionCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import com.baidubce.services.vod.VodClient;
import com.baidubce.services.vod.model.GenerateMediaIdResponse;
import com.baidubce.services.vod.model.GetMediaResourceResponse;
import com.baidubce.services.vod.model.ProcessMediaRequest;
import com.baidubce.services.vod.model.ProcessMediaResponse;
import com.baidubce.util.HttpUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by wenyiming on 26/04/2017.
 */

public enum ResourceUtil {

    INSTANCE;

    private VodClient vodClient; // 用于apply，process媒资
    private BosClient bosClient; // 用于文件上传
    private BceBosService bosService;

    ResourceUtil() {
        // tempAk, tempSk, sessionToken are from your servers
        // BOS和VOD公用同一种认证
        String tempAk = "d9ee2dc4f8db11e6a5d27b86934ef082";
        String tempSk = "2f59b7a2106d4a898ca098522a382ceb";
        String sessionToken = "MjUzZjQzNTY4OTE0NDRkNjg3N2E4YzJhZTc4YmU5ZDh8AAAAADgBAADj2BkqcKFrD3kAsKRhxaQHRE+0QWor9sJPDHjU2mJH3ufdywB2og44oMOrRgBGVST28Trwy4jReBu7eHT1f12u6aso/vksTiXkQ/tZ/Z8/SULrt0H34ehGnK3R41woEKmaCTH2vEkSBxxJVDFmQeMopphpfof7xvnjuouWXQFn8/hY6P40lsAzjQtk2SGfBLhBugWIDuL7ZNeiaEhT7MOBtj/LyP39dp684YMYWBTBhooATQa+FTEvBYCAXFRKWhU=";
        DefaultBceSessionCredentials stsCredentials =
                new DefaultBceSessionCredentials(
                        tempAk,
                        tempSk,
                        sessionToken);

//        // 不推荐 DefaultBceCredentials. 因为ak,sk 泄漏后风险非常大。请使用上面的 DefaultBceSessionCredentials
//        DefaultBceCredentials stsCredentials = new DefaultBceCredentials(ResourceUtilAkSk.VodAK,
//                ResourceUtilAkSk.VodSK);
//        BceClientConfiguration vodConfig = new BceClientConfiguration();
//        vodConfig.withCredentials(stsCredentials);
//
//        BosClientConfiguration bosConfig = new BosClientConfiguration();
//        bosConfig.withCredentials(stsCredentials);

//        vodClient = new VodClient(vodConfig);
//        bosClient = new BosClient(bosConfig);

        // TODO: 03/05/2017 后面准备使用 bos 的 restapi上传 https://cloud.baidu.com/doc/BOS/API.html#PutObject.E6.8E.A5.E5.8F.A3
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        bosService = retrofit.create(BceBosService.class);
    }

    public Flowable<ProcessMediaResponse> applyUploadAndProcess(final File file) {
        return Flowable.fromCallable(new Callable<GenerateMediaIdResponse>() {
            @Override
            public GenerateMediaIdResponse call() throws Exception {
                String mode = "no_transcoding";
                return vodClient.applyMediaForSpecificMode(mode);
//                return vodClient.applyMedia();
            }
        })
                .subscribeOn(Schedulers.io())
                .map(new Function<GenerateMediaIdResponse, ProcessMediaResponse>() {
                    @Override
                    public ProcessMediaResponse apply(@NonNull GenerateMediaIdResponse generateMediaIdResponse) throws Exception {
                        String bosKey = generateMediaIdResponse.getSourceKey();
                        String bucket = generateMediaIdResponse.getSourceBucket();

                        FileUploadSession session = new FileUploadSession(bosClient);
                        if (!session.upload(file, bucket, bosKey)) {
                            throw new RuntimeException("上传失败");
                        }
                        ProcessMediaRequest request =
                                new ProcessMediaRequest()
                                        .withMediaId(generateMediaIdResponse.getMediaId())
                                        .withTitle(file.getName())
                                        .withDescription("来自用户的上传视频")
                                        .withSourceExtension(getFileExtension(file))
                                        .withTranscodingPresetGroupName("vod.inbuilt.adaptive.hls");
                        // process: let vod to process bos file
                        return vodClient.processMedia(request);
                    }
                });
    }

    public List<VideoInfo> getMediaResources(Set<String> medias) {
        List<VideoInfo> videoInfos = new ArrayList<>();
        for (String mediaId : medias) {
            GetMediaResourceResponse response = vodClient.getMediaResource(mediaId);
            if (response.getStatus().equals("PUBLISHED")) {
                videoInfos.add(new VideoInfo(response));
            }
        }
        return videoInfos;
    }

    public VideoInfo getMediaResources(String mediaId) {
        GetMediaResourceResponse response = vodClient.getMediaResource(mediaId);
        if (response.getStatus().equals("PUBLISHED")) {
            return new VideoInfo(response);
        } else {
            return null;
        }
    }

    private String getFileExtension(@NonNull File file) {
        String filename = file.getName();
        if (filename.lastIndexOf(".") != -1) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        }
        return null;
    }

    private static String generateAuthorization(String method, String path) {
        String signString = method + "\n" + getCanonicalURIPath(path);
        return signString;
    }

    private static String getCanonicalURIPath(String path) {
        if (path == null) {
            return "/";
        } else if (path.startsWith("/")) {
            return HttpUtils.normalizePath(path);
        } else {
            return "/" + HttpUtils.normalizePath(path);
        }
    }

}
