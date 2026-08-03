package org.yerbas.wallet.core;

import java.util.NavigableMap;
import java.util.TreeMap;

/** Immutable Yerbas mainnet checkpoints copied from current Yerbas Core. */
public final class CheckpointStore {
    private final NavigableMap<Integer, String> checkpoints;

    private CheckpointStore(NavigableMap<Integer, String> checkpoints) {
        this.checkpoints = new TreeMap<>(checkpoints);
    }

    public static CheckpointStore mainnet() {
        NavigableMap<Integer, String> values = new TreeMap<>();
        values.put(0, "eff0bbe5c1bbe1ef8da54822a18f528d6dc58232990bdb86e0a77ab2814ed12c");
        values.put(1420, "9cf529c09eaf90f1b1b96c681c203aa80e1840b1709c928ab6e840b562d54c34");
        values.put(8837, "25c1c019d70e6990d3ed680fa9703cb84d620008a6cb8f635bbcdcef913dfbae");
        values.put(16460, "6cb79413f86770f856556ce641e15beb45656915db9ce4511cc9d4853b532fe5");
        values.put(42069, "729ee24deac4d1df060191debdf52079a70987dc2001e058002641a1412d3782");
        values.put(108069, "15280a0f159f2739a3e6658bbc68534cefa79b14f5a250e1e4f724b5b3985998");
        values.put(209639, "c99ce3a58ba3828a1a09469d0afedb91e8238e6cb4fd2bc3970c9ff56bbbb528");
        values.put(310420, "5e36ff7864c6ef90a165d28b702884fa1c91be4ffea3111d9fbc3724fa6f410f");
        values.put(410420, "b21e783ad0b134010e08f1641e425fc4cf74db586bf928ca4ed202da21c50be5");
        values.put(811220, "be1c1caf6166786435a81c81b61d809f98823641dbe64b15516ad59b3e4bce10");
        values.put(1060820, "f70aa712f4d467d4e28e431ac2b8851c37e9335669075b4c25f072b1b032feb0");
        return new CheckpointStore(values);
    }

    public boolean verify(int height, String displayedHash) {
        String expected = checkpoints.get(height);
        return expected == null || expected.equalsIgnoreCase(displayedHash);
    }

    public int latestHeight() {
        return checkpoints.lastKey();
    }

    public String hashAt(int height) {
        return checkpoints.get(height);
    }
}
