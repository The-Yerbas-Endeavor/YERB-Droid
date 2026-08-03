package org.yerbas.wallet.core;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** BIP39 seed derivation from an already validated mnemonic sentence. */
public final class WalletSeed {
    private WalletSeed() {}

    public static byte[] fromMnemonic(String mnemonic, String passphrase) {
        if (mnemonic == null || mnemonic.isBlank()) throw new IllegalArgumentException("mnemonic is required");
        String normalizedMnemonic = Normalizer.normalize(mnemonic.trim().replaceAll("\\s+", " "), Normalizer.Form.NFKD);
        String normalizedPassphrase = Normalizer.normalize(passphrase == null ? "" : passphrase, Normalizer.Form.NFKD);
        char[] password = normalizedMnemonic.toCharArray();
        byte[] salt = ("mnemonic" + normalizedPassphrase).getBytes(StandardCharsets.UTF_8);
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, 2048, 512);
            try {
                return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (Exception failure) {
            throw new IllegalStateException("BIP39 seed derivation failed", failure);
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(salt, (byte) 0);
        }
    }
}
