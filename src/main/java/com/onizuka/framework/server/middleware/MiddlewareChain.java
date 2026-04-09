package com.onizuka.framework.server.middleware;

import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;

public interface MiddlewareChain {
    HttpResponse next (HttpRequest request);
}
