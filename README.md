# Oni Web

I am trying to build a reactive web framework in java, I'll be implementing everything on my own and. I'll use Java IO library to take some work off my shoulder.

- **HTTP Server** — using Java NIO (java.nio.channels) to handle raw TCP connections and parse HTTP/1.1 requests manually (request line, headers, body)
- **Router Engine** — annotation-based (`@Route`, `@GET`, `@POST`, `@PathParam`) using reflection to map URLs to handler methods at startup
- **Dependency Injection Container** — a mini IoC container with `@Component` and `@Inject`, managing bean registration and lifecycle using reflection
- **Middleware Pipeline** — a chain-of-responsibility pattern where middleware (logging, auth, rate limiting) plugs in via a `@Middleware` annotation
- **Template Engine** — a simple `{{variable}}` and `{{#each list}}` parser, no Thymeleaf
- **Async Request Handling** — non-blocking request handling using CompletableFuture and a custom thread pool
- **Demo App** — a small real app (URL Shortener or mini blog) built entirely on top of my framework