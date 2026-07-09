package com.onizuka.framework.http;

import java.util.Map;

public class HttpRequest {
    public String method;
    public String path;
    public String version;
    public Map<String, String> headers;
    public Map<String, String> pathParams;
    public Map<String, String> queryParams;
    public String body = "";

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

    /**
     * Case-insensitive header lookup. HTTP header names are case-insensitive
     * per RFC 7230, but headers are stored here exactly as received, so a
     * plain headers.get("content-length") would miss "Content-Length".
     */
    public String getHeader(String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
