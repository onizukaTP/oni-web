package com.onizuka.framework.http;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    public int status;
    public Map<String, String> headers;
    public String body;

    public HttpResponse(int status, String body) {
        this(status, Map.of("Content-Type", "text/plain; charset=UTF-8"), body);
    }

    public HttpResponse(int status, Map<String, String> headers, String body) {
        this.status = status;
        // Always store a mutable copy - callers (and this framework) rely on
        // addHeader() working regardless of how the response was constructed.
        // Previously this held whatever map was passed in directly, so any
        // response built via Map.of(...) - including the single-arg
        // constructor above - would throw UnsupportedOperationException the
        // moment addHeader() was called on it.
        this.headers = new HashMap<>(headers);
        this.body = body;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }
}
