package org.yerbas.wallet.core;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;

/** Base58Check encoding for Yerbas addresses and extended keys. */
public final class Base58Check {
    private static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

    private Base58Check() {}

    public static String encode(byte[] payload) {
        if (payload == null || payload.length == 0) throw new IllegalArgumentException("payload is required");
        byte[] checksum = Arrays.copyOf(doubleSha256(payload), 4);
        byte[] all = new byte[payload.length + checksum.length];
        System.arraycopy(payload, 0, all, 0, payload.length);
        System.arraycopy(checksum, 0, all, payload.length, checksum.length);

        BigInteger value = new BigInteger(1, all);
        StringBuilder out = new StringBuilder();
        BigInteger base = BigInteger.valueOf(58);
        while (value.signum() > 0) {
            BigInteger[] qr = value.divideAndRemainder(base);
            out.append(ALPHABET[qr[1].intValue()]);
            value = qr[0];
        }
        for (byte b : all) {
            if (b != 0) break;
            out.append('1');
        }
        return out.reverse().toString();
    }

    static byte[] doubleSha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(digest.digest(input));
        } catch (Exception failure) {
            throw new IllegalStateException("SHA-256 unavailable", failure);
        }
    }
}
