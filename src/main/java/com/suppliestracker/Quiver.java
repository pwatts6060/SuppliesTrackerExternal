package com.suppliestracker;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Item;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.game.ItemVariationMapping;

public class Quiver
{
	private final SuppliesTrackerPlugin plugin;
	private int quiverAmmoId;
	private int quiverAmmoCount;

	private static final Set<Integer> DIZANAS_QUIVER_IDS = ImmutableSet.<Integer>builder()
		.addAll(ItemVariationMapping.getVariations(ItemVariationMapping.map(ItemID.DIZANAS_QUIVER_CHARGED)))
		.addAll(ItemVariationMapping.getVariations(ItemVariationMapping.map(ItemID.DIZANAS_QUIVER_INFINITE)))
		.addAll(ItemVariationMapping.getVariations(ItemVariationMapping.map(ItemID.SKILLCAPE_MAX_DIZANAS)))
		.build();

	@Inject
	Quiver(SuppliesTrackerPlugin plugin) {
		this.plugin = plugin;
		quiverAmmoId = -1;
		quiverAmmoCount = 0;
	}

	public void updateVarp(int varpId)
	{
		if (varpId != VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT && varpId != VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO)
		{
			return;
		}

		int oldAmmoId = quiverAmmoId;
		int oldAmmoCount = quiverAmmoCount;
		quiverAmmoId = plugin.client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO);
		quiverAmmoCount = plugin.client.getVarpValue(VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT);

		if (quiverAmmoId < 0 && oldAmmoId > 0) {
			// ammo is now missing, either deposited into bank/withdraw into inv, or last arrows fired
			if (oldAmmoCount > 0 && oldAmmoCount <= 2) { // assume arrow fired if was only 1-2 ammo (dark bow can fire 2 at a time)
				plugin.buildEntries(oldAmmoId, oldAmmoCount);
				// there is a bug where withdrawing a 1-2 ammo from quiver will track as being fired, uncommon wontfix
			}
		} else if (quiverAmmoId >= 0 && quiverAmmoId == oldAmmoId) {
			int countChange = oldAmmoCount - quiverAmmoCount;
			if (countChange >= 0 && countChange <= 2) {
				plugin.buildEntries(quiverAmmoId, countChange);
			}
		}
	}
}
