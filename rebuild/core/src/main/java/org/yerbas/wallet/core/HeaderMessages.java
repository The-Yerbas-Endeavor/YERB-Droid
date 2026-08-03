package org.yerbas.wallet.core;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/** Codecs for getheaders and headers payloads. */
public final class HeaderMessages {
    private HeaderMessages() {}

    public static byte[] encodeGetHeaders(int protocol, List<byte[]> locators, byte[] stopHash) {
        if (locators.isEmpty()) throw new IllegalArgumentException("at least one locator required");
        if (stopHash.length != 32) throw new IllegalArgumentException("stop hash must be 32 bytes");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeInt32(out, protocol);
        write(out, CompactSize.write(locators.size()));
        for (byte[] locator : locators) {
            if (locator.length != 32) throw new IllegalArgumentException("locator must be 32 bytes");
            write(out, locator);
        }
        write(out, stopHash);
        return out.toByteArray();
    }

    public static List<BlockHeader> decodeHeaders(byte[] payload) {
        CompactSize.Decoded count = CompactSize.read(payload, 0);
        if (count.value() > 2000) throw new IllegalArgumentException("too many headers");
        int offset = count.encodedLength();
        List<BlockHeader> headers = new ArrayList<>((int) count.value());
        for (int i = 0; i < count.value(); i++) {
            if (offset + BlockHeader.SERIALIZED_SIZE > payload.length) throw new IllegalArgumentException("truncated header");
            byte[] raw = java.util.Arrays.copyOfRange(payload, offset, offset + BlockHeader.SERIALIZED_SIZE);
            offset += BlockHeader.SERIALIZED_SIZE;
            CompactSize.Decoded txCount = CompactSize.read(payload, offset);
            if (txCount.value() != 0) throw new IllegalArgumentException("headers entry transaction count must be zero");
            offset += txCount.encodedLength();
            headers.add(BlockHeader.parse(raw));
        }
        if (offset != payload.length) throw new IllegalArgumentException("trailing bytes in headers payload");
        return List.copyOf(headers);
    }

    private static void writeInt32(ByteArrayOutputStream out, int value) {
        write(out, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
    }

    private static void write(ByteArrayOutputStream out, byte[] bytes) { out.writeBytes(bytes); }
}
