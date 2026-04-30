package com.onizuka.framework.server.routing;

import com.onizuka.framework.annotations.GET;
import com.onizuka.framework.annotations.PathParam;
import com.onizuka.framework.annotations.QueryParam;
import com.onizuka.framework.exception.BadRequestException;
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
            // Create a single instance of the controller (like a singleton)
            Object controllerInstance = controllerClass
                    .getDeclaredConstructor()
                    .newInstance();

            // Iterate through all methods of the controller
            for (Method method : controllerClass.getDeclaredMethods()) {

                // Only register methods annotated with @GET
                if (!method.isAnnotationPresent(GET.class)) continue;

                // Extract route path from annotation
                GET get = method.getAnnotation(GET.class);
                String path = get.value();

                // Allow invocation even if method is private
                method.setAccessible(true);

                /**
                 * Validate method parameters
                 * Only allow:
                 *  - HttpRequest (full request object)
                 *  - @PathParam (dynamic URL segment)
                 *  - @QueryParam (query string)
                 */
                for (Parameter param : method.getParameters()) {

                    if (param.getType() == HttpRequest.class) continue;

                    if (param.isAnnotationPresent(PathParam.class)) continue;

                    if (param.isAnnotationPresent(QueryParam.class)) continue;

                    // Reject unsupported parameter types early (fail-fast)
                    throw new RuntimeException(
                            "Unsupported parameter in " + method.getName() +
                                    ": " + param.getName()
                    );
                }

                /**
                 * Build a handler (lambda)
                 * This precomputes execution logic so the dispatcher stays simple.
                 */
                Handler handler = (HttpRequest req) -> {
                    try {
                        Parameter[] parameters = method.getParameters();
                        Object[] args = new Object[parameters.length];

                        // Resolve each parameter dynamically at request time
                        for (int i = 0; i < parameters.length; i++) {
                            Parameter param = parameters[i];

                            // Inject full HttpRequest object
                            if (param.getType() == HttpRequest.class) {
                                args[i] = req;
                            }

                            // Handle @PathParam (e.g., /users/{id})
                            else if (param.isAnnotationPresent(PathParam.class)) {
                                String name = param.getAnnotation(PathParam.class).value();
                                String value = req.getPathParam(name);
                                Class<?> type = param.getType();

                                // Missing value handling
                                if (value == null) {
                                    // Primitives cannot be null → throw 400
                                    if (type.isPrimitive()) {
                                        throw new BadRequestException("Missing path param: " + name);
                                    }
                                    // Wrapper types can be null
                                    args[i] = null;
                                } else {
                                    // Convert string → target type
                                    args[i] = convert(value, type, name);
                                }
                            }

                            // Handle @QueryParam (e.g., ?page=1)
                            else if (param.isAnnotationPresent(QueryParam.class)) {
                                String name = param.getAnnotation(QueryParam.class).value();
                                String value = req.getQueryParam(name);
                                Class<?> type = param.getType();

                                // Missing value handling
                                if (value == null) {
                                    if (type.isPrimitive()) {
                                        throw new BadRequestException("Missing query param: " + name);
                                    }
                                    args[i] = null;
                                } else {
                                    // Convert string → target type
                                    args[i] = convert(value, type, name);
                                }
                            }

                            // Safety fallback (should never happen due to validation above)
                            else {
                                throw new RuntimeException(
                                        "Unsupported parameter: " + param.getName()
                                );
                            }
                        }

                        // Invoke controller method using reflection
                        return (HttpResponse) method.invoke(controllerInstance, args);

                    }
                    // Client error (bad input → 400)
                    catch (BadRequestException e) {
                        return new HttpResponse(400, e.getMessage());
                    }
                    // Server error (unexpected failure → 500)
                    catch (Exception e) {
                        e.printStackTrace();
                        return new HttpResponse(500, "Internal Server Error");
                    }
                };

                // Register the route
                routes.add(new Route("GET", path, handler));

                // Debug log
                System.out.println("Registered GET " + path);
            }

        } catch (Exception e) {
            // Fail fast if controller instantiation fails
            throw new RuntimeException(e);
        }
    }

    private static Object convert(String value, Class<?> type, String name) {
        try {
            // No conversion needed
            if (type == String.class) return value;

            // Integer conversion
            if (type == int.class || type == Integer.class)
                return Integer.parseInt(value);

            // Long conversion
            if (type == long.class || type == Long.class)
                return Long.parseLong(value);

            // Double conversion
            if (type == double.class || type == Double.class)
                return Double.parseDouble(value);

            // Boolean conversion with strict validation
            if (type == boolean.class || type == Boolean.class) {
                // Prevent silent false (e.g., "abc" → false)
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    throw new BadRequestException(
                            "Invalid boolean value '" + value + "' for parameter '" + name + "'"
                    );
                }
                return Boolean.parseBoolean(value);
            }

            // Unsupported type → developer error
            throw new RuntimeException("Unsupported type: " + type.getName());

        }
        // Invalid numeric format → client error
        catch (NumberFormatException e) {
            throw new BadRequestException(
                    "Invalid value '" + value + "' for parameter '" + name + "'"
            );
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