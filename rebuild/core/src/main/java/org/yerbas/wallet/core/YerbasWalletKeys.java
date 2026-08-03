package org.yerbas.wallet.core;

/** Yerbas BIP44 derivation and mainnet address creation. */
public final class YerbasWalletKeys {
    public static final int COIN_TYPE = 200;
    public static final int PUBKEY_ADDRESS_PREFIX = 140;

    private final ExtendedPrivateKey account;

    private YerbasWalletKeys(ExtendedPrivateKey account) {
        this.account = account;
    }

    public static YerbasWalletKeys fromSeed(byte[] seed) {
        ExtendedPrivateKey key = ExtendedPrivateKey.master(seed)
                .derive(44, true)
                .derive(COIN_TYPE, true)
                .derive(0, true);
        return new YerbasWalletKeys(key);
    }

    public ExtendedPrivateKey receiveKey(int index) {
        if (index < 0) throw new IllegalArgumentException("index must be non-negative");
        return account.derive(0, false).derive(index, false);
    }

    public ExtendedPrivateKey changeKey(int index) {
        if (index < 0) throw new IllegalArgumentException("index must be non-negative");
        return account.derive(1, false).derive(index, false);
    }

    public String receiveAddress(int index) {
        return address(receiveKey(index));
    }

    public String changeAddress(int index) {
        return address(changeKey(index));
    }

    private static String address(ExtendedPrivateKey key) {
        byte[] hash = key.publicKeyHash();
        byte[] payload = new byte[21];
        payload[0] = (byte) PUBKEY_ADDRESS_PREFIX;
        System.arraycopy(hash, 0, payload, 1, hash.length);
        return Base58Check.encode(payload);
    }
}
