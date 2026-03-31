package com.onizuka.framework.server.routing;

import com.onizuka.framework.http.HttpRequest;

import java.util.function.Function;

public class Route {
    public String method;
    public String pathPattern;
    public Function<HttpRequest, String> handler;

    public Route(String method, String pathPattern, Function<HttpRequest, String> handler) {
        this.method = method;
        this.pathPattern = pathPattern;
        this.handler = handler;
    }
}
