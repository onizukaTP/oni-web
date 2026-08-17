package com.onizuka.app.controllers;

import com.onizuka.app.models.User;
import com.onizuka.framework.annotations.*;
import com.onizuka.framework.exception.NotFoundException;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.http.JsonResponse;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller("/users")
public class UserController {

    private final Map<String, User> userDb = new ConcurrentHashMap<>();

    public UserController() {
        userDb.put("1", new User("1", "Eikichi Onizuka", "onizuka@gto.jp"));
        userDb.put("2", new User("2", "Ryuji Danma", "ryuji@gto.jp"));
    }

    @GET("/")
    public Collection<User> getAllUsers() {
        return userDb.values();
    }

    @GET("/{id}")
    public User getUserById(@PathParam("id") String id) {
        User user = userDb.get(id);
        if (user == null) {
            throw new NotFoundException("User with ID " + id + " not found");
        }
        return user;
    }

    @POST("/")
    public HttpResponse createUser(@RequestBody User user) {
        if (user.getId() == null || user.getId().isBlank()) {
            user.setId(String.valueOf(userDb.size() + 1));
        }
        userDb.put(user.getId(), user);
        return new JsonResponse(201, user);
    }

    @PUT("/{id}")
    public User updateUser(@PathParam("id") String id, @RequestBody User user) {
        if (!userDb.containsKey(id)) {
            throw new NotFoundException("User with ID " + id + " not found");
        }
        user.setId(id);
        userDb.put(id, user);
        return user;
    }

    @DELETE("/{id}")
    public HttpResponse deleteUser(@PathParam("id") String id) {
        if (userDb.remove(id) == null) {
            throw new NotFoundException("User with ID " + id + " not found");
        }
        return new JsonResponse(200, Map.of("message", "User " + id + " deleted successfully"));
    }
}
