package com.onizuka.framework.server.middleware.implementations;

import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.server.middleware.Middleware;
import com.onizuka.framework.server.middleware.MiddlewareChain;

public class LoggingMiddleware implements Middleware {
    @Override
    public HttpResponse handle(HttpRequest request, MiddlewareChain next) {
        System.out.println("Incoming request: " + request.path);

        HttpResponse response = next.next(request); // go forward

        System.out.println("Outgoing request: " + response.status);

        return response;
    }
}
