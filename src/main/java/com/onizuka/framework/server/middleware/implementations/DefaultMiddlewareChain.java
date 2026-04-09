package com.onizuka.framework.server.middleware.implementations;

import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.server.middleware.Middleware;
import com.onizuka.framework.server.middleware.MiddlewareChain;

import java.util.List;
import java.util.function.Function;

public class DefaultMiddlewareChain implements MiddlewareChain {
    private List<Middleware> middlewares;
    private int index;
    private Function<HttpRequest, HttpResponse> handler;

    public DefaultMiddlewareChain(List<Middleware> middlewares, Function<HttpRequest, HttpResponse> handler) {
        this.middlewares = middlewares;
        this.handler = handler;
        this.index = 0;
    }

    @Override
    public HttpResponse next(HttpRequest request) {
        if (index < middlewares.size()) {
            System.out.println("Running Middleware: " + index);
            Middleware middleware = middlewares.get(index++);
            return middleware.handle(request, this);
        }
        return handler.apply(request);
    }
}
