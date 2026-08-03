package org.yerbas.wallet.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/** Yerbas P2P message framing: magic, 12-byte command, payload length and checksum. */
public record WireMessage(String command, byte[] payload) {
    public static final int HEADER_SIZE = 24;
    public static final int MAX_PAYLOAD_SIZE = 32 * 1024 * 1024;

    public WireMessage {
        if (command == null || command.isBlank() || command.length() > 12) {
            throw new IllegalArgumentException("Invalid command");
        }
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c < 0x20 || c > 0x7e) throw new IllegalArgumentException("Command must be printable ASCII");
        }
        payload = payload == null ? new byte[0] : payload.clone();
        if (payload.length > MAX_PAYLOAD_SIZE) throw new IllegalArgumentException("Payload too large");
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public byte[] serialize(NetworkParameters network) {
        byte[] result = new byte[HEADER_SIZE + payload.length];
        System.arraycopy(network.messageStart(), 0, result, 0, 4);
        byte[] commandBytes = command.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(commandBytes, 0, result, 4, commandBytes.length);
        ByteBuffer.wrap(result, 16, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(payload.length);
        System.arraycopy(checksum(payload), 0, result, 20, 4);
        System.arraycopy(payload, 0, result, HEADER_SIZE, payload.length);
        return result;
    }

    public static WireMessage parse(NetworkParameters network, byte[] bytes) {
        if (bytes == null || bytes.length < HEADER_SIZE) throw new IllegalArgumentException("Truncated message");
        if (!Arrays.equals(network.messageStart(), Arrays.copyOfRange(bytes, 0, 4))) {
            throw new IllegalArgumentException("Wrong Yerbas network magic");
        }
        int end = 4;
        while (end < 16 && bytes[end] != 0) end++;
        for (int i = end; i < 16; i++) if (bytes[i] != 0) throw new IllegalArgumentException("Malformed command padding");
        String command = new String(bytes, 4, end - 4, StandardCharsets.US_ASCII);
        int payloadLength = ByteBuffer.wrap(bytes, 16, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_SIZE || bytes.length != HEADER_SIZE + payloadLength) {
            throw new IllegalArgumentException("Invalid payload length");
        }
        byte[] payload = Arrays.copyOfRange(bytes, HEADER_SIZE, bytes.length);
        byte[] expected = Arrays.copyOfRange(bytes, 20, 24);
        if (!Arrays.equals(expected, checksum(payload))) throw new IllegalArgumentException("Invalid message checksum");
        return new WireMessage(command, payload);
    }

    private static byte[] checksum(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Arrays.copyOf(digest.digest(digest.digest(payload)), 4);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
