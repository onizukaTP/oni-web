package com.onizuka.framework.util;

import com.onizuka.framework.http.HttpRequest;

// helper function
@FunctionalInterface
public interface Handler {
    Object handle(HttpRequest req);
}
