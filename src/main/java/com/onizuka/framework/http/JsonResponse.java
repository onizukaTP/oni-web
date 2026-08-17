package com.onizuka.framework.http;

import com.onizuka.framework.json.JsonUtil;

import java.util.Map;

public class JsonResponse extends HttpResponse {

    public JsonResponse(int status, Object data) {
        super(status, Map.of("Content-Type", "application/json; charset=UTF-8"), JsonUtil.toJson(data));
    }
}
