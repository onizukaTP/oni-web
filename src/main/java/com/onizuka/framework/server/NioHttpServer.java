package com.onizuka.framework.server;

import com.onizuka.framework.client.ClientState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioHttpServer {
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

            System.out.println("Full request:");
            System.out.println(fullRequest);

            state.request.delete(0, index + DELIMITER.length());
        }

        // clear buffer so it can be written into again
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
