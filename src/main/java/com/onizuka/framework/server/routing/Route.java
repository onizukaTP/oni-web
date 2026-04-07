package com.onizuka.framework.server.routing;

import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;

import java.util.function.Function;

public class Route {
    public String method;
    public String pathPattern;
    public Function<HttpRequest, HttpResponse> handler;

    public Route(String method, String pathPattern, Function<HttpRequest, HttpResponse> handler) {
        this.method = method;
        this.pathPattern = pathPattern;
        this.handler = handler;
    }

    @Override
    public String toString() {
        return "Route{" +
                "method='" + method + '\'' +
                ", pathPattern='" + pathPattern + '\'' +
                '}';
    }
}
