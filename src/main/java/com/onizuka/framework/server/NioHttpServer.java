package com.onizuka.framework.server;

import com.onizuka.app.controllers.UserController;
import com.onizuka.framework.client.ClientState;
import com.onizuka.framework.exception.BadRequestException;
import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpRequestParser;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.server.dispatcher.RequestDispatcher;
import com.onizuka.framework.server.middleware.Middleware;
import com.onizuka.framework.server.middleware.implementations.DefaultMiddlewareChain;
import com.onizuka.framework.server.middleware.implementations.LoggingMiddleware;
import com.onizuka.framework.server.routing.RouteRegistry;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NioHttpServer {

    private static final int PORT = 8080;

    // caps how much unparsed data we'll buffer per connection before giving
    // up on it - without this, a client that never completes its headers
    // (or claims a huge Content-Length) can grow inputBuffer unboundedly
    private static final int MAX_REQUEST_SIZE = 1_048_576; // 1 MB

    private static final byte[] HEADER_DELIMITER = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    private static final List<Middleware> middlewares = List.of(
            new LoggingMiddleware()
//            new AuthMiddleware()
    );

    public static void main(String[] args) {
        try {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(PORT));

            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server started on port " + PORT);

            RouteRegistry.registerRoutes(UserController.class);

            while (true) {
                selector.select(); // blocking until an event occurs

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    // isolate failures to the connection they happened on -
                    // previously any unexpected exception here (an IOException
                    // during read/write, a bug in a handler, etc.) would
                    // propagate out of this loop and kill the entire server
                    try {
                        if (!key.isValid()) continue;

                        if (key.isAcceptable()) {
                            handleAccept((ServerSocketChannel) key.channel(), selector);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (IOException | RuntimeException e) {
                        System.err.println("Connection error, dropping client: " + e);
                        closeQuietly(key);
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleAccept(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel client = serverChannel.accept();

        if (client != null) {
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ, new ClientState());

            System.out.println("New client connected: " + client.getRemoteAddress());
        }
    }

    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ClientState state = (ClientState) key.attachment();

        state.readScratch.clear();
        int bytesRead = client.read(state.readScratch);

        if (bytesRead == -1) {
            // client closed the connection
            closeQuietly(key);
            return;
        }
        if (bytesRead == 0) {
            return;
        }

        state.readScratch.flip();
        byte[] chunk = new byte[state.readScratch.remaining()];
        state.readScratch.get(chunk);
        state.inputBuffer.write(chunk);

        if (state.inputBuffer.size() > MAX_REQUEST_SIZE) {
            queueResponse(key, state, new HttpResponse(431, "Request Header Fields Too Large"), true);
            state.inputBuffer.reset();
            return;
        }

        // a single read (or a connection kept alive across many requests)
        // can contain more than one complete request back to back, so keep
        // extracting requests until the buffer no longer has a full one
        while (true) {
            byte[] data = state.inputBuffer.toByteArray();
            int headerEnd = indexOf(data, HEADER_DELIMITER);

            if (headerEnd == -1) {
                // headers not fully received yet - wait for more reads
                break;
            }

            int headerBlockEnd = headerEnd + HEADER_DELIMITER.length;
            String headerText = new String(data, 0, headerBlockEnd, StandardCharsets.UTF_8);

            HttpRequest request;
            try {
                request = HttpRequestParser.parse(headerText);
            } catch (Exception e) {
                queueResponse(key, state, new HttpResponse(400, "Bad Request: " + e.getMessage()), true);
                state.inputBuffer.reset();
                return;
            }

            int contentLength = 0;
            String contentLengthHeader = request.getHeader("Content-Length");
            if (contentLengthHeader != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthHeader.trim());
                } catch (NumberFormatException e) {
                    queueResponse(key, state, new HttpResponse(400, "Invalid Content-Length"), true);
                    state.inputBuffer.reset();
                    return;
                }
            }

            int totalNeeded = headerBlockEnd + contentLength;
            if (data.length < totalNeeded) {
                // headers are in, but the body hasn't fully arrived yet
                break;
            }

            if (contentLength > 0) {
                request.body = new String(data, headerBlockEnd, contentLength, StandardCharsets.UTF_8);
            }

            // this request is fully parsed - keep whatever bytes come after
            // it (a pipelined next request) and reprocess the loop
            byte[] remainder = Arrays.copyOfRange(data, totalNeeded, data.length);
            state.inputBuffer.reset();
            state.inputBuffer.write(remainder);

            HttpResponse response = processRequest(request);
            boolean keepAlive = shouldKeepAlive(request);
            response.addHeader("Connection", keepAlive ? "keep-alive" : "close");

            queueResponse(key, state, response, !keepAlive);
        }
    }

    private static HttpResponse processRequest(HttpRequest request) {
        try {
            DefaultMiddlewareChain chain = new DefaultMiddlewareChain(middlewares, RequestDispatcher::handle);
            HttpResponse res = chain.next(request);

            if (!res.headers.containsKey("Content-Type")) {
                res.addHeader("Content-Type", "text/plain; charset=UTF-8");
            }
            return res;
        } catch (BadRequestException e) {
            return new HttpResponse(400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return new HttpResponse(500, "Internal Server Error");
        }
    }

    private static boolean shouldKeepAlive(HttpRequest request) {
        String connection = request.getHeader("Connection");
        if (connection != null) {
            return !connection.equalsIgnoreCase("close");
        }
        // HTTP/1.1 defaults to persistent connections; HTTP/1.0 defaults to close
        return request.version != null && request.version.startsWith("HTTP/1.1");
    }

    private static void handleWrite(SelectionKey key) throws IOException {
        ClientState state = (ClientState) key.attachment();
        flushPendingWrites(key, state);
    }

    private static void queueResponse(SelectionKey key, ClientState state, HttpResponse response, boolean closeAfter) throws IOException {
        state.pendingWrites.add(ByteBuffer.wrap(serialize(response)));
        if (closeAfter) {
            state.closeAfterWrite = true;
        }
        // try to send immediately; anything that doesn't fit stays queued
        // and OP_WRITE will pick up where this left off
        flushPendingWrites(key, state);
    }

    private static void flushPendingWrites(SelectionKey key, ClientState state) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();

        while (!state.pendingWrites.isEmpty()) {
            ByteBuffer buffer = state.pendingWrites.peekFirst();
            client.write(buffer);

            if (buffer.hasRemaining()) {
                // socket send buffer is full - stop for now, resume on OP_WRITE
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                return;
            }
            state.pendingWrites.pollFirst();
        }

        // everything queued has been flushed
        if (state.closeAfterWrite) {
            closeQuietly(key);
            return;
        }

        // nothing left to write - stop listening for writability so select()
        // doesn't keep waking up on every writable tick for no reason
        if (key.isValid()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    private static byte[] serialize(HttpResponse res) {
        byte[] bodyBytes = res.body != null ? res.body.getBytes(StandardCharsets.UTF_8) : new byte[0];

        StringBuilder head = new StringBuilder();
        head.append(statusLine(res.status)).append("\r\n");

        for (Map.Entry<String, String> header : res.headers.entrySet()) {
            if (header.getKey().equalsIgnoreCase("Content-Length")) continue; // computed below
            head.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }

        // Content-Length must be a byte count, not a char count - the
        // previous version used res.body.length(), which undercounts as
        // soon as the body contains any multi-byte UTF-8 character
        head.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        head.append("\r\n");

        byte[] headBytes = head.toString().getBytes(StandardCharsets.UTF_8);
        byte[] full = new byte[headBytes.length + bodyBytes.length];
        System.arraycopy(headBytes, 0, full, 0, headBytes.length);
        System.arraycopy(bodyBytes, 0, full, headBytes.length, bodyBytes.length);
        return full;
    }

    private static String statusLine(int status) {
        switch (status) {
            case 200: return "HTTP/1.1 200 OK";
            case 400: return "HTTP/1.1 400 Bad Request";
            case 401: return "HTTP/1.1 401 Unauthorized";
            case 404: return "HTTP/1.1 404 Not Found";
            case 431: return "HTTP/1.1 431 Request Header Fields Too Large";
            case 500: return "HTTP/1.1 500 Internal Server Error";
            default:  return "HTTP/1.1 " + status + " Unknown";
        }
    }

    private static int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static void closeQuietly(SelectionKey key) {
        try {
            key.channel().close();
        } catch (IOException ignored) {
        } finally {
            key.cancel();
        }
    }
}
