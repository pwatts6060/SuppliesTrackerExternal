package com.suppliestracker;

import com.google.inject.Singleton;
import javax.inject.Inject;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.gameval.ItemID;

@Singleton
public class ElementalTomes
{

	private final SuppliesTrackerPlugin plugin;

	public static boolean isPage(int id)
	{
		if (id == ItemID.WINT_SEARING_PAGE) {
			return true;
		}

		for (Tome t : Tome.values) {
			if (t.pageId == id) {
				return true;
			}
		}
		return false;
	}

	public enum Tome
	{
		FIRE(ItemID.TOME_OF_FIRE, ItemID.WINT_BURNT_PAGE, 99, 126, 129, 155, 1464),
		EARTH(ItemID.TOME_OF_EARTH, ItemID.SOILED_PAGE, 96, 123, 138, 164, 1461),
		WATER(ItemID.TOME_OF_WATER, ItemID.SOAKED_PAGE, 93, 120, 135, 161, 1458),
		;

		private final int tomeId;
		private final int pageId;
		private final int[] spellIds;

		Tome(int tomeId, int pageId, int... spellIds)
		{
			this.tomeId = tomeId;
			this.pageId = pageId;
			this.spellIds = spellIds;
		}

		static final Tome[] values = Tome.values();
	}

	@Inject
	ElementalTomes(SuppliesTrackerPlugin plugin) {
		this.plugin = plugin;
	}

	public void graphicChange(GraphicChanged event)
	{
		ItemContainer equip = plugin.client.getItemContainer(InventoryID.WORN);
		if (equip == null)
			return;

		Item shield = equip.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx());
		if (shield == null)
			return;

		for (int i = 0; i < Tome.values.length; i++) {
			if (Tome.values[i].tomeId == shield.getId()) {
				checkTomeUsage(Tome.values[i], event);
				break;
			}
		}
	}

	private void checkTomeUsage(Tome tome, GraphicChanged event)
	{
		for (int gfxId : tome.spellIds) {
			if (event.getActor().hasSpotAnim(gfxId)) {
				if (plugin.getConfig().fireTomeUsesSearing() && tome == Tome.FIRE) {
					plugin.buildEntries(ItemID.WINT_SEARING_PAGE);
				} else {
					plugin.buildEntries(tome.pageId);
				}
			}
		}
	}
}
