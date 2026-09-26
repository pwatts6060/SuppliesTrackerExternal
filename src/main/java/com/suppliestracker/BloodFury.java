package com.suppliestracker;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Tracks charges used by the amulet of blood fury, which loses one charge per successful melee hit.
 * The game keeps the remaining charges in a varbit, so the difference between updates is the
 * number of charges used, including successful hits of 0 that can't be told apart from misses.
 */
@Singleton
public class BloodFury
{
	// a single attack can use several charges (e.g. scythe), anything larger is a recharge or login sync
	static final int MAX_CHARGES_PER_UPDATE = 10;

	private final SuppliesTrackerPlugin plugin;
	private int charges = -1;

	@Inject
	BloodFury(SuppliesTrackerPlugin plugin)
	{
		this.plugin = plugin;
	}

	public void updateVarbit(int varbitId)
	{
		if (varbitId != VarbitID.CHARGES_BLOOD_FURY_QUANTITY)
		{
			return;
		}

		int oldCharges = charges;
		charges = plugin.client.getVarbitValue(VarbitID.CHARGES_BLOOD_FURY_QUANTITY);

		if (oldCharges < 0)
		{
			return;
		}

		int used = oldCharges - charges;
		if (used > 0 && used <= MAX_CHARGES_PER_UPDATE)
		{
			plugin.buildChargesEntries(ItemID.BLOOD_AMULET, used);
		}
	}

	/**
	 * Forget the known charge count, so the next update after logging in is used as a baseline
	 */
	public void reset()
	{
		charges = -1;
	}
}
