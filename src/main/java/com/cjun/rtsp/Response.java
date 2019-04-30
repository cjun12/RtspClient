package com.cjun.rtsp;

import java.util.HashMap;
import java.util.Map;

public class Response {
    private Map<String, String> headers = new HashMap<>();
    private byte[] content;
    private int statusCode;
    private int statusMessage;
    private long contentLength;

    Response() {
    }
}
