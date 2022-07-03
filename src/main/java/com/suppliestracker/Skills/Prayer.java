package com.suppliestracker.Skills;

import com.suppliestracker.SuppliesTrackerPlugin;
import lombok.Setter;
import net.runelite.client.game.ItemManager;

import javax.inject.Singleton;

@Singleton
public class Prayer
{
	private final SuppliesTrackerPlugin plugin;
	private final ItemManager itemManager;
	@Setter
	private int bonesId = 0;

	public Prayer(SuppliesTrackerPlugin plugin, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.itemManager = itemManager;
	}

	public void OnChat(String message)
	{
		String name = itemManager.getItemComposition(bonesId).getName().toLowerCase();

		if (bonesId <= 0 || !name.contains("bones"))
		{
			return;
		}
		if (message.toLowerCase().contains("you bury the bones"))
		{
			plugin.buildEntries(bonesId);
		}
	}

	public void build()
	{
		plugin.buildEntries(bonesId);
	}
}
