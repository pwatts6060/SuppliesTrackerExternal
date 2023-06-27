/*
 * Copyright (c) 2022, Patrick <https://github.com/pwatts6060>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *	list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	this list of conditions and the following disclaimer in the documentation
 *	and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.suppliestracker;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import net.runelite.api.ItemID;

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
