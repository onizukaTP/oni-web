package com.onizuka.framework.server.routing;

import com.onizuka.framework.annotations.GET;
import com.onizuka.framework.annotations.PathParam;
import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.util.Handler;


import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class RouteRegistry {

    private static final List<Route> routes = new ArrayList<>();

    public static void registerRoutes(Class<?> controllerClass) { // can also use clazz or type
        try {
            Object controllerInstance = controllerClass
                    .getDeclaredConstructor()
                    .newInstance();

            for (Method method : controllerClass.getDeclaredMethods()) {

                // Detect @GET
                if (method.isAnnotationPresent(GET.class)) {

                    // temp simple validation
                    if (method.getParameterCount() != 1 ||
                            method.getParameterTypes()[0] != HttpRequest.class) {
                        throw new RuntimeException("Invalid handler method: " + method.getName());
                    }
                    GET get = method.getAnnotation(GET.class);
                    String path = get.value();

                    // Create handler
                    Handler handler = (HttpRequest req) -> {
                        try {
                            Parameter[] parameters = method.getParameters();
                            Object[] args = new Object[parameters.length];

                            for (int i = 0; i < parameters.length; i++) {
                                Parameter param = parameters[i];

                                if (param.isAnnotationPresent(PathParam.class)) {

                                    String name = param.getAnnotation(PathParam.class).value();

                                    String value = req.getPathParam(name);

                                    args[i] = value; // for now assume String only
                                }
                            }

                            method.setAccessible(true);
                            return (HttpResponse) method.invoke(controllerInstance, req);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    };

                    // Register route
                    Route route = new Route("GET", path, handler);
                    routes.add(route);

                    System.out.println("Registered GET " + path);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Route> getRoutes() {return routes;}
}