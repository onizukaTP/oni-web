package com.onizuka.framework.server.middleware.implementations;

import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.server.middleware.Middleware;
import com.onizuka.framework.server.middleware.MiddlewareChain;

public class CorsMiddleware implements Middleware {

    private final String allowedOrigins;
    private final String allowedMethods;
    private final String allowedHeaders;

    public CorsMiddleware(String allowedOrigins, String allowedMethods, String allowedHeaders) {
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
    }

    public CorsMiddleware() {
        this("*", "GET, POST, PUT, DELETE, PATCH, OPTIONS", "Content-Type, Authorization");
    }

    @Override
    public HttpResponse handle(HttpRequest request, MiddlewareChain next) {
        if ("OPTIONS".equalsIgnoreCase(request.method)) {
            HttpResponse res = new HttpResponse(204, "");
            addCorsHeaders(res);
            return res;
        }

        HttpResponse res = next.next(request);
        addCorsHeaders(res);
        return res;
    }

    private void addCorsHeaders(HttpResponse response) {
        response.addHeader("Access-Control-Allow-Origin", allowedOrigins);
        response.addHeader("Access-Control-Allow-Methods", allowedMethods);
        response.addHeader("Access-Control-Allow-Headers", allowedHeaders);
    }
}
