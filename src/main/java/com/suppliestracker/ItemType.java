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

import static com.suppliestracker.SuppliesTrackerPlugin.runeIds;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static net.runelite.api.ItemID.*;

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
	PRAYER("Prayer"),
	BAIT("Bait"),
	;


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
		int itemId = item.getId();
		if (name.endsWith("(4)"))
		{
			return POTION;
		}
		else if ((name.contains("bones") && !name.contains(" to ")) || name.startsWith("ensouled") || name.endsWith(" ashes"))
		{
			return PRAYER;
		}
		else if (name.contains("bolt") || name.contains("dart")
			|| name.contains(" arrow") || name.contains("javelin")
			|| name.contains("knive") || name.contains("throwing")
			|| name.contains("zulrah's scale") || name.contains("cannonball")
			|| name.contains("knife")|| name.contains("chinchompa")
			|| name.endsWith(" brutal") || name.contains("thrownaxe")
			|| item.getId() == REVENANT_ETHER)
		{
			return AMMO;
		}
		else if (runeIds.contains(itemId))
		{
			return RUNE;
		}
		else if (name.contains("teleport") || itemId == STONY_BASALT || itemId == ICY_BASALT)
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
				name.contains("compost") || name.contains("plant cure") || name.contains(" spore"))
		{
			return FARMING;
		}
		else if (item.getId() == SCYTHE_OF_VITUR || item.getId() == SANGUINESTI_STAFF ||
				item.getId() == TRIDENT_OF_THE_SEAS || item.getId() == TRIDENT_OF_THE_SWAMP ||
				item.getId() == BLADE_OF_SAELDOR || item.getId() == IBANS_STAFF ||
				item.getId() == TUMEKENS_SHADOW || item.getId() == VENATOR_BOW || item.getId() == ANCIENT_ESSENCE ||
		 		item.getId() == THAMMARONS_SCEPTRE || item.getId() == CRAWS_BOW ||
				item.getId() == VIGGORAS_CHAINMACE || item.getId() == ACCURSED_SCEPTRE || item.getId() == WEBWEAVER_BOW ||
				item.getId() == URSINE_CHAINMACE || item.getId() == ACCURSED_SCEPTRE_A)
		{
			return CHARGES;
		}
		else if (item.getId() == HEALER_ICON_20802 || item.getId() == HEALER_ICON_22308)
		{
			return DEATH;
		}
		else if (Bait.isBait(item.getId()))
		{
			return BAIT;
		}
		return FOOD;
	}
}
