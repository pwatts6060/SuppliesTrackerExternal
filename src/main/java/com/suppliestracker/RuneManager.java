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
