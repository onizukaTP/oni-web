package com.onizuka.framework.server;

import com.onizuka.framework.client.ClientState;
import com.onizuka.framework.http.HttpRequest;
import com.onizuka.framework.http.HttpRequestParser;
import com.onizuka.framework.http.HttpResponse;
import com.onizuka.framework.server.dispatcher.RequestDispatcher;
import com.onizuka.framework.server.middleware.Middleware;
import com.onizuka.framework.server.middleware.implementations.AuthMiddleware;
import com.onizuka.framework.server.middleware.implementations.DefaultMiddlewareChain;
import com.onizuka.framework.server.middleware.implementations.LoggingMiddleware;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NioHttpServer {

    private static final List<Middleware> middlewares = List.of(
            new LoggingMiddleware()
//            new AuthMiddleware()
    );

    public static void main(String[] args) {
        try {
            // create server socker channel
            ServerSocketChannel serverChannel = ServerSocketChannel.open();

            // set non blocking mode
            serverChannel.configureBlocking(false);

            // bind to port
            serverChannel.bind(new InetSocketAddress(8080));

            // create selector
            Selector selector = Selector.open();

            // register channel for accept events
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server started on port 8080");

            // event loop
            while (true) {
                selector.select(); // blocking until event occurs

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        handleAccept(server, selector);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleRead(SelectionKey key) throws IOException {
        String DELIMITER = "\r\n\r\n";
        SocketChannel client = (SocketChannel) key.channel();

        // write → flip → read → clear → repeat
        // get the buffer attached to this client (stores incoming data)
        ClientState state = (ClientState) key.attachment();
        ByteBuffer buffer = state.buffer;

        // read data from client into the buffer
        int bytesRead = client.read(buffer);

        // -1 means client closed the connection
        if (bytesRead == -1) {
            System.out.println("Connection closed");
            client.close();
            return;
        }

        /*
        * Example: writing mode
        * [ H E L L O _ _ _ _ _ ]
        *           ↑
        *       position = 5
        * limit = 10
        * */

        // flip to read mode from writing mode
        // position = where new data will be written
        // limit = capacity
        buffer.flip(); // limit = number of bytes read, position = 0

        /*
        * Example: after flip
        * [ H E L L O _ _ _ _ _ ]
        * ↑         ↑
        * pos=0   limit=5
        * */

        // read only the data that was actually written into the buffer
        byte[] bytes = new byte[buffer.remaining()];
        // read bytes from buffer into array
        // position moves forward as we read, limit stays unchanged
        buffer.get(bytes); // position = limit after this

        // Example: read all data
        // [ H E L L O _ _ _ _ _ ]
        //           ↑
        //        position = 5
        // limit = 5

        // convert bytes to string
        String data = new String(bytes);
        state.request.append(data);

        System.out.println("Received chunk:");
        System.out.println(data);

        // check if we received full HTTP request (end of headers)
        int index;
        while ((index = state.request.indexOf(DELIMITER)) != -1) {
            String fullRequest = state.request.substring(0, index + DELIMITER.length());

            try {
                HttpRequest request = HttpRequestParser.parse(fullRequest);

                DefaultMiddlewareChain chain =
                        new DefaultMiddlewareChain(middlewares, RequestDispatcher::handle);

                HttpResponse res = chain.next(request);
                System.out.println("Handled request");

                if (!res.headers.containsKey("Content-Type")) {
                    res.addHeader("Content-Type", "text/plain; charset=UTF-8");
                }

                StringBuilder response = new StringBuilder();

                // status line
                String statusLine;
                switch (res.status) {
                    case 200: statusLine = "HTTP/1.1 200 OK"; break;
                    case 404: statusLine = "HTTP/1.1 404 Not Found"; break;
                    case 401: statusLine = "HTTP/1.1 401 Unauthorized"; break;
                    default: statusLine = "HTTP/1.1 500 Internal Server Error";
                }

                response.append(statusLine).append("\r\n");

                // headers
                for (Map.Entry<String, String> header : res.headers.entrySet()) {
                    response.append(header.getKey())
                            .append(": ")
                            .append(header.getValue())
                            .append("\r\n");
                }

                // mandatory header
                response.append("Content-Length: ")
                                .append(res.body.length())
                                        .append("\r\n");

                // empty line
                response.append("\r\n");

                // body
                response.append(res.body);

                System.out.println("Response:");
                System.out.println(response);

                client.write(ByteBuffer.wrap(response.toString().getBytes(StandardCharsets.UTF_8)));

            } catch (Exception e) {
                String response =
                        "HTTP/1.1 400 Bad Request\r\n" +
                                "Content-Length: 11\r\n" +
                                "\r\n" +
                                "Bad Request";

                client.write(ByteBuffer.wrap(response.getBytes()));
            }

            // connection closed - disables keep-alive/multiple requests per connection
            client.close();
            return;
        }

        // clear buffer so it can be written into again
        // unreachable code path, connection is closed above and the method is returned
        buffer.clear(); // position = 0, limit = capacity
        /*
         * Example: back to write mode
         * [ H E L L O _ _ _ _ _ ]
         * ↑
         * pos=0
         * limit=10
         */
    }

    private static void handleAccept(ServerSocketChannel serverChannel, Selector selector) throws IOException{
        SocketChannel client = serverChannel.accept();

        if (client != null) {
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ, new ClientState());

            System.out.println("New Client Connected: " + client.getRemoteAddress());
        }

    }
}
