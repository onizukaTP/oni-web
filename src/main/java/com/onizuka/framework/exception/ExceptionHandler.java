package com.onizuka.framework.exception;

import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.http.JsonResponse;
import com.onizuka.framework.util.OniLogger;

import java.util.Map;

public class ExceptionHandler {

    private static final OniLogger log = OniLogger.get(ExceptionHandler.class);

    public static HttpResponse handle(Throwable throwable) {
        if (throwable instanceof HttpException httpEx) {
            log.warn("Handled HttpException [" + httpEx.getStatusCode() + "]: " + httpEx.getMessage());
            return new JsonResponse(httpEx.getStatusCode(), Map.of(
                    "status", httpEx.getStatusCode(),
                    "error", httpEx.getMessage()
            ));
        }

        if (throwable instanceof BadRequestException badReqEx) {
            log.warn("Handled BadRequestException [400]: " + badReqEx.getMessage());
            return new JsonResponse(400, Map.of(
                    "status", 400,
                    "error", badReqEx.getMessage()
            ));
        }

        log.error("Unhandled Exception caught in pipeline: " + throwable.getMessage(), throwable);
        return new JsonResponse(500, Map.of(
                "status", 500,
                "error", "Internal Server Error"
        ));
    }
}
