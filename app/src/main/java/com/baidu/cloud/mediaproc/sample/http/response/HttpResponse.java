package com.baidu.cloud.mediaproc.sample.http.response;

/**
 * Created by wenyiming on 22/01/2018.
 */

public class HttpResponse<T> {

    public boolean success;
    public HttpMessage message;
    public T result;

    public static class HttpMessage {
        public String global;
    }
}
