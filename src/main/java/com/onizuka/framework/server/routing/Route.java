package com.onizuka.framework.server.routing;

import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.util.Handler;

public class Route {
    public String method;
    public String pathPattern;
    public Handler handler;

    public Route(String method, String pathPattern, Handler handler) {
        this.method = method;
        this.pathPattern = pathPattern;
        this.handler = handler;
    }

    public Object handle(HttpRequest req) {
        return handler.handle(req);
    }

    @Override
    public String toString() {
        return "Route{" +
                "method='" + method + '\'' +
                ", pathPattern='" + pathPattern + '\'' +
                '}';
    }
}
