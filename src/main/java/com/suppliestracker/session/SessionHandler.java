package com.suppliestracker.session;

import com.google.inject.Inject;
import com.suppliestracker.SuppliesTrackerItemJson;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import jdk.vm.ci.meta.Local;
import net.runelite.api.Client;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

public class SessionHandler
{
	private static final File SESSION_DIR = new File(RUNELITE_DIR, "supplies-tracker");

	private final Client client;

	private final Map<Integer, Integer> supplies = new HashMap<>();
	private final Map<Integer, Integer> charges = new HashMap<>();

	@Inject
	public SessionHandler(Client client)
	{
		this.client = client;
		SESSION_DIR.mkdir();
	}

	public void setupMaps(int itemId, int quantity, boolean isCharges)
	{
		Map<Integer, Integer> map = isCharges ? charges : supplies;
		map.put(itemId, map.getOrDefault(itemId, 0) + quantity);
	}

	public void clearItem(int itemId)
	{
		this.supplies.remove(itemId);
		this.charges.remove(itemId);
		buildSessionFile(this.charges, this.supplies);
	}

	public void clearSupplies()
	{
		this.supplies.clear();
		this.charges.clear();
		buildSessionFile(this.charges, this.supplies);
	}

	public void addToSession(int itemId, int quantity, boolean isCharges)
	{
		Map<Integer, Integer> map = isCharges ? charges : supplies;
		map.put(itemId, map.getOrDefault(itemId, 0) + quantity);
		buildSessionFile(this.charges, this.supplies);
	}

	private void buildSessionFile(Map<Integer, Integer> c, Map<Integer, Integer> s)
	{
		try
		{
			File sessionFile = new File(RUNELITE_DIR + "/supplies-tracker/" + client.getUsername() + ".txt");

			if (!sessionFile.createNewFile())
			{
				sessionFile.delete();
				sessionFile.createNewFile();
			}

			try (FileWriter f = new FileWriter(sessionFile, true); BufferedWriter b = new BufferedWriter(f); PrintWriter p = new PrintWriter(b))
			{
				for (int id : c.keySet())
				{
					p.println("c" + id + ":" + c.get(id));
				}
				for (int id : s.keySet())
				{
					p.println(id + ":" + s.get(id));
				}
			}
			catch (IOException i)
			{
				i.printStackTrace();
			}

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void addNewRecordToJson(SuppliesTrackerItemJson json)
	{
		try
		{
			File jsonDir = new File(RUNELITE_DIR + "/supplies-tracker/json");
			jsonDir.mkdir();

			DateTimeFormatter timeStampPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			File sessionFile = new File(RUNELITE_DIR + "/supplies-tracker/json/" + timeStampPattern.format(java.time.LocalDateTime.now()) + ".log");

			sessionFile.createNewFile();

			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sessionFile, true), "UTF-8"));
			writer.append(json.toJson());
			writer.append("\n");
			writer.flush();
			writer.close();

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void clearSession()
	{
		supplies.clear();
		charges.clear();
	}
}
