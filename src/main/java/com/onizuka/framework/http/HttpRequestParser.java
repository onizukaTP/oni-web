package com.onizuka.framework.http;

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

        return new HttpRequest(
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                headers
        );

    }
}
