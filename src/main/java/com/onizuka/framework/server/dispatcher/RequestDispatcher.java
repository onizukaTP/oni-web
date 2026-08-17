package com.onizuka.framework.server.dispatcher;

import com.onizuka.framework.exception.NotFoundException;
import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.server.StaticFileHandler;
import com.onizuka.framework.server.routing.Route;
import com.onizuka.framework.server.routing.RouteRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestDispatcher {

    private final RouteRegistry routeRegistry;
    private final StaticFileHandler staticFileHandler;

    public RequestDispatcher(RouteRegistry routeRegistry, StaticFileHandler staticFileHandler) {
        this.routeRegistry = routeRegistry;
        this.staticFileHandler = staticFileHandler;
    }

    public HttpResponse handle(HttpRequest request) {
        if ("GET".equalsIgnoreCase(request.method) && staticFileHandler != null) {
            HttpResponse staticRes = staticFileHandler.handle(request);
            if (staticRes != null) return staticRes;
        }

        Route route = match(request);
        if (route == null) {
            throw new NotFoundException("No route matched path: " + request.path);
        }

        return route.handler.handle(request);
    }

    public Route match(HttpRequest request) {
        List<Route> routes = routeRegistry.getRoutes();
        String[] requestParts = request.path.substring(1).split("/");

        for (Route route : routes) {
            if (!route.method.equalsIgnoreCase(request.method)) continue;

            String[] patternParts = route.pathPattern.substring(1).split("/");
            if (patternParts.length != requestParts.length) continue;

            Map<String, String> params = new HashMap<>();
            boolean match = true;

            for (int i = 0; i < patternParts.length; i++) {
                String pattern = patternParts[i];
                String actual = requestParts[i];

                if (pattern.startsWith("{") && pattern.endsWith("}")) {
                    String key = pattern.substring(1, pattern.length() - 1);
                    params.put(key, actual);
                } else if (!pattern.equals(actual)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                request.pathParams = params;
                return route;
            }
        }
        return null;
    }
}
