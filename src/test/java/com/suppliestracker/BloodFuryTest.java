package com.suppliestracker;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import com.suppliestracker.Skills.XpDropTracker;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import org.mockito.ArgumentCaptor;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;

public class BloodFuryTest
{
	private SuppliesTrackerPlugin plugin;
	private SuppliesTrackerConfig config;
	private XpDropTracker xpDropTracker;
	private ChatMessageManager chatMessageManager;
	private ItemContainer worn;
	private BloodFury bloodFury;

	@Before
	public void setUp()
	{
		plugin = mock(SuppliesTrackerPlugin.class);
		Client client = mock(Client.class);
		plugin.client = client;
		config = mock(SuppliesTrackerConfig.class);
		when(plugin.getConfig()).thenReturn(config);
		worn = mock(ItemContainer.class);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(worn);
		xpDropTracker = mock(XpDropTracker.class);
		chatMessageManager = mock(ChatMessageManager.class);
		bloodFury = new BloodFury(plugin, xpDropTracker, chatMessageManager);

		wearAmulet(ItemID.BLOOD_AMULET);
		gainXp(Skill.STRENGTH);
	}

	private void removeAmulet()
	{
		when(worn.getItem(EquipmentInventorySlot.AMULET.getSlotIdx())).thenReturn(null);
	}

	private void verifyReminders(String... expected)
	{
		ArgumentCaptor<QueuedMessage> captor = ArgumentCaptor.forClass(QueuedMessage.class);
		verify(chatMessageManager, times(expected.length)).queue(captor.capture());
		for (int i = 0; i < expected.length; i++)
		{
			assertTrue(captor.getAllValues().get(i).getRuneLiteFormattedMessage().contains(expected[i]));
		}
	}

	private void wearAmulet(int itemId)
	{
		when(worn.getItem(EquipmentInventorySlot.AMULET.getSlotIdx())).thenReturn(new Item(itemId, 1));
	}

	private void gainXp(Skill skill)
	{
		when(xpDropTracker.hadXpWithinTicks(eq(skill), anyInt())).thenReturn(true);
	}

	private void check(String charges)
	{
		bloodFury.onChatMessage("Your Amulet of blood fury will work for " + charges + " more hits.");
	}

	@Test
	public void meleeHitUsesOneCharge()
	{
		bloodFury.onOwnHitsplat(12);
		verify(plugin).buildChargesEntries(ItemID.BLOOD_AMULET, 1);
	}

	@Test
	public void missIsNotCounted()
	{
		bloodFury.onOwnHitsplat(0);
		verify(plugin, never()).buildChargesEntries(anyInt(), anyInt());
	}

	@Test
	public void hitWithoutAmuletIsNotCounted()
	{
		wearAmulet(ItemID.ENCHANTED_ONYX_AMULET);
		bloodFury.onOwnHitsplat(12);
		verify(plugin, never()).buildChargesEntries(anyInt(), anyInt());
	}

	@Test
	public void rangedHitIsNotCounted()
	{
		when(xpDropTracker.hadXpWithinTicks(eq(Skill.STRENGTH), anyInt())).thenReturn(false);
		gainXp(Skill.RANGED);
		gainXp(Skill.DEFENCE);
		bloodFury.onOwnHitsplat(12);
		verify(plugin, never()).buildChargesEntries(anyInt(), anyInt());
	}

	@Test
	public void defensiveMeleeHitIsCounted()
	{
		when(xpDropTracker.hadXpWithinTicks(eq(Skill.STRENGTH), anyInt())).thenReturn(false);
		gainXp(Skill.DEFENCE);
		bloodFury.onOwnHitsplat(12);
		verify(plugin).buildChargesEntries(ItemID.BLOOD_AMULET, 1);
	}

	@Test
	public void firstCheckOnlySetsBaseline()
	{
		check("9,987");
		verify(plugin, never()).buildChargesEntries(anyInt(), anyInt());
	}

	@Test
	public void checkAddsHitsThatWereMissed()
	{
		check("9,987");
		for (int i = 0; i < 4; i++)
		{
			bloodFury.onOwnHitsplat(10);
		}
		check("9,981");
		verify(plugin, times(4)).buildChargesEntries(ItemID.BLOOD_AMULET, 1);
		verify(plugin).buildChargesEntries(ItemID.BLOOD_AMULET, 2);
	}

	@Test
	public void checkRemovesOvercountedHits()
	{
		check("100");
		for (int i = 0; i < 5; i++)
		{
			bloodFury.onOwnHitsplat(10);
		}
		check("97");
		verify(plugin).buildChargesEntries(ItemID.BLOOD_AMULET, -2);
	}

	@Test
	public void checkMatchingCountMakesNoCorrection()
	{
		check("100");
		bloodFury.onOwnHitsplat(10);
		check("99");
		verify(plugin).buildChargesEntries(ItemID.BLOOD_AMULET, 1);
		verify(plugin, never()).buildChargesEntries(eq(ItemID.BLOOD_AMULET), eq(0));
	}

	@Test
	public void rechargeIsNotCorrected()
	{
		check("500");
		check("10,500");
		verify(plugin, never()).buildChargesEntries(anyInt(), anyInt());
	}

	@Test
	public void singleHitMessageIsParsed()
	{
		check("2");
		bloodFury.onChatMessage("Your Amulet of blood fury will work for 1 more hit.");
		verify(plugin).buildChargesEntries(ItemID.BLOOD_AMULET, 1);
	}

	@Test
	public void resetForgetsBaseline()
	{
		check("100");
		bloodFury.reset();
		check("90");
		verify(plugin, never()).buildChargesEntries(anyInt(), anyInt());
	}

	@Test
	public void unrelatedMessageIsIgnored()
	{
		check("100");
		bloodFury.onChatMessage("Your Amulet of the damned will work for 50 more hits.");
		check("100");
		verify(plugin, never()).buildChargesEntries(anyInt(), anyInt());
	}

	@Test
	public void noReminderWhenSettingIsOff()
	{
		bloodFury.onEquipmentChanged();
		verify(chatMessageManager, never()).queue(any());
	}

	@Test
	public void remindsWhenEquippedOrLoggedInWearing()
	{
		when(config.bloodFuryCheckReminder()).thenReturn(true);
		bloodFury.onEquipmentChanged();
		verifyReminders(BloodFury.EQUIP_REMINDER);
	}

	@Test
	public void remindsOnlyOnceWhileStillWearing()
	{
		when(config.bloodFuryCheckReminder()).thenReturn(true);
		bloodFury.onEquipmentChanged();
		bloodFury.onEquipmentChanged();
		verifyReminders(BloodFury.EQUIP_REMINDER);
	}

	@Test
	public void remindsToCheckWhenRemovedWithUncheckedHits()
	{
		when(config.bloodFuryCheckReminder()).thenReturn(true);
		bloodFury.onEquipmentChanged();
		bloodFury.onOwnHitsplat(10);
		removeAmulet();
		bloodFury.onEquipmentChanged();
		verifyReminders(BloodFury.EQUIP_REMINDER, BloodFury.UNEQUIP_REMINDER);
	}

	@Test
	public void noRemovalReminderWhenAlreadyChecked()
	{
		when(config.bloodFuryCheckReminder()).thenReturn(true);
		bloodFury.onEquipmentChanged();
		bloodFury.onOwnHitsplat(10);
		check("100");
		removeAmulet();
		bloodFury.onEquipmentChanged();
		verifyReminders(BloodFury.EQUIP_REMINDER);
	}

	@Test
	public void remindsAgainAfterLoggingBackIn()
	{
		when(config.bloodFuryCheckReminder()).thenReturn(true);
		bloodFury.onEquipmentChanged();
		bloodFury.reset();
		bloodFury.onEquipmentChanged();
		verifyReminders(BloodFury.EQUIP_REMINDER, BloodFury.EQUIP_REMINDER);
	}
}
