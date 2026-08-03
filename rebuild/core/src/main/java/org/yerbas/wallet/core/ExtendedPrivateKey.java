package org.yerbas.wallet.core;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.math.ec.ECPoint;

/** Minimal BIP32 private-key implementation for Yerbas wallet derivation. */
public final class ExtendedPrivateKey {
    private static final BigInteger CURVE_N = SECNamedCurves.getByName("secp256k1").getN();
    private static final ECPoint G = SECNamedCurves.getByName("secp256k1").getG();

    private final BigInteger privateKey;
    private final byte[] chainCode;

    private ExtendedPrivateKey(BigInteger privateKey, byte[] chainCode) {
        if (privateKey.signum() <= 0 || privateKey.compareTo(CURVE_N) >= 0) throw new IllegalArgumentException("invalid private key");
        if (chainCode.length != 32) throw new IllegalArgumentException("chain code must be 32 bytes");
        this.privateKey = privateKey;
        this.chainCode = chainCode.clone();
    }

    public static ExtendedPrivateKey master(byte[] seed) {
        byte[] material = hmacSha512("Bitcoin seed".getBytes(java.nio.charset.StandardCharsets.US_ASCII), seed);
        BigInteger key = new BigInteger(1, Arrays.copyOfRange(material, 0, 32));
        return new ExtendedPrivateKey(key, Arrays.copyOfRange(material, 32, 64));
    }

    public ExtendedPrivateKey derive(int index, boolean hardened) {
        long child = Integer.toUnsignedLong(index) | (hardened ? 0x8000_0000L : 0L);
        byte[] data = new byte[37];
        if (hardened) {
            data[0] = 0;
            System.arraycopy(privateKeyBytes(), 0, data, 1, 32);
        } else {
            System.arraycopy(publicKeyCompressed(), 0, data, 0, 33);
        }
        ByteBuffer.wrap(data, 33, 4).order(ByteOrder.BIG_ENDIAN).putInt((int) child);
        byte[] material = hmacSha512(chainCode, data);
        BigInteger left = new BigInteger(1, Arrays.copyOfRange(material, 0, 32));
        BigInteger derived = left.add(privateKey).mod(CURVE_N);
        if (left.compareTo(CURVE_N) >= 0 || derived.signum() == 0) throw new IllegalStateException("invalid BIP32 child; increment index");
        return new ExtendedPrivateKey(derived, Arrays.copyOfRange(material, 32, 64));
    }

    public byte[] privateKeyBytes() { return unsigned32(privateKey); }

    public byte[] publicKeyCompressed() { return G.multiply(privateKey).normalize().getEncoded(true); }

    public byte[] publicKeyHash() {
        byte[] sha;
        try { sha = java.security.MessageDigest.getInstance("SHA-256").digest(publicKeyCompressed()); }
        catch (Exception failure) { throw new IllegalStateException(failure); }
        RIPEMD160Digest ripemd = new RIPEMD160Digest();
        ripemd.update(sha, 0, sha.length);
        byte[] out = new byte[20];
        ripemd.doFinal(out, 0);
        return out;
    }

    private static byte[] hmacSha512(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key, "HmacSHA512"));
            return mac.doFinal(data);
        } catch (Exception failure) {
            throw new IllegalStateException("HMAC-SHA512 unavailable", failure);
        }
    }

    private static byte[] unsigned32(BigInteger value) {
        byte[] raw = value.toByteArray();
        if (raw.length == 33 && raw[0] == 0) raw = Arrays.copyOfRange(raw, 1, 33);
        if (raw.length > 32) throw new IllegalArgumentException("value exceeds 256 bits");
        byte[] out = new byte[32];
        System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
        return out;
    }
}
