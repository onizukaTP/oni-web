package com.onizuka.app.controllers;

import com.onizuka.framework.annotations.GET;
import com.onizuka.framework.annotations.PathParam;
import com.onizuka.framework.annotations.QueryParam;
import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpResponse;

public class UserController {

    @GET("/users/{id}")
    public HttpResponse getUser(@PathParam("id") String id) {
        return new HttpResponse(200, "user " + id);
    }

    @GET("/users")
    public HttpResponse getUsers(
            HttpRequest req,
            @QueryParam("page") Integer page
    ) {
        if (page != null) {
            return new HttpResponse(200, "Page: " + page);
        }
        return new HttpResponse(200, "All users");
    }
}
