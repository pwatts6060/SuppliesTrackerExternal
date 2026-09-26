package com.suppliestracker;

import com.suppliestracker.Skills.XpDropTracker;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.util.Text;

/**
 * Tracks charges used by the amulet of blood fury, which loses one charge per successful melee hit.
 * The game doesn't expose the remaining charges live, so hits are counted as they land and the count
 * is corrected whenever the amulet's remaining charges are shown in chat (e.g. from "Check"), since
 * successful hits of 0 can't be told apart from misses.
 */
@Singleton
public class BloodFury
{
	private static final Pattern CHARGES_MESSAGE = Pattern.compile("(?:will work for|can perform) ([\\d,]+) more hits?");
	// a melee hitsplat can land a tick after the xp drop for the attack
	private static final int MELEE_XP_WINDOW_TICKS = 2;

	private final SuppliesTrackerPlugin plugin;
	private final XpDropTracker xpDropTracker;

	// remaining charges from the last chat message, or -1 if not known yet
	private int lastKnownCharges = -1;
	// charges added from counted hits since lastKnownCharges was read
	private int countedSinceCheck = 0;

	@Inject
	BloodFury(SuppliesTrackerPlugin plugin, XpDropTracker xpDropTracker)
	{
		this.plugin = plugin;
		this.xpDropTracker = xpDropTracker;
	}

	/**
	 * Called for each of the local player's hitsplats on another actor
	 */
	public void onOwnHitsplat(int amount)
	{
		// a 0 is usually a miss, but can be a successful hit that still used a charge;
		// those can't be told apart here, so the next charges message corrects for them
		if (amount <= 0 || !isWearing() || !isMeleeAttack())
		{
			return;
		}

		countedSinceCheck++;
		plugin.buildChargesEntries(ItemID.BLOOD_AMULET, 1);
	}

	public void onChatMessage(String message)
	{
		Matcher matcher = CHARGES_MESSAGE.matcher(Text.removeTags(message));
		if (!message.contains("blood fury") || !matcher.find())
		{
			return;
		}

		int charges = Integer.parseInt(matcher.group(1).replace(",", ""));
		if (lastKnownCharges >= 0)
		{
			int used = lastKnownCharges - charges;
			// a negative amount means the amulet was recharged, so there is nothing to correct
			if (used >= 0)
			{
				int correction = used - countedSinceCheck;
				if (correction != 0)
				{
					plugin.buildChargesEntries(ItemID.BLOOD_AMULET, correction);
				}
			}
		}

		lastKnownCharges = charges;
		countedSinceCheck = 0;
	}

	/**
	 * Forget the known charges, e.g. when logging out in case another account logs in
	 */
	public void reset()
	{
		lastKnownCharges = -1;
		countedSinceCheck = 0;
	}

	private boolean isWearing()
	{
		ItemContainer worn = plugin.client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return false;
		}

		Item amulet = worn.getItem(EquipmentInventorySlot.AMULET.getSlotIdx());
		return amulet != null && amulet.getId() == ItemID.BLOOD_AMULET;
	}

	private boolean isMeleeAttack()
	{
		if (xpDropTracker.hadXpWithinTicks(Skill.ATTACK, MELEE_XP_WINDOW_TICKS)
			|| xpDropTracker.hadXpWithinTicks(Skill.STRENGTH, MELEE_XP_WINDOW_TICKS))
		{
			return true;
		}

		// defensive ranged and magic also give defence xp
		return xpDropTracker.hadXpWithinTicks(Skill.DEFENCE, MELEE_XP_WINDOW_TICKS)
			&& !xpDropTracker.hadXpWithinTicks(Skill.RANGED, MELEE_XP_WINDOW_TICKS)
			&& !xpDropTracker.hadXpWithinTicks(Skill.MAGIC, MELEE_XP_WINDOW_TICKS);
	}
}
