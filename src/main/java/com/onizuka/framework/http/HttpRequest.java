package com.onizuka.framework.http;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    public String method;
    public String path;
    public String version;
    public Map<String, String> headers;
    public Map<String, String> pathParams = new HashMap<>();

    public HttpRequest(String method, String path, String version, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.version = version;
        this.headers = headers;
    }
}
