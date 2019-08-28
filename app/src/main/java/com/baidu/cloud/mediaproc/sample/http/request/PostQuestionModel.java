package com.baidu.cloud.mediaproc.sample.http.request;

import java.util.List;

/**
 * Created by wenyiming on 22/01/2018.
 */

public class PostQuestionModel {


    /**
     * playDomain : play.test.com
     * app : testapp
     * stream : teststream
     * question : {"topic":"who are you?","options":["A. me","B. you","C. him","D. she"]}
     */

    public String playDomain;
    public String app;
    public String stream;
    public Question question;

    public static class Question {
        /**
         * topic : who are you?
         * options : ["A. me","B. you","C. him","D. she"]
         */

        public String topic;
        public List<String> options;
    }
}
