# Oni Web

I am building a reactive web framework in Java from scratch — no Spring, no Netty, no external dependencies beyond the JDK. The goal is to deeply understand how web frameworks work internally, and to build something impressive.
The framework will consist of these layers:

- **HTTP Server** — using Java NIO (java.nio.channels) to handle raw TCP connections and parse HTTP/1.1 requests manually (request line, headers, body)
- **Router Engine** — annotation-based (`@Route`, `@GET`, `@POST`, `@PathParam`) using reflection to map URLs to handler methods at startup
- **Dependency Injection Container** — a mini IoC container with `@Component` and `@Inject`, managing bean registration and lifecycle using reflection
- **Middleware Pipeline** — a chain-of-responsibility pattern where middleware (logging, auth, rate limiting) plugs in via a `@Middleware` annotation
- **Template Engine** — a simple `{{variable}}` and `{{#each list}}` parser, no Thymeleaf
- **Async Request Handling** — non-blocking request handling using CompletableFuture and a custom thread pool
- **Demo App** — a small real app (URL Shortener or mini blog) built entirely on top of my framework

I am a Java developer and through this project I am hoping to learn advanced concepts like NIO, reflection, and concurrency.

## Core Concepts:
- ### Channel
  - Channels are pipe line, they are bidirectional (read + write)
  - `ServerSocketChannel` &rarr; listens for connections
  - `SocketChannel` &rarr; represents a client connection
- ### Buffer
  - NIO does not give you data directly, we are manually managing memory state
  - Important concepts: `position`, `limit`, `flip()` and `clear()`
- ### Selectors
  - A Selector lets one thread monitor many channels
  - `OP_ACCEPT` &rarr; new connection
  - `OP_READ` &rarr; data available
  - `OP_WRITE` &rarr; ready to send data
- ### Event loop model
  - accept connection &rarr; read request &rarr; send response
```
while (true) {
    selector.select();

    for (each key ready) {
        if (ACCEPT) → accept connection
        if (READ) → read request
        if (WRITE) → send response
    }
}
```
- ### Partial Reads
  - NIO does not guarantee you get the full request in one read.
```
GET /he
llo HTTP/1.1\r\n
Host: example.com\r\n
```

---
## Learnings:

**What is Java NIO?** <br>
Java NIO (Non-blocking I/O) lets you handle many connections with fewer threads by working with streams of data instead of blocking calls.