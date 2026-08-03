package org.yerbas.wallet.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/** Standard 80-byte Yerbas block header. */
public record BlockHeader(
        int version,
        byte[] previousBlock,
        byte[] merkleRoot,
        long timestamp,
        long compactTarget,
        long nonce) {

    public static final int SERIALIZED_SIZE = 80;

    public BlockHeader {
        if (previousBlock.length != 32 || merkleRoot.length != 32) {
            throw new IllegalArgumentException("Header hashes must be 32 bytes");
        }
        previousBlock = previousBlock.clone();
        merkleRoot = merkleRoot.clone();
        requireUnsigned32(timestamp, "timestamp");
        requireUnsigned32(compactTarget, "compactTarget");
        requireUnsigned32(nonce, "nonce");
    }

    @Override
    public byte[] previousBlock() {
        return previousBlock.clone();
    }

    @Override
    public byte[] merkleRoot() {
        return merkleRoot.clone();
    }

    public static BlockHeader parse(byte[] bytes) {
        if (bytes.length != SERIALIZED_SIZE) {
            throw new IllegalArgumentException("Yerbas headers must be exactly 80 bytes");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int version = buffer.getInt();
        byte[] previous = new byte[32];
        byte[] merkle = new byte[32];
        buffer.get(previous);
        buffer.get(merkle);
        return new BlockHeader(
                version,
                previous,
                merkle,
                Integer.toUnsignedLong(buffer.getInt()),
                Integer.toUnsignedLong(buffer.getInt()),
                Integer.toUnsignedLong(buffer.getInt()));
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(SERIALIZED_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(version);
        buffer.put(previousBlock);
        buffer.put(merkleRoot);
        buffer.putInt((int) timestamp);
        buffer.putInt((int) compactTarget);
        buffer.putInt((int) nonce);
        return buffer.array();
    }

    /** Wire-order double-SHA256 block identifier. */
    public byte[] blockId() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(digest.digest(serialize()));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public boolean linksTo(BlockHeader parent) {
        return Arrays.equals(previousBlock, parent.blockId());
    }

    private static void requireUnsigned32(long value, String field) {
        if (value < 0 || value > 0xffff_ffffL) {
            throw new IllegalArgumentException(field + " must fit uint32");
        }
    }
}
