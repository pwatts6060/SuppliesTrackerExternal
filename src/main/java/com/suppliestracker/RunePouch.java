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

	private final int[] OLD_AMOUNT_VARBITS = new int[RUNE_VARBITS.length];
	private final int[] OLD_RUNE_VARBITS = new int[RUNE_VARBITS.length];
	private final int[] runes = new int[RUNE_VARBITS.length];
	private final int[] amounts_used = new int[RUNE_VARBITS.length];

	@Inject
	RunePouch(SuppliesTrackerPlugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * Checks local variable data against client data then returns differences then updates local to client
	 */
	void updateRunePouch()
	{
		for (int i = 0; i < amounts_used.length; i++) {
			//check amounts
			if (OLD_AMOUNT_VARBITS[i] != plugin.client.getVarbitValue(AMOUNT_VARBITS[i]))
			{
				if (OLD_AMOUNT_VARBITS[i] > plugin.client.getVarbitValue(AMOUNT_VARBITS[i]))
				{
					amounts_used[i] += OLD_AMOUNT_VARBITS[i] - plugin.client.getVarbitValue(AMOUNT_VARBITS[i]);
				}
				OLD_AMOUNT_VARBITS[i] = plugin.client.getVarbitValue(AMOUNT_VARBITS[0]);
			}

			//check runes
			if (OLD_RUNE_VARBITS[i] != plugin.client.getVarbitValue(RUNE_VARBITS[i]))
			{
				runes[i] = plugin.client.getVarbitValue(RUNE_VARBITS[i]);
				OLD_RUNE_VARBITS[i] = plugin.client.getVarbitValue(RUNE_VARBITS[i]);
			}
		}
	}

	void checkUsedRunePouch(XpDropTracker xpDropTracker, boolean noXpCast)
	{
		if (!xpDropTracker.hadXpThisTick(Skill.MAGIC) && !noXpCast) {
			return;
		}
		for (int i = 0; i < runes.length; i++) {
			if (amounts_used[i] != 0 && amounts_used[i] < 20)
			{
				plugin.buildEntries(Runes.getRune(runes[i]).getItemId(), amounts_used[i]);
			}
		}
	}

	public void resetAmountUsed()
	{
		Arrays.fill(amounts_used, 0);
	}
}
