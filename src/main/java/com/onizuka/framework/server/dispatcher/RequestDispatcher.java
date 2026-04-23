package com.onizuka.framework.server.dispatcher;

import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.server.routing.Route;
import com.onizuka.framework.server.routing.RouteRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestDispatcher {

    private static final List<Route> routes = RouteRegistry.getRoutes();

    // Hard coded response
//    static {
//        routes.add(new Route(
//                "GET",
//                "/users",
//                req -> new HttpResponse(200, "All Users")
//            ));
//        routes.add(new Route(
//                "GET",
//                "/users/{id}",
//                req -> {
//                    String id = req.getPathParam("id");
//
//                    HttpResponse res = new HttpResponse(200, "{ \"id\": \"" + id + "\" }");
//                    res.addHeader("Content-Type", "application/json");
//
//                    return res;
//                }
//            ));
//    }

    public static HttpResponse handle(HttpRequest request) {

        Route route = match(request);

        if (route == null)
            return new HttpResponse(404, "Not Found");

        System.out.println(route);

        return route.handler.handle(request);
    }

    // Route:
    // ("GET", "/users/{id}", "User details")
    public static Route match (HttpRequest request) {

        // /users/123 -> ["users", "123"]
        String[] requestParts = request.path.substring(1).split("/");

        for (Route route : routes) {
            if (!route.method.equalsIgnoreCase(request.method)) continue;

            // /users/{id} -> ["users", "{id}"]
            String[] patternParts = route.pathPattern.substring(1).split("/");

            if (patternParts.length != requestParts.length) continue;

            Map<String, String> params = new HashMap<>();
            boolean match = true;
            for (int i = 0; i < patternParts.length; i++) {
                // compare each part with its respective index
                String pattern = patternParts[i];
                String actual = requestParts[i];

                // param = {id}
                if (pattern.startsWith("{") && pattern.endsWith("}")) {
                    String key = pattern.substring(1, pattern.length() - 1);
                    params.put(key, actual);
                } else {
                    if (!pattern.equals(actual)) {
                        match = false;
                        break;
                    }
                }
            }
            if (match) {
                request.pathParams = params;
                System.out.println("param: " + request.pathParams);
                return route;
            }
        }
        return null;
    }
}
