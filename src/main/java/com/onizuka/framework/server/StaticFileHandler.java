package com.onizuka.framework.server;

import com.onizuka.framework.exception.NotFoundException;
import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.util.OniLogger;

import java.io.InputStream;
import java.net.URLConnection;

public class StaticFileHandler {

    private static final OniLogger log = OniLogger.get(StaticFileHandler.class);
    private final String staticDir;

    public StaticFileHandler(String staticDir) {
        this.staticDir = staticDir.startsWith("/") ? staticDir : "/" + staticDir;
    }

    public HttpResponse handle(HttpRequest request) {
        String path = request.path;
        if (path.contains("..")) {
            return new HttpResponse(403, "Access Denied");
        }

        String resourcePath = staticDir + (path.equals("/") ? "/index.html" : path);
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new NotFoundException("Static file not found: " + path);
            }

            byte[] bytes = is.readAllBytes();
            String contentType = URLConnection.guessContentTypeFromName(resourcePath);
            if (contentType == null) {
                if (resourcePath.endsWith(".css")) contentType = "text/css";
                else if (resourcePath.endsWith(".js")) contentType = "application/javascript";
                else if (resourcePath.endsWith(".html")) contentType = "text/html";
                else contentType = "application/octet-stream";
            }

            HttpResponse response = new HttpResponse(200, new String(bytes));
            response.addHeader("Content-Type", contentType);
            return response;
        } catch (Exception e) {
            return null;
        }
    }
}
