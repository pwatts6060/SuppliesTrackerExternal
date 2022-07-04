package com.suppliestracker;

import net.runelite.api.ItemID;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Singleton
public final class Bait {
    private final SuppliesTrackerPlugin plugin;
    private static final Set<Integer> baitIds = new LinkedHashSet<>(Arrays.asList(
            ItemID.FEATHER,
            ItemID.STRIPY_FEATHER,
            ItemID.SPIRIT_FLAKES,
            ItemID.RAW_KARAMBWANJI,
            ItemID.FISHING_BAIT,
            ItemID.DARK_FISHING_BAIT,
            ItemID.SANDWORMS
    ));

    @Inject
    Bait(SuppliesTrackerPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isBait(int id) {
        return baitIds.contains(id);
    }

    public void onXpDrop() {
        for (int id : baitIds) {
            if (plugin.changedItems.getOrDefault(id, 0) == -1) {
                plugin.buildEntries(id);
            }
        }
    }
}
