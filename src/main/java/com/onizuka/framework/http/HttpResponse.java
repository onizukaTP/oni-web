package com.onizuka.framework.http;

import java.util.Map;

public class HttpResponse {
    public int status;
    public Map<String, String> headers;
    public String body;

    public HttpResponse(int status, String body) {
        // Map.of() immutable
        this(status, Map.of("Content-Type", "text/plain; charset=UTF-8"), body);
    }

    public HttpResponse(int status, Map<String, String> headers, String body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }
}
