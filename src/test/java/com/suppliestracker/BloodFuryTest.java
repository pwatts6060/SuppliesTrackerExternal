package com.suppliestracker;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Before;
import org.junit.Test;

public class BloodFuryTest
{
	private SuppliesTrackerPlugin plugin;
	private Client client;
	private BloodFury bloodFury;

	@Before
	public void setUp()
	{
		plugin = mock(SuppliesTrackerPlugin.class);
		client = mock(Client.class);
		plugin.client = client;
		bloodFury = new BloodFury(plugin);
	}

	private void setCharges(int charges)
	{
		when(client.getVarbitValue(VarbitID.CHARGES_BLOOD_FURY_QUANTITY)).thenReturn(charges);
		bloodFury.updateVarbit(VarbitID.CHARGES_BLOOD_FURY_QUANTITY);
	}

	@Test
	public void firstUpdateIsOnlyABaseline()
	{
		setCharges(10000);
		verify(plugin, never()).buildChargesEntries(anyInt(), anyInt());
	}

	@Test
	public void singleHitUsesOneCharge()
	{
		setCharges(10000);
		setCharges(9999);
		verify(plugin).buildChargesEntries(ItemID.BLOOD_AMULET, 1);
	}

	@Test
	public void multiHitAttackUsesSeveralCharges()
	{
		setCharges(10000);
		setCharges(9997);
		verify(plugin).buildChargesEntries(ItemID.BLOOD_AMULET, 3);
	}

	@Test
	public void lastChargeIsTracked()
	{
		setCharges(1);
		setCharges(0);
		verify(plugin).buildChargesEntries(ItemID.BLOOD_AMULET, 1);
	}

	@Test
	public void rechargingIsNotTracked()
	{
		setCharges(500);
		setCharges(10500);
		verify(plugin, never()).buildChargesEntries(anyInt(), anyInt());
	}

	@Test
	public void largeDropIsNotTracked()
	{
		setCharges(10000);
		setCharges(0);
		verify(plugin, never()).buildChargesEntries(anyInt(), anyInt());
	}

	@Test
	public void resetStartsANewBaseline()
	{
		setCharges(5);
		bloodFury.reset();
		setCharges(3);
		verify(plugin, never()).buildChargesEntries(anyInt(), anyInt());
	}

	@Test
	public void unrelatedVarbitIsIgnored()
	{
		bloodFury.updateVarbit(VarbitID.CHARGES_BLOOD_FURY_QUANTITY + 1);
		verifyNoInteractions(client);
	}
}
