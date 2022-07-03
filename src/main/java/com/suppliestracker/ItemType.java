/*
 * Copyright (c) 2018, Daddy Dozer <https://github.com/Dyldozer>
 * Copyright (c) 2018, Davis Cook <daviscook447@gmail.com>
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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static net.runelite.api.ItemID.BLADE_OF_SAELDOR;
import static net.runelite.api.ItemID.COINS_995;
import static net.runelite.api.ItemID.HEALER_ICON_20802;
import static net.runelite.api.ItemID.HEALER_ICON_22308;
import static net.runelite.api.ItemID.IBANS_STAFF;
import static net.runelite.api.ItemID.SANGUINESTI_STAFF;
import static net.runelite.api.ItemID.SCYTHE_OF_VITUR;
import static net.runelite.api.ItemID.TRIDENT_OF_THE_SEAS;
import static net.runelite.api.ItemID.TRIDENT_OF_THE_SWAMP;

/**
 * The potential types that supplies can be along with a categorization function
 * that assigns the supplies to these categories
 */
@AllArgsConstructor
public enum ItemType
{
	FOOD("Food"),
	POTION("Potions"),
	RUNE("Runes"),
	AMMO("Ammo"),
	TELEPORT("Teleports"),
	COINS("Coins"),
	JEWELLERY("Jewellery"),
	CHARGES("Charges"),
	FARMING("Farming"),
	DEATH("Deaths"),
	PRAYER("Prayer");


	@Getter(AccessLevel.PUBLIC)
	private String label;

	/**
	 * Takes an item and determines what ItemType it should categorize into
	 *
	 * @param item the item to determine category for
	 * @return our best guess for what category this item goes into
	 * note that if the guess is wrong (per say) it won't break anything because it will be
	 * consistently wrong but it could have an item that is clearly not food in the food section
	 */
	public static ItemType categorize(SuppliesTrackerItem item)
	{
		String name = item.getName().toLowerCase();
		if (name.endsWith("(4)"))
		{
			return POTION;
		}
		else if ((name.contains("bones") && !name.contains(" to ")) || name.contains("ensouled"))
		{
			return PRAYER;
		}
		else if (name.contains("bolt") || name.contains("dart")
			|| name.contains(" arrow") || name.contains("javelin")
			|| name.contains("knive") || name.contains("throwing")
			|| name.contains("zulrah's scale") || name.contains("cannonball")
			|| name.contains("knife")|| name.contains("chinchompa")
			|| name.contains("thrownaxe"))
		{
			return AMMO;
		}
		else if (name.contains("rune"))
		{
			return RUNE;
		}
		else if (name.contains("teleport"))
		{
			return TELEPORT;
		}
		else if (item.getId() == COINS_995)
		{
			return COINS;
		}
		else if (name.contains("ring of") || name.contains("amulet") ||
				name.contains("bracelet") || name.contains("necklace"))
		{
			return JEWELLERY;
		}
		else if (name.contains(" sapling") || name.contains(" seed") ||
				name.contains("compost") || name.contains("plant cure"))
		{
			return FARMING;
		}
		else if (item.getId() == SCYTHE_OF_VITUR || item.getId() == SANGUINESTI_STAFF ||
				item.getId() == TRIDENT_OF_THE_SEAS || item.getId() == TRIDENT_OF_THE_SWAMP ||
				item.getId() == BLADE_OF_SAELDOR || item.getId() == IBANS_STAFF)
		{
			return CHARGES;
		}
		else if (item.getId() == HEALER_ICON_20802 || item.getId() == HEALER_ICON_22308)
		{
			return DEATH;
		}
		return FOOD;
	}
}
