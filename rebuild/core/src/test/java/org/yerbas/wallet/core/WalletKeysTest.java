package org.yerbas.wallet.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

final class WalletKeysTest {
    @Test
    void matchesBip39SeedVector() {
        byte[] seed = WalletSeed.fromMnemonic(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
                "TREZOR");
        assertEquals(
                "c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e5349553"
                        + "1f09a6987599d18264c1e1c92f2cf141630c7a3c4ab7c81b2f001698e7463b04",
                HexFormat.of().formatHex(seed));
    }

    @Test
    void matchesBip32MasterPrivateKeyVector() {
        byte[] seed = HexFormat.of().parseHex("000102030405060708090a0b0c0d0e0f");
        ExtendedPrivateKey master = ExtendedPrivateKey.master(seed);
        assertArrayEquals(
                HexFormat.of().parseHex("e8f32e723decf4051aefac8e2c93c9c9b214313817cdb01a1494b917c8436b35"),
                master.privateKeyBytes());
    }

    @Test
    void derivesStableYerbasReceiveAndChangeAddresses() {
        byte[] seed = WalletSeed.fromMnemonic(
                "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about", "");
        YerbasWalletKeys wallet = YerbasWalletKeys.fromSeed(seed);
        String receive0 = wallet.receiveAddress(0);
        String receive0Again = wallet.receiveAddress(0);
        String receive1 = wallet.receiveAddress(1);
        String change0 = wallet.changeAddress(0);

        assertEquals(receive0, receive0Again);
        assertNotEquals(receive0, receive1);
        assertNotEquals(receive0, change0);
        assertTrue(receive0.startsWith("y"), receive0);
    }
}
