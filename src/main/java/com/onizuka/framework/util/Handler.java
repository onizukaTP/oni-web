package com.onizuka.framework.util;

import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;

// helper function
@FunctionalInterface
public interface Handler {
    HttpResponse handle(HttpRequest req);
}
