package com.onizuka.framework.server.routing;

import com.onizuka.framework.annotations.*;
import com.onizuka.framework.exception.BadRequestException;
import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.json.JsonUtil;
import com.onizuka.framework.util.Handler;
import com.onizuka.framework.util.OniLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class RouteRegistry {

    private static final OniLogger log = OniLogger.get(RouteRegistry.class);
    private final List<Route> routes = new ArrayList<>();

    public void registerController(Class<?> controllerClass) {
        try {
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            String basePath = "";

            if (controllerClass.isAnnotationPresent(Controller.class)) {
                basePath = controllerClass.getAnnotation(Controller.class).value();
            }

            for (Method method : controllerClass.getDeclaredMethods()) {
                String httpMethod = null;
                String routePath = "";

                if (method.isAnnotationPresent(GET.class)) {
                    httpMethod = "GET";
                    routePath = method.getAnnotation(GET.class).value();
                } else if (method.isAnnotationPresent(POST.class)) {
                    httpMethod = "POST";
                    routePath = method.getAnnotation(POST.class).value();
                } else if (method.isAnnotationPresent(PUT.class)) {
                    httpMethod = "PUT";
                    routePath = method.getAnnotation(PUT.class).value();
                } else if (method.isAnnotationPresent(DELETE.class)) {
                    httpMethod = "DELETE";
                    routePath = method.getAnnotation(DELETE.class).value();
                } else if (method.isAnnotationPresent(PATCH.class)) {
                    httpMethod = "PATCH";
                    routePath = method.getAnnotation(PATCH.class).value();
                }

                if (httpMethod == null) continue;

                String fullPath = normalizePath(basePath + "/" + routePath);
                method.setAccessible(true);

                for (Parameter param : method.getParameters()) {
                    if (param.getType() == HttpRequest.class) continue;
                    if (param.isAnnotationPresent(PathParam.class)) continue;
                    if (param.isAnnotationPresent(QueryParam.class)) continue;
                    if (param.isAnnotationPresent(RequestBody.class)) continue;

                    throw new RuntimeException("Unsupported parameter type in method " + method.getName() + ": " + param.getName());
                }

                String finalHttpMethod = httpMethod;
                Handler handler = (HttpRequest req) -> {
                    Parameter[] parameters = method.getParameters();
                    Object[] args = new Object[parameters.length];

                    for (int i = 0; i < parameters.length; i++) {
                        Parameter param = parameters[i];

                        if (param.getType() == HttpRequest.class) {
                            args[i] = req;
                        } else if (param.isAnnotationPresent(PathParam.class)) {
                            String name = param.getAnnotation(PathParam.class).value();
                            String val = req.getPathParam(name);
                            if (val == null && param.getType().isPrimitive()) {
                                throw new BadRequestException("Missing path parameter: " + name);
                            }
                            args[i] = val == null ? null : convert(val, param.getType(), name);
                        } else if (param.isAnnotationPresent(QueryParam.class)) {
                            String name = param.getAnnotation(QueryParam.class).value();
                            String val = req.getQueryParam(name);
                            if (val == null && param.getType().isPrimitive()) {
                                throw new BadRequestException("Missing query parameter: " + name);
                            }
                            args[i] = val == null ? null : convert(val, param.getType(), name);
                        } else if (param.isAnnotationPresent(RequestBody.class)) {
                            args[i] = JsonUtil.fromJson(req.body, param.getType());
                        }
                    }

                    try {
                        Object result = method.invoke(controllerInstance, args);
                        if (result instanceof HttpResponse response) {
                            return response;
                        }
                        return new com.onizuka.framework.http.JsonResponse(200, result);
                    } catch (Exception e) {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        if (cause instanceof RuntimeException re) throw re;
                        throw new RuntimeException(cause);
                    }
                };

                routes.add(new Route(finalHttpMethod, fullPath, handler));
                log.info("Registered route: [" + finalHttpMethod + "] " + fullPath);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to register controller: " + controllerClass.getName(), e);
        }
    }

    private String normalizePath(String path) {
        return path.replaceAll("//+", "/");
    }

    private Object convert(String value, Class<?> type, String name) {
        try {
            if (type == String.class) return value;
            if (type == int.class || type == Integer.class) return Integer.parseInt(value);
            if (type == long.class || type == Long.class) return Long.parseLong(value);
            if (type == double.class || type == Double.class) return Double.parseDouble(value);
            if (type == boolean.class || type == Boolean.class) {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    throw new BadRequestException("Invalid boolean for " + name);
                }
                return Boolean.parseBoolean(value);
            }
            throw new RuntimeException("Unsupported type: " + type.getName());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid numeric value for parameter '" + name + "': " + value);
        }
    }

    public List<Route> getRoutes() {
        return routes;
    }
}