package com.onizuka.framework.server.middleware.implementations;

import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.http.JsonResponse;
import com.onizuka.framework.server.middleware.Middleware;
import com.onizuka.framework.server.middleware.MiddlewareChain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitMiddleware implements Middleware {

    private final int maxRequests;
    private final long windowMillis;
    private final ConcurrentHashMap<String, RequestCounter> clients = new ConcurrentHashMap<>();

    public RateLimitMiddleware(int maxRequestsPerMinute) {
        this.maxRequests = maxRequestsPerMinute;
        this.windowMillis = 60_000;
    }

    @Override
    public HttpResponse handle(HttpRequest request, MiddlewareChain next) {
        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        RequestCounter counter = clients.compute(clientIp, (ip, current) -> {
            if (current == null || (now - current.startTime) > windowMillis) {
                return new RequestCounter(now, new AtomicInteger(1));
            }
            current.count.incrementAndGet();
            return current;
        });

        if (counter.count.get() > maxRequests) {
            return new JsonResponse(429, Map.of(
                    "status", 429,
                    "error", "Too Many Requests. Rate limit exceeded."
            ));
        }

        return next.next(request);
    }

    private String getClientIp(HttpRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return "default-client";
    }

    private static class RequestCounter {
        final long startTime;
        final AtomicInteger count;

        RequestCounter(long startTime, AtomicInteger count) {
            this.startTime = startTime;
            this.count = count;
        }
    }
}
