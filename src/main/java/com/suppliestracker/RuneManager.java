package com.suppliestracker;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

import java.util.Set;

import static net.runelite.api.ItemID.RUNE_POUCH;
import static net.runelite.api.ItemID.RUNE_POUCH_23650;
import static net.runelite.api.ItemID.RUNE_POUCH_L;

public class RuneManager {

    private static final Set<Integer> runePouchIds = ImmutableSet.of(RUNE_POUCH, RUNE_POUCH_23650, RUNE_POUCH_L);

    public boolean hasRunePouch(ItemContainer invContainer) {
        for (Item item : invContainer.getItems())
        {
            if (runePouchIds.contains(item.getId()))
            {
                return true;
            }
        }
        return false;
    }
}
