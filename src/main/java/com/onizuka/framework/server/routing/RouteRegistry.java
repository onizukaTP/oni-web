package com.onizuka.framework.server.routing;

import com.onizuka.framework.annotations.GET;
import com.onizuka.framework.annotations.PathParam;
import com.onizuka.framework.annotations.QueryParam;
import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.util.Handler;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class RouteRegistry {

    // Stores all registered routes
    private static final List<Route> routes = new ArrayList<>();

    /**
     * This method scans a controller class using reflection
     * and converts annotated methods into executable routes.
     */
    public static void registerRoutes(Class<?> controllerClass) {
        try {
            // Create ONE instance of the controller (singleton-style)
            Object controllerInstance = controllerClass
                    .getDeclaredConstructor()
                    .newInstance();

            // Loop through all methods in the controller
            for (Method method : controllerClass.getDeclaredMethods()) {

                // Only process methods annotated with @GET
                if (!method.isAnnotationPresent(GET.class)) continue;

                GET get = method.getAnnotation(GET.class);
                String path = get.value();

                // Allow private methods to be invoked
                method.setAccessible(true);

                /**
                 * Validate method parameters
                 * Supported:
                 *  - HttpRequest
                 *  - @PathParam
                 *  - @QueryParam
                 */
                for (Parameter param : method.getParameters()) {

                    if (param.getType() == HttpRequest.class) continue;

                    if (param.isAnnotationPresent(PathParam.class)) continue;

                    if (param.isAnnotationPresent(QueryParam.class)) continue;

                    throw new RuntimeException(
                            "Unsupported parameter in " + method.getName() +
                                    ": " + param.getName()
                    );
                }

                /**
                 * Create a handler (lambda)
                 * This is the KEY part:
                 * We pre-build execution logic so dispatcher stays simple.
                 */
                Handler handler = (HttpRequest req) -> {
                    try {
                        Parameter[] parameters = method.getParameters();
                        Object[] args = new Object[parameters.length];

                        // Resolve arguments dynamically at runtime
                        for (int i = 0; i < parameters.length; i++) {
                            Parameter param = parameters[i];

                            // Inject full request
                            if (param.getType() == HttpRequest.class) {
                                args[i] = req;
                            }

                            // Inject path variable
                            else if (param.isAnnotationPresent(PathParam.class)) {
                                String name = param.getAnnotation(PathParam.class).value();
                                String value = req.getPathParam(name);

                                if (value == null) {
                                    throw new RuntimeException("Missing path param: " + name);
                                }

                                args[i] = value;
                            }

                            // Inject query param
                            else if (param.isAnnotationPresent(QueryParam.class)) {
                                String name = param.getAnnotation(QueryParam.class).value();
                                args[i] = req.getQueryParam(name); // can be null
                            }

                            else {
                                throw new RuntimeException(
                                        "Unsupported parameter: " + param.getName()
                                );
                            }
                        }

                        // Invoke controller method via reflection
                        return (HttpResponse) method.invoke(controllerInstance, args);

                    } catch (Exception e) {
                        System.out.println(e.toString());
                        return new HttpResponse(500, "Internal Server Error");
                    }
                };

                // Create and store route
                Route route = new Route("GET", path, handler);
                routes.add(route);

                System.out.println("Registered GET " + path);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns all registered routes
     * Dispatcher will use this
     */
    public static List<Route> getRoutes() {
        return routes;
    }
}