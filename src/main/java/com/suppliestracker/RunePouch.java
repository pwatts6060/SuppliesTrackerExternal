package com.suppliestracker;

import com.suppliestracker.Skills.XpDropTracker;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;

@Singleton
public class RunePouch
{
	private final SuppliesTrackerPlugin plugin;

	//Rune pouch stuff
	private static final int[] AMOUNT_VARBITS =
		{
			VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2,
			VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4
		};
	private static final int[] RUNE_VARBITS =
		{
			VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2,
			VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4
		};

	private final int[] pouchAmount;
	private final int[] pouchRuneIds;
	private final int[] prevPouchRuneIds;
	private final int[] amounts_used;

	@Inject
	RunePouch(SuppliesTrackerPlugin plugin) {
		this.plugin = plugin;
		pouchAmount = new int[RUNE_VARBITS.length];
		pouchRuneIds = new int[RUNE_VARBITS.length];
		prevPouchRuneIds = new int[RUNE_VARBITS.length];
		amounts_used = new int[RUNE_VARBITS.length];
	}

	/**
	 * Checks local variable data against client data then returns differences then updates local to client
	 */
	void updateVarbit(int varbitId)
	{
		for (int i = 0; i < RUNE_VARBITS.length; i++) {
			if (RUNE_VARBITS[i] == varbitId) {
				prevPouchRuneIds[i] = pouchRuneIds[i];
				pouchRuneIds[i] = plugin.client.getVarbitValue(RUNE_VARBITS[i]);
				return;
			}
		}

		for (int i = 0; i < AMOUNT_VARBITS.length; i++) {
			if (AMOUNT_VARBITS[i] == varbitId) {
				int newAmount = plugin.client.getVarbitValue(AMOUNT_VARBITS[i]);
				if (newAmount < pouchAmount[i])
				{
					amounts_used[i] += pouchAmount[i] - newAmount;
				}
				pouchAmount[i] = newAmount;
				return;
			}
		}
	}

	void checkUsedRunePouch(XpDropTracker xpDropTracker, boolean noXpCast)
	{
		if (!xpDropTracker.hadXpThisTick(Skill.MAGIC) && !noXpCast) {
			return;
		}
		for (int i = 0; i < pouchRuneIds.length; i++) {
			if (amounts_used[i] != 0 && amounts_used[i] < 20)
			{
				if (pouchRuneIds[i] == 0) {
					// special case when last runes used from pouch
					plugin.buildEntries(Runes.getRune(prevPouchRuneIds[i]).getItemId(), amounts_used[i]);
				} else {
					plugin.buildEntries(Runes.getRune(pouchRuneIds[i]).getItemId(), amounts_used[i]);
				}
			}
		}
	}

	public void resetAmountUsed()
	{
		Arrays.fill(amounts_used, 0);
	}
}
