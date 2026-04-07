package com.onizuka.framework.http;

import java.util.Map;

public class HttpResponse {
    public int status;
    public Map<String, String> header;
    public String body;

    public HttpResponse(int status, String body) {
        // Map.of() immutable
        this(status, Map.of("Content-Type", "text/plain; charset=UTF-8"), body);
    }

    public HttpResponse(int status, Map<String, String> header, String body) {
        this.status = status;
        this.header = header;
        this.body = body;
    }

    public void addHeader(String key, String value) {
        header.put(key, value);
    }
}
