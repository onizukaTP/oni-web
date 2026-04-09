package com.onizuka.framework.server.middleware.implementations;

import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.server.middleware.Middleware;
import com.onizuka.framework.server.middleware.MiddlewareChain;

public class AuthMiddleware implements Middleware {
    @Override
    public HttpResponse handle(HttpRequest request, MiddlewareChain next) {
        String token = request.headers.get("Authorization");

        if (token == null || !token.equals("valid-token")) {
            System.out.println("Unauthorized request");
            return new HttpResponse(401, "Unauthorized");
        }

        System.out.println("Auth success");

        return next.next(request); // continue if valid

    }
}
