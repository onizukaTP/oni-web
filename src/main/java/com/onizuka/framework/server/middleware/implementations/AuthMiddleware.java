package com.onizuka.framework.server.middleware.implementations;

import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.server.middleware.Middleware;
import com.onizuka.framework.server.middleware.MiddlewareChain;
import com.onizuka.framework.util.OniLogger;

public class AuthMiddleware implements Middleware {
    private static final OniLogger log = OniLogger.get(AuthMiddleware.class);

    @Override
    public HttpResponse handle(HttpRequest request, MiddlewareChain next) {
        String token = request.getHeader("Authorization");

        if (token == null || !token.equals("valid-token")) {
            log.warn("Unauthorized request attempt to " + request.path);
            return new HttpResponse(401, "Unauthorized");
        }

        return next.next(request);
    }
}
