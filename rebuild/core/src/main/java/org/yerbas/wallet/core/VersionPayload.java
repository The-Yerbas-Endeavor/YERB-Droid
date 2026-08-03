package org.yerbas.wallet.core;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Yerbas version-message payload codec. */
public record VersionPayload(
        int protocolVersion,
        long services,
        long timestamp,
        NetworkAddress receiver,
        NetworkAddress sender,
        long nonce,
        String userAgent,
        int startHeight,
        boolean relay) {

    public record NetworkAddress(long services, byte[] ipv6, int port) {
        public NetworkAddress {
            if (ipv6 == null || ipv6.length != 16) {
                throw new IllegalArgumentException("network address must contain 16 IP bytes");
            }
            if (port < 0 || port > 65535) throw new IllegalArgumentException("invalid port");
            ipv6 = ipv6.clone();
        }

        @Override public byte[] ipv6() { return ipv6.clone(); }
    }

    public VersionPayload {
        if (userAgent == null) throw new IllegalArgumentException("userAgent is required");
        if (userAgent.getBytes(StandardCharsets.UTF_8).length > 256) {
            throw new IllegalArgumentException("userAgent is too long");
        }
    }

    public byte[] encode() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeLE32(out, protocolVersion);
        writeLE64(out, services);
        writeLE64(out, timestamp);
        writeAddress(out, receiver);
        writeAddress(out, sender);
        writeLE64(out, nonce);
        byte[] agent = userAgent.getBytes(StandardCharsets.UTF_8);
        out.writeBytes(CompactSize.write(agent.length));
        out.writeBytes(agent);
        writeLE32(out, startHeight);
        out.write(relay ? 1 : 0);
        return out.toByteArray();
    }

    public static VersionPayload decode(byte[] payload) {
        if (payload == null || payload.length < 85) {
            throw new IllegalArgumentException("truncated version payload");
        }
        ByteBuffer in = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        int protocol = in.getInt();
        long services = in.getLong();
        long timestamp = in.getLong();
        NetworkAddress receiver = readAddress(in);
        NetworkAddress sender = readAddress(in);
        long nonce = in.getLong();
        int compactOffset = in.position();
        CompactSize.Decoded size = CompactSize.read(payload, compactOffset);
        if (size.value() > 256) throw new IllegalArgumentException("userAgent is too long");
        int agentStart = compactOffset + size.encodedLength();
        int agentEnd = Math.addExact(agentStart, Math.toIntExact(size.value()));
        if (agentEnd + 4 > payload.length) throw new IllegalArgumentException("truncated version userAgent");
        String userAgent = new String(payload, agentStart, agentEnd - agentStart, StandardCharsets.UTF_8);
        ByteBuffer tail = ByteBuffer.wrap(payload, agentEnd, payload.length - agentEnd).order(ByteOrder.LITTLE_ENDIAN);
        int startHeight = tail.getInt();
        boolean relay = !tail.hasRemaining() || tail.get() != 0;
        if (tail.hasRemaining()) throw new IllegalArgumentException("trailing version payload bytes");
        return new VersionPayload(protocol, services, timestamp, receiver, sender, nonce, userAgent, startHeight, relay);
    }

    private static void writeAddress(ByteArrayOutputStream out, NetworkAddress address) {
        writeLE64(out, address.services());
        out.writeBytes(address.ipv6());
        out.write((address.port() >>> 8) & 0xff);
        out.write(address.port() & 0xff);
    }

    private static NetworkAddress readAddress(ByteBuffer in) {
        long services = in.getLong();
        byte[] ip = new byte[16];
        in.get(ip);
        int port = (Byte.toUnsignedInt(in.get()) << 8) | Byte.toUnsignedInt(in.get());
        return new NetworkAddress(services, ip, port);
    }

    private static void writeLE32(ByteArrayOutputStream out, int value) {
        out.writeBytes(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
    }

    private static void writeLE64(ByteArrayOutputStream out, long value) {
        out.writeBytes(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array());
    }

    public static NetworkAddress unspecified(int port) {
        return new NetworkAddress(0, new byte[16], port);
    }
}
