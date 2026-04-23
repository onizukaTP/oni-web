package com.onizuka.framework.http;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    public String method;
    public String path;
    public String version;
    public Map<String, String> headers;
    public Map<String, String> pathParams;
    public Map<String, String> queryParams;

    public HttpRequest(String method, String path, String version, Map<String, String> headers, Map<String, String> pathParams, Map<String, String> queryParams) {
        this.method = method;
        this.path = path;
        this.version = version;
        this.headers = headers;
        this.pathParams = pathParams;
        this.queryParams = queryParams;
    }

    public String getPathParam(String id) {
        return pathParams.get(id);
    }

    public String getQueryParam(String name) {
        return queryParams.get(name);
    }
}
