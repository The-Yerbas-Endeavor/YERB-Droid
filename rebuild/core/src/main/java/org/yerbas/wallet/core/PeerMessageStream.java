package org.yerbas.wallet.core;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Incremental decoder for arbitrarily fragmented TCP peer messages. */
public final class PeerMessageStream {
    private final NetworkParameters network;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public PeerMessageStream(NetworkParameters network) {
        this.network = network;
    }

    public synchronized List<WireMessage> append(byte[] bytes, int offset, int length) {
        if (bytes == null || offset < 0 || length < 0 || offset + length > bytes.length) {
            throw new IllegalArgumentException("Invalid stream chunk");
        }
        buffer.write(bytes, offset, length);
        List<WireMessage> messages = new ArrayList<>();
        while (true) {
            byte[] all = buffer.toByteArray();
            if (all.length < WireMessage.HEADER_SIZE) break;
            int payloadLength = ByteBuffer.wrap(all, 16, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (payloadLength < 0 || payloadLength > WireMessage.MAX_PAYLOAD_SIZE) {
                throw new IllegalArgumentException("Invalid peer payload length");
            }
            int frameLength = WireMessage.HEADER_SIZE + payloadLength;
            if (all.length < frameLength) break;
            messages.add(WireMessage.parse(network, Arrays.copyOfRange(all, 0, frameLength)));
            buffer.reset();
            buffer.writeBytes(Arrays.copyOfRange(all, frameLength, all.length));
        }
        return List.copyOf(messages);
    }

    public synchronized int bufferedBytes() {
        return buffer.size();
    }
}
