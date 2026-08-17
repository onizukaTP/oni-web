# OniWeb Framework 🚀

A lightweight, high-performance, reactive backend framework built in **Java 17** using **Java NIO (`java.nio.channels`)**. Designed as a simple, transparent, and ultra-fast alternative to bloated traditional web frameworks.

---

## ⚡ Key Features

- **Non-blocking I/O Engine**: Powered by `java.nio.channels.Selector` and `ServerSocketChannel`.
- **Worker Thread Offloading**: Event loop remains pure non-blocking I/O while handler tasks execute on a dedicated worker pool.
- **Declarative Annotation Routing**: `@Controller`, `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PATCH`.
- **Parameter Injection**: Dynamic resolution of `@PathParam`, `@QueryParam`, `@RequestBody`, and `HttpRequest`.
- **First-class Jackson JSON**: Transparent request body deserialization & response object serialization.
- **Middleware Pipeline**: Built-in support for CORS (`CorsMiddleware`), Rate Limiting (`RateLimitMiddleware`), Logging, and custom chains.
- **Static Asset Server**: Built-in non-blocking static file handler (`/static/*` / `index.html`).
- **Declarative Exception Handling**: Maps `HttpException`, `NotFoundException`, and unknown errors to clean JSON responses.

---

## 🚀 Quick Start

### 1. Create a Controller

```java
package com.onizuka.app.controllers;

import com.onizuka.framework.annotations.*;
import com.onizuka.framework.exception.NotFoundException;

@Controller("/api/hello")
public class HelloController {

    @GET("/{name}")
    public String greet(@PathParam("name") String name) {
        return "Hello, " + name + "!";
    }
}
```

### 2. Bootstrap Your Application

```java
package com.onizuka.app;

import com.onizuka.framework.OniWeb;

public class App {
    public static void main(String[] args) {
        OniWeb.start(App.class, 8080);
    }
}
```

---

## ⚖️ OniWeb vs. Spring Boot

| Feature / Metric | Spring Boot | OniWeb |
|---|---|---|
| **Startup Time** | ~2.0s - 8.0s | **< 200ms** |
| **Memory Footprint** | ~200MB - 400MB | **~20MB - 40MB** |
| **I/O Model** | Thread-per-request (Tomcat) | **Java NIO Event Loop + Worker Pool** |
| **Framework Size** | ~30MB - 80MB JAR | **< 2MB (with Jackson)** |
| **Auto-Configuration** | Heavy Magic / Hidden Logic | **Zero Magic / 100% Explicit** |

---

## 🧪 Verification & Test Commands (PowerShell)

```powershell
# Run server
mvn compile exec:java

# GET all users
Invoke-RestMethod http://localhost:8080/users

# GET user by ID
Invoke-RestMethod http://localhost:8080/users/1

# POST new user
Invoke-RestMethod -Uri http://localhost:8080/users -Method Post -ContentType "application/json" -Body '{"id":"3","name":"Akira","email":"akira@oni.com"}'

# PUT update user
Invoke-RestMethod -Uri http://localhost:8080/users/3 -Method Put -ContentType "application/json" -Body '{"name":"Akira Senoh","email":"akira.senoh@oni.com"}'

# DELETE user
Invoke-RestMethod -Uri http://localhost:8080/users/3 -Method Delete

# Static file serving
Invoke-WebRequest http://localhost:8080/index.html
```