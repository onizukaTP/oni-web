package com.onizuka.framework.server.middleware.implementations;

import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.server.middleware.Middleware;
import com.onizuka.framework.server.middleware.MiddlewareChain;
import com.onizuka.framework.util.OniLogger;

public class LoggingMiddleware implements Middleware {
    private static final OniLogger log = OniLogger.get(LoggingMiddleware.class);

    @Override
    public HttpResponse handle(HttpRequest request, MiddlewareChain next) {
        long start = System.currentTimeMillis();
        log.info("--> " + request.method + " " + request.path);

        HttpResponse response = next.next(request);

        long duration = System.currentTimeMillis() - start;
        log.info("<-- " + response.status + " (" + duration + " ms)");

        return response;
    }
}
