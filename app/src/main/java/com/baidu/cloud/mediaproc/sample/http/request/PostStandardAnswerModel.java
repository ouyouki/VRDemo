package com.baidu.cloud.mediaproc.sample.http.request;


public class PostStandardAnswerModel {
    /**
     * playDomain : play.test.com
     * app : testapp
     * stream : teststream
     * answer : {"topic":"who are you?","option":"B. you"}
     */

    public String playDomain;
    public String app;
    public String stream;
    public PostAnswerModel.Answer answer;
}
