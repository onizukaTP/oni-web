package com.onizuka.framework.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestParser {
    /*
     * GET /index.html HTTP/1.1\r\n
     * Host: example.com\r\n\r\n
     * */
    public static HttpRequest parse(String rawRequest) {
        // split the lines using "\r\n" delimiter
        String[] lines = rawRequest.split("\r\n");

        // get the method, path and version from the first line
        String[] parts = lines[0].trim().split("\\s+");

        // dealing with malformed request or missing values
        if (parts.length < 3)
            throw new IllegalArgumentException("Invalid request line");

        // loop through the headers and add them to the map
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) break;

            String[] headerParts = lines[i].split(":", 2);
            if (headerParts.length == 2)
                headers.put(headerParts[0].trim(), headerParts[1].trim());
        }

        // path + query parsing
        String fullPath = parts[1].trim();
        String path;
        String queryString = null;

        int queryIndex = fullPath.indexOf('?');
        if (queryIndex != -1) {
            path = fullPath.substring(0, queryIndex).trim();
            queryString = fullPath.substring(queryIndex + 1);
        } else {
            path = fullPath;
        }

        // query params
        Map<String, String> queryParams = new HashMap<>();
        if (queryString != null && !queryString.isEmpty()) {
            String[] pairs = queryString.split("&");

            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);

                String key = URLDecoder.decode(keyValue[0].trim(), StandardCharsets.UTF_8);
                String value = keyValue.length > 1
                        ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8)
                        : "";

                queryParams.put(key, value);
            }
        }

        // path params (placeholder)
        Map<String, String> pathParams = new HashMap<>();

        return new HttpRequest(
                parts[0].trim().toUpperCase(), // method
                path,
                parts[2].trim(), // version
                headers,
                pathParams,
                queryParams
        );

    }
}
