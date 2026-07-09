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

**Why do we need a Selector instead of just creating a thread per connection?** <br>
In a thread-per-connection model, each connection blocks a thread while waiting for I/O,
which leads to high memory usage and CPU overhead due to context switching. <br>
A Selector allows a single thread to monitor multiple channels and only process those
that are ready for I/O operations like read, write or accept. <br>
This avoids idle threads and makes the system much more scalable.

**channel.configureBlocking(false);**
`configureBlocking(false)` is required because the Selector model depends on
non-blocking channels. If a channel is blocking, operations like `read()` would stall the
event loop thread, preventing it from handling other connections, Also, blocking
channels cannot be registered with a Selector, resulting in an `IllegalBlockingModeException`.

**Where do you handle routing?** <br>
I separate I/O. parsing and routing into different components, After parsing the request,
I pass it to a dispatcher that decided the response based on method and path.

**What is Unwinding?** <br>
Call stack unwinding happens when functions start returning (or when an error/exception occurs).
and the stack gets cleared step by step.