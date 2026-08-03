package org.yerbas.wallet.core;

import java.util.ArrayList;
import java.util.List;

/** Builds Bitcoin-family exponentially stepped block locators from stored headers. */
public final class BlockLocator {
    private BlockLocator() {}

    public static List<byte[]> build(HeaderStore store) {
        HeaderStore.StoredHeader cursor = store.best()
                .orElseThrow(() -> new IllegalStateException("header store has no best tip"));
        List<byte[]> locator = new ArrayList<>();
        int step = 1;
        int emitted = 0;

        while (true) {
            locator.add(cursor.header().blockId());
            if (cursor.height() == 0) break;

            int targetHeight = Math.max(0, cursor.height() - step);
            while (cursor.height() > targetHeight) {
                cursor = store.find(cursor.header().previousBlock())
                        .orElseThrow(() -> new IllegalStateException("missing parent while building locator"));
            }
            emitted++;
            if (emitted >= 10) step = Math.multiplyExact(step, 2);
        }
        return List.copyOf(locator);
    }
}
