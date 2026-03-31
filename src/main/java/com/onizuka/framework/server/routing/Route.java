package com.onizuka.framework.server.routing;

public class Route {
    public String method;
    public String pathPattern;
    public String response;

    public Route(String method, String pathPattern, String response) {
        this.method = method;
        this.pathPattern = pathPattern;
        this.response = response;
    }
}
