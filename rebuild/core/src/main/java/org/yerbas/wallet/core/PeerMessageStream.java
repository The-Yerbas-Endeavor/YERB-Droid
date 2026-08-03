package org.yerbas.wallet.core;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/** Incremental decoder for TCP peer message frames. */
public final class PeerMessageStream {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public void append(byte[] bytes) {
        buffer.writeBytes(bytes);
    }

    public byte[] pollPayload(int payloadLength) {
        byte[] all = buffer.toByteArray();
        if (all.length < payloadLength) return null;
        byte[] result = Arrays.copyOfRange(all, 0, payloadLength);
        buffer.reset();
        buffer.writeBytes(Arrays.copyOfRange(all, payloadLength, all.length));
        return result;
    }

    public int bufferedBytes() {
        return buffer.size();
    }
}
