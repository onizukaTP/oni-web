package com.onizuka.framework.client;

import java.nio.ByteBuffer;

public class ClientState {
    public ByteBuffer buffer = ByteBuffer.allocate(1024);
    public StringBuilder request = new StringBuilder();
}
