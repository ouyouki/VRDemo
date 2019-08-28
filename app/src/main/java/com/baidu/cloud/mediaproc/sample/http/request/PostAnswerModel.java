package com.baidu.cloud.mediaproc.sample.http.request;


public class PostAnswerModel {

    /**
     * playDomain : play.test.com
     * app : testapp
     * stream : teststream
     * answer : {"topic":"who are you?","option":"A. me"}
     */

    public String playDomain;
    public String app;
    public String stream;
    public Answer answer;

    public static class Answer {
        /**
         * topic : who are you?
         * option : A. me
         */

        public String topic;
        public String option;
    }
}
