package com.onizuka.framework.client;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Per-connection state attached to each SocketChannel's SelectionKey.
 *
 * Holds:
 *  - the inbound byte accumulator, which survives across multiple selector
 *    wakeups until a full request (headers + body) has arrived
 *  - the outbound write queue, used when a non-blocking write doesn't
 *    accept the whole response in one call (partial write)
 */
public class ClientState {

    // scratch buffer reused for each individual channel.read() call
    public final ByteBuffer readScratch = ByteBuffer.allocate(4096);

    // accumulates raw bytes for the request(s) currently being parsed.
    // Using bytes instead of a decoded String keeps this binary-safe for
    // request bodies (previously this was a StringBuilder, which silently
    // mangles data when multi-byte UTF-8 characters get split across reads).
    public final ByteArrayOutputStream inputBuffer = new ByteArrayOutputStream();

    // bytes still waiting to be flushed to the client. A non-blocking
    // SocketChannel.write() is not guaranteed to write everything in one
    // call - this queue holds whatever didn't fit until OP_WRITE fires again.
    public final Deque<ByteBuffer> pendingWrites = new ArrayDeque<>();

    // set once we know this connection must close after pendingWrites drains
    // (e.g. "Connection: close", malformed request, oversized request)
    public boolean closeAfterWrite = false;
}
