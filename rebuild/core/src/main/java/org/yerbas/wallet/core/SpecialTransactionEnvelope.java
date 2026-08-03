package org.yerbas.wallet.core;

import java.util.Arrays;

/** Preserves the Dash/Yerbas special-transaction extra payload without interpreting type-specific fields. */
public record SpecialTransactionEnvelope(int baseVersion, int type, byte[] extraPayload) {
    public SpecialTransactionEnvelope {
        if (baseVersion < 0 || baseVersion > 0xffff) throw new IllegalArgumentException("Invalid base version");
        if (type < 0 || type > 0xffff) throw new IllegalArgumentException("Invalid transaction type");
        extraPayload = extraPayload == null ? new byte[0] : extraPayload.clone();
        if (type == 0 && extraPayload.length != 0) {
            throw new IllegalArgumentException("Normal transactions cannot carry an extra payload");
        }
    }

    @Override
    public byte[] extraPayload() {
        return extraPayload.clone();
    }

    public int packedVersion() {
        return (type << 16) | baseVersion;
    }

    public byte[] serializeExtraPayload() {
        if (type == 0) return new byte[0];
        byte[] length = CompactSize.write(extraPayload.length);
        byte[] result = Arrays.copyOf(length, length.length + extraPayload.length);
        System.arraycopy(extraPayload, 0, result, length.length, extraPayload.length);
        return result;
    }

    public static SpecialTransactionEnvelope fromPackedVersion(int packedVersion, byte[] serializedPayload) {
        int baseVersion = packedVersion & 0xffff;
        int type = (packedVersion >>> 16) & 0xffff;
        if (type == 0) {
            if (serializedPayload != null && serializedPayload.length != 0) {
                throw new IllegalArgumentException("Unexpected payload on normal transaction");
            }
            return new SpecialTransactionEnvelope(baseVersion, 0, new byte[0]);
        }
        if (serializedPayload == null || serializedPayload.length == 0) {
            throw new IllegalArgumentException("Missing special transaction payload");
        }
        CompactSize.Decoded length = CompactSize.read(serializedPayload, 0);
        long end = (long) length.encodedLength() + length.value();
        if (end != serializedPayload.length || length.value() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Malformed special transaction payload");
        }
        return new SpecialTransactionEnvelope(baseVersion, type,
                Arrays.copyOfRange(serializedPayload, length.encodedLength(), serializedPayload.length));
    }
}
