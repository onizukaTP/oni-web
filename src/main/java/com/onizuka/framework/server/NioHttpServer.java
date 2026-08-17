package com.onizuka.framework.server;

import com.onizuka.framework.OniWebConfig;
import com.onizuka.framework.client.ClientState;
import com.onizuka.framework.exception.ExceptionHandler;
import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpRequestParser;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.server.dispatcher.RequestDispatcher;
import com.onizuka.framework.server.middleware.Middleware;
import com.onizuka.framework.server.middleware.implementations.DefaultMiddlewareChain;
import com.onizuka.framework.util.OniLogger;

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

    private static final OniLogger log = OniLogger.get(NioHttpServer.class);
    private static final int MAX_REQUEST_SIZE = 1_048_576; // 1 MB
    private static final byte[] HEADER_DELIMITER = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    private final OniWebConfig config;
    private final List<Middleware> middlewares;
    private final RequestDispatcher dispatcher;
    private final WorkerThreadPool workerPool;

    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean running = true;

    public NioHttpServer(OniWebConfig config, List<Middleware> middlewares, RequestDispatcher dispatcher) {
        this.config = config;
        this.middlewares = middlewares;
        this.dispatcher = dispatcher;
        this.workerPool = new WorkerThreadPool(config.getWorkerThreads());
    }

    public void start() {
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(config.getPort()));

            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            while (running) {
                selector.select();
                if (!running) break;

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    try {
                        if (!key.isValid()) continue;

                        if (key.isAcceptable()) {
                            handleAccept((ServerSocketChannel) key.channel(), selector);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (Exception e) {
                        log.error("Connection error, closing client", e);
                        closeQuietly(key);
                    }
                }
            }

        } catch (IOException e) {
            log.error("Server exception", e);
        } finally {
            shutdown();
        }
    }

    private void handleAccept(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel client = serverChannel.accept();
        if (client != null) {
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ, new ClientState());
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ClientState state = (ClientState) key.attachment();

        state.readScratch.clear();
        int bytesRead = client.read(state.readScratch);

        if (bytesRead == -1) {
            closeQuietly(key);
            return;
        }
        if (bytesRead == 0) return;

        state.readScratch.flip();
        byte[] chunk = new byte[state.readScratch.remaining()];
        state.readScratch.get(chunk);
        state.inputBuffer.write(chunk);

        if (state.inputBuffer.size() > MAX_REQUEST_SIZE) {
            queueResponse(key, state, new HttpResponse(431, "Request Header Fields Too Large"), true);
            state.inputBuffer.reset();
            return;
        }

        while (true) {
            byte[] data = state.inputBuffer.toByteArray();
            int headerEnd = indexOf(data, HEADER_DELIMITER);
            if (headerEnd == -1) break;

            int headerBlockEnd = headerEnd + HEADER_DELIMITER.length;
            String headerText = new String(data, 0, headerBlockEnd, StandardCharsets.UTF_8);

            HttpRequest request;
            try {
                request = HttpRequestParser.parse(headerText);
            } catch (Exception e) {
                queueResponse(key, state, ExceptionHandler.handle(e), true);
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
            if (data.length < totalNeeded) break;

            if (contentLength > 0) {
                request.body = new String(data, headerBlockEnd, contentLength, StandardCharsets.UTF_8);
            }

            byte[] remainder = Arrays.copyOfRange(data, totalNeeded, data.length);
            state.inputBuffer.reset();
            state.inputBuffer.write(remainder);

            workerPool.execute(() -> {
                HttpResponse response = processRequest(request);
                boolean keepAlive = shouldKeepAlive(request);
                response.addHeader("Connection", keepAlive ? "keep-alive" : "close");

                try {
                    queueResponse(key, state, response, !keepAlive);
                    if (selector.isOpen()) {
                        selector.wakeup();
                    }
                } catch (IOException e) {
                    closeQuietly(key);
                }
            });
        }
    }

    private HttpResponse processRequest(HttpRequest request) {
        try {
            DefaultMiddlewareChain chain = new DefaultMiddlewareChain(middlewares, dispatcher::handle);
            HttpResponse res = chain.next(request);
            if (!res.headers.containsKey("Content-Type")) {
                res.addHeader("Content-Type", "text/plain; charset=UTF-8");
            }
            return res;
        } catch (Throwable t) {
            return ExceptionHandler.handle(t);
        }
    }

    private boolean shouldKeepAlive(HttpRequest request) {
        String connection = request.getHeader("Connection");
        if (connection != null) return !connection.equalsIgnoreCase("close");
        return request.version != null && request.version.startsWith("HTTP/1.1");
    }

    private void handleWrite(SelectionKey key) throws IOException {
        ClientState state = (ClientState) key.attachment();
        flushPendingWrites(key, state);
    }

    private synchronized void queueResponse(SelectionKey key, ClientState state, HttpResponse response, boolean closeAfter) throws IOException {
        state.pendingWrites.add(ByteBuffer.wrap(serialize(response)));
        if (closeAfter) state.closeAfterWrite = true;
        if (key.isValid()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    private void flushPendingWrites(SelectionKey key, ClientState state) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();

        while (!state.pendingWrites.isEmpty()) {
            ByteBuffer buffer = state.pendingWrites.peekFirst();
            client.write(buffer);

            if (buffer.hasRemaining()) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                return;
            }
            state.pendingWrites.pollFirst();
        }

        if (state.closeAfterWrite) {
            closeQuietly(key);
            return;
        }

        if (key.isValid()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    private byte[] serialize(HttpResponse res) {
        byte[] bodyBytes = res.body != null ? res.body.getBytes(StandardCharsets.UTF_8) : new byte[0];

        StringBuilder head = new StringBuilder();
        head.append(statusLine(res.status)).append("\r\n");

        for (Map.Entry<String, String> header : res.headers.entrySet()) {
            if (header.getKey().equalsIgnoreCase("Content-Length")) continue;
            head.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }

        head.append("Content-Length: ").append(bodyBytes.length).append("\r\n\r\n");

        byte[] headBytes = head.toString().getBytes(StandardCharsets.UTF_8);
        byte[] full = new byte[headBytes.length + bodyBytes.length];
        System.arraycopy(headBytes, 0, full, 0, headBytes.length);
        System.arraycopy(bodyBytes, 0, full, headBytes.length, bodyBytes.length);
        return full;
    }

    private String statusLine(int status) {
        return switch (status) {
            case 200 -> "HTTP/1.1 200 OK";
            case 201 -> "HTTP/1.1 201 Created";
            case 204 -> "HTTP/1.1 204 No Content";
            case 400 -> "HTTP/1.1 400 Bad Request";
            case 401 -> "HTTP/1.1 401 Unauthorized";
            case 403 -> "HTTP/1.1 403 Forbidden";
            case 404 -> "HTTP/1.1 404 Not Found";
            case 409 -> "HTTP/1.1 409 Conflict";
            case 429 -> "HTTP/1.1 429 Too Many Requests";
            case 431 -> "HTTP/1.1 431 Request Header Fields Too Large";
            case 500 -> "HTTP/1.1 500 Internal Server Error";
            default -> "HTTP/1.1 " + status + " Unknown";
        };
    }

    private int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private void closeQuietly(SelectionKey key) {
        try {
            if (key.channel() != null) key.channel().close();
        } catch (IOException ignored) {
        } finally {
            key.cancel();
        }
    }

    public synchronized void shutdown() {
        if (!running) return;
        running = false;
        log.info("Shutting down OniWeb Server...");

        workerPool.shutdown();

        try {
            if (selector != null && selector.isOpen()) selector.close();
            if (serverChannel != null && serverChannel.isOpen()) serverChannel.close();
        } catch (IOException e) {
            log.error("Error closing server channel/selector", e);
        }
    }
}
