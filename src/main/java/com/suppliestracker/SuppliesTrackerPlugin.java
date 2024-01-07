/*
 * Copyright (c) 2018, Psikoi <https://github.com/psikoi>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2018, Sir Girion <https://github.com/darakelian>
 * Copyright (c) 2018, Daddy Dozer <https://github.com/Dyldozer>
 * Copyright (c) 2018, Davis Cook <https://github.com/daviscook477>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *	list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	this list of conditions and the following disclaimer in the documentation
 *	and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.suppliestracker;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;

import com.suppliestracker.Skills.Farming;
import com.suppliestracker.Skills.SkillTracker;
import com.suppliestracker.Skills.XpDropTracker;
import com.suppliestracker.session.SessionHandler;
import com.suppliestracker.ui.SuppliesTrackerPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.suppliestracker.ActionType.CAST;
import static net.runelite.api.ItemID.*;
import static net.runelite.client.RuneLite.RUNELITE_DIR;

import net.runelite.api.AnimationID;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GraphicID;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.ObjectID;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.http.api.item.ItemPrice;
import static net.runelite.api.GraphicID.CANNONBALL;
import static net.runelite.api.GraphicID.GRANITE_CANNONBALL;

@PluginDescriptor(
	name = "Supplies Used Tracker",
	description = "Tracks supplies used during the session",
	tags = {"cost"}
)
@Singleton
@Slf4j
public class SuppliesTrackerPlugin extends Plugin
{
	//Regex patterns
	private static final String POTION_PATTERN = "[(]\\d[)]";
	private static final Pattern eatPattern = Pattern.compile("^eat");
	private static final Pattern drinkPattern = Pattern.compile("^drink");
	private static final Pattern teleportPattern = Pattern.compile("^teleport");
	private static final Pattern teletabPattern = Pattern.compile("^break|^troll stronghold|^weiss");
	private static final Pattern spellPattern = Pattern.compile("^cast|^grand\\sexchange|^outside|^seers|^yanille");
	private static final Pattern bpDartsPattern = Pattern.compile("Darts: <col=007f00>(.*) dart x (.*)</col>\\. Scales: <col=007f00>(.*) \\((.*)%\\)</col>\\.");

	//Equipment slot constants
	private static final int EQUIPMENT_MAINHAND_SLOT = EquipmentInventorySlot.WEAPON.getSlotIdx();
	private static final int EQUIPMENT_AMMO_SLOT = EquipmentInventorySlot.AMMO.getSlotIdx();
	private static final int EQUIPMENT_CAPE_SLOT = EquipmentInventorySlot.CAPE.getSlotIdx();

	//Ava's calculations
	private static final double NO_AVAS_PERCENT = 1.0;
	private static final double ASSEMBLER_PERCENT = 0.20;
	private static final double ACCUMULATOR_PERCENT = 0.28;
	private static final double ATTRACTOR_PERCENT = 0.40;

	//blowpipe scale usage
	private static final double SCALES_PERCENT = 2.0 / 3.0;

	//Max use amounts
	private static final int POTION_DOSES = 4, CAKE_DOSES = 3, PIZZA_PIE_DOSES = 2;

	private static final Random random = new Random();

	private Map<Integer, Integer> itemAmounts;
	public final Map<Integer, Integer> changedItems = new HashMap<>(4);

	// id array for checking thrown items and runes
	private static final Set<Integer> thrownWeaponIds = ImmutableSet.of(
			BRONZE_DART,
			BRONZE_DARTP,
			BRONZE_DARTP_5628,
			BRONZE_DARTP_5635,
			IRON_DART,
			IRON_DARTP,
			IRON_DART_P,
			IRON_DARTP_5636,
			STEEL_DART,
			STEEL_DARTP,
			STEEL_DARTP_5630,
			STEEL_DARTP_5637,
			BLACK_DART,
			BLACK_DARTP,
			BLACK_DARTP_5631,
			BLACK_DARTP_5638,
			MITHRIL_DART,
			MITHRIL_DARTP,
			MITHRIL_DARTP_5632,
			MITHRIL_DARTP_5639,
			ADAMANT_DART,
			ADAMANT_DARTP,
			ADAMANT_DARTP_5633,
			ADAMANT_DARTP_5640,
			RUNE_DART,
			RUNE_DARTP,
			RUNE_DARTP_5634,
			RUNE_DARTP_5641,
			AMETHYST_DART,
			AMETHYST_DARTP,
			AMETHYST_DARTP_25855,
			AMETHYST_DARTP_25857,
			DRAGON_DART,
			DRAGON_DARTP,
			DRAGON_DARTP_11233,
			DRAGON_DARTP_11234,
			BRONZE_KNIFE,
			BRONZE_KNIFEP,
			BRONZE_KNIFEP_5654,
			BRONZE_KNIFEP_5661,
			IRON_KNIFE,
			IRON_KNIFEP,
			IRON_KNIFEP_5655,
			IRON_KNIFEP_5662,
			STEEL_KNIFE,
			STEEL_KNIFEP,
			STEEL_KNIFEP_5656,
			STEEL_KNIFEP_5663,
			BLACK_KNIFE,
			BLACK_KNIFEP,
			BLACK_KNIFEP_5658,
			BLACK_KNIFEP_5665,
			MITHRIL_KNIFE,
			MITHRIL_KNIFEP,
			MITHRIL_KNIFEP_5657,
			MITHRIL_KNIFEP_5664,
			ADAMANT_KNIFE,
			ADAMANT_KNIFEP,
			ADAMANT_KNIFEP_5659,
			ADAMANT_KNIFEP_5666,
			RUNE_KNIFE,
			RUNE_KNIFEP,
			RUNE_KNIFEP_5660,
			RUNE_KNIFEP_5667,
			BRONZE_THROWNAXE,
			IRON_THROWNAXE,
			STEEL_THROWNAXE,
			MITHRIL_THROWNAXE,
			ADAMANT_THROWNAXE,
			RUNE_THROWNAXE,
			DRAGON_KNIFE,
			DRAGON_KNIFE_22812,
			DRAGON_KNIFE_22814,
			DRAGON_KNIFEP_22808,
			DRAGON_KNIFEP_22810,
			DRAGON_KNIFEP,
			DRAGON_THROWNAXE,
			CHINCHOMPA_10033,
			RED_CHINCHOMPA_10034,
			BLACK_CHINCHOMPA
	);

	static final Set<Integer> runeIds = ImmutableSet.of(
			FIRE_RUNE,
			AIR_RUNE,
			WATER_RUNE,
			EARTH_RUNE,
			MIND_RUNE,
			BODY_RUNE,
			COSMIC_RUNE,
			CHAOS_RUNE,
			NATURE_RUNE,
			LAW_RUNE,
			DEATH_RUNE,
			ASTRAL_RUNE,
			BLOOD_RUNE,
			SOUL_RUNE,
			WRATH_RUNE,
			MIST_RUNE,
			DUST_RUNE,
			MUD_RUNE,
			SMOKE_RUNE,
			STEAM_RUNE,
			LAVA_RUNE,
			BLIGHTED_ANCIENT_ICE_SACK,
			BLIGHTED_VENGEANCE_SACK,
			BLIGHTED_TELEPORT_SPELL_SACK,
			BLIGHTED_ENTANGLE_SACK,
			BLIGHTED_SURGE_SACK
	);

	//Hold Supply Data
	private static final Map<Integer, SuppliesTrackerItem> suppliesEntry = new HashMap<>();
	private final Map<Integer, SuppliesTrackerItem> currentSuppliesEntry = new HashMap<>();

	public boolean showSession = false;

	private static final int BLOWPIPE_ATTACK = 5061;
	private static final int HIGH_LEVEL_MAGIC_ATTACK = 1167;
	private static final int LOW_LEVEL_MAGIC_ATTACK = 1162;
	private static final int BARRAGE_ANIMATION = 1979;
	private static final int BLITZ_ANIMATION = 1978;
	private static final int SCYTHE_OF_VITUR_ANIMATION = 8056;
	private static final int TUMEKENS_SHADOW_ANIMATION = 9493;
	private static final int VENATOR_BOW_ANIMATION = 9858;
	private static final int ONEHAND_SLASH_SWORD = 390;
	private static final int ONEHAND_STAB_SWORD = 386;
	private static final int GAUNTLET_PADDLEFISH = 23874;
	private static final int LOW_LEVEL_STANDARD_SPELLS = 711;
	private static final int WAVE_SPELL_ANIMATION = 727;
	private static final int SURGE_SPELL_ANIMATION = 7855;
	private static final int HIGH_ALCH_ANIMATION = 713;
	private static final int LUNAR_HUMIDIFY = 6294;
	private static final int ENSOULED_HEADS_ANIMATION = 7198;
	private static final int IBANS_STAFF_ANIMATION = 708;
	private static final int SLAYERS_STAFF_ANIMATION = 1576;
	private static final int ENTANGLE_ANIMATION = 1161;
	private static final int VULNERABILITY_ANIMATION = 1165;
	private static final int CRUMBLE_UNDEAD_ANIMATION = 1166;
	private static final int ENFEEBLE_ANIMATION = 1168;
	private static final int STUN_ANIMATION = 1169;
	private static final int BOW_SHOOT_ANIMATION = 426;
	private static final int WEBWEAVER_SPEC_ANIMATION = 9964;
	private static final int ACCURSED_SPEC_ANIMATION = 9961;
	private static final int CHAINMACE_ANIMATION = 245;
	private static final int CHAINMACE_SPEC_ANIMATION = 9963;
	private final Deque<ItemMenuAction> actionStack = new ArrayDeque<>();

	//Item arrays
	private final String[] RAIDS_CONSUMABLES = new String[]{"xeric's", "elder", "twisted", "revitalisation", "overload", "prayer enhance", "pysk", "suphi", "leckish", "brawk", "mycil", "roqed", "kyren", "guanic", "prael", "giral", "phluxia", "kryket", "murng", "psykk", "egniol"};
	private final Set<Integer> TRIDENT_OF_THE_SEAS_IDS = ImmutableSet.of(TRIDENT_OF_THE_SEAS, TRIDENT_OF_THE_SEAS_E, TRIDENT_OF_THE_SEAS_FULL);
	private final Set<Integer> TRIDENT_OF_THE_SWAMP_IDS = ImmutableSet.of(TRIDENT_OF_THE_SWAMP_E, TRIDENT_OF_THE_SWAMP, UNCHARGED_TOXIC_TRIDENT_E, UNCHARGED_TOXIC_TRIDENT);
	private final Set<Integer> IBANS_STAFF_IDS = ImmutableSet.of(IBANS_STAFF, IBANS_STAFF_U);
	private final Set<Integer> TUMEKENS_SHADOW_IDS = ImmutableSet.of(TUMEKENS_SHADOW_UNCHARGED, TUMEKENS_SHADOW);
	private int ammoId = 0;
	private int ammoAmount = 0;
	private int thrownId = 0;
	private int thrownAmount = 0;
	private boolean ammoLoaded = false;
	private boolean throwingAmmoLoaded = false;

	private int mainHandId = 0;
	private SuppliesTrackerPanel panel;
	private NavigationButton navButton;
	private int attackStyleVarbit = -1;

	private boolean sessionLoading = false;
	private SessionHandler sessionHandler;
	private long sessionHash = -1;

	private Projectile lastBlowpipeProj = null;
	private BlowpipeDart blowpipeDart = BlowpipeDart.ADAMANT;

	@Inject
	private Bait bait;

	//Rune pouch stuff
	private static final int[] AMOUNT_VARBITS =
		{
			Varbits.RUNE_POUCH_AMOUNT1, Varbits.RUNE_POUCH_AMOUNT2, Varbits.RUNE_POUCH_AMOUNT3, Varbits.RUNE_POUCH_AMOUNT4
		};
	private static final int[] RUNE_VARBITS =
		{
			Varbits.RUNE_POUCH_RUNE1, Varbits.RUNE_POUCH_RUNE2, Varbits.RUNE_POUCH_RUNE3, Varbits.RUNE_POUCH_RUNE4
		};

	private static final int[] OLD_AMOUNT_VARBITS =
		{
			0, 0, 0, 0
		};
	private static final int[] OLD_RUNE_VARBITS =
		{
			0, 0, 0, 0
		};


	private static int rune1 = 0;
	private static int rune2 = 0;
	private static int rune3 = 0;
	private static int rune4 = 0;

	private int amountused1 = 0;
	private int amountused2 = 0;
	private int amountused3 = 0;
	private int amountused4 = 0;

	private boolean noXpCast = false;

	//skills

	private Farming farming;

	//Cannon
	private WorldPoint cannonPosition;
	private boolean cannonPlaced;
	private boolean skipProjectileCheckThisTick;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	@Getter
	private SuppliesTrackerConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Client client;

	@Inject
	private RuneManager runeManager;

	private int ensouledHeadId = -1;

	@Inject
	public XpDropTracker xpDropTracker;

	@Inject
	public SkillTracker skillTracker;

	/**
	 * Checks if item name is potion
	 *
	 * @param name the name of the item
	 * @return if the item is a potion - i.e. has a (1) (2) (3) or (4) in the name
	 */
	static boolean isPotion(String name)
	{
		return name.endsWith("(4)") ||
			name.endsWith("(3)") ||
			name.endsWith("(2)") ||
			name.endsWith("(1)");
	}

	/**
	 * Checks if item name is pizza or pie
	 *
	 * @param name the name of the item
	 * @return if the item is a pizza or a pie - i.e. has pizza or pie in the name
	 */
	public static boolean isPizzaPie(String name)
	{
		return name.toLowerCase().contains("pizza") ||
			name.toLowerCase().contains(" pie");
	}

	public static boolean isCake(String name, int itemId)
	{
		return name.toLowerCase().contains("cake") ||
			itemId == CHOCOLATE_SLICE;
	}


	@Override
	protected void startUp()
	{
		panel = new SuppliesTrackerPanel(itemManager, this);
		farming = new Farming(this, itemManager);
		final BufferedImage header = ImageUtil.loadImageResource(getClass(), "panel_icon.png");
		panel.loadHeaderIcon(header);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panel_icon.png");

		this.sessionHandler = new SessionHandler(client);

		navButton = NavigationButton.builder()
			.tooltip("Supplies Tracker")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		cannonPlaced = false;
		cannonPosition = null;
		skipProjectileCheckThisTick = false;
		clientToolbar.removeNavigation(navButton);
	}

	@Provides
	SuppliesTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SuppliesTrackerConfig.class);
	}

	@Subscribe
	void onStatChanged(StatChanged event)
	{
		int oldXp = skillTracker.skillXp.getOrDefault(event.getSkill(), 0);
		if (event.getXp() != oldXp) {
			onXpDrop(event.getSkill(), event.getXp() - oldXp);
			skillTracker.skillXp.put(event.getSkill(), event.getXp());
		}
	}

	private void onXpDrop(Skill skill, int xpDrop) {
		xpDropTracker.update(skill, xpDrop);
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (mainHandId == TOXIC_BLOWPIPE
				&& client.getLocalPlayer().getAnimation() == BLOWPIPE_ATTACK
				&& client.getLocalPlayer().getAnimationFrame() == 0) {
			blowpipeShot();
		}

		skipProjectileCheckThisTick = false;

		if (xpDropTracker.hadXpThisTick(Skill.MAGIC))
		{
			checkUsedRunePouch();
			noXpCast = false;
		}
		else if (noXpCast)
		{
			checkUsedRunePouch();
			noXpCast = false;
		}

		amountused1 = 0;
		amountused2 = 0;
		amountused3 = 0;
		amountused4 = 0;
	}

	private void blowpipeShot() {
		double ava_percent = getAccumulatorPercent();
		// randomize the usage of supplies since we CANNOT actually get real supplies used
		if (random.nextDouble() <= ava_percent)
		{
			buildEntries(blowpipeDart.dartID);
		}
		if (random.nextDouble() <= SCALES_PERCENT)
		{
			buildEntries(ZULRAHS_SCALES);
		}
	}

	/**
	 * checks the player's cape slot to determine what percent of their darts are lost
	 * - where lost means either break or drop to floor
	 *
	 * @return the percent lost
	 */
	private double getAccumulatorPercent()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null || EQUIPMENT_CAPE_SLOT >= equipment.getItems().length) {
			return NO_AVAS_PERCENT;
		}
		int capeID = equipment.getItems()[EQUIPMENT_CAPE_SLOT].getId();

		switch (capeID)
		{
			case MAX_CAPE_13342:
			case RANGING_CAPE:
			case RANGING_CAPET:
				return config.vorkathsHead() ? ASSEMBLER_PERCENT : ACCUMULATOR_PERCENT;
			case ASSEMBLER_MAX_CAPE:
			case ASSEMBLER_MAX_CAPE_L:
			case AVAS_ASSEMBLER:
			case AVAS_ASSEMBLER_L:
			case MASORI_ASSEMBLER:
			case MASORI_ASSEMBLER_L:
			case MASORI_ASSEMBLER_MAX_CAPE:
			case MASORI_ASSEMBLER_MAX_CAPE_L:
				return ASSEMBLER_PERCENT;
			case ACCUMULATOR_MAX_CAPE:
			case AVAS_ACCUMULATOR:
				return ACCUMULATOR_PERCENT;
			case AVAS_ATTRACTOR:
				return ATTRACTOR_PERCENT;
			default:
				return NO_AVAS_PERCENT;
		}
	}

	@Subscribe
	private void onVarbitChanged(VarbitChanged event)
	{
		updateRunePouch();

		if (attackStyleVarbit != -1 && attackStyleVarbit == client.getVarpValue(VarPlayer.ATTACK_STYLE)) {
			return;
		}
		attackStyleVarbit = client.getVarpValue(VarPlayer.ATTACK_STYLE);
		if (attackStyleVarbit == 0 || attackStyleVarbit == 3)
		{
			if (client.getLocalPlayer() != null && client.getLocalPlayer().getInteracting() instanceof Player)
			{
			}
		}
		else if (attackStyleVarbit == 1)
		{
			if (client.getLocalPlayer() != null && client.getLocalPlayer().getInteracting() instanceof Player)
			{
			}
		}
	}

	/**
	 * Checks for changes between the provided inventories in runes specifically to add those runes
	 * to the supply tracker
	 * <p>
	 * we can't in general just check for when inventory slots change but this method is only run
	 * immediately after the player performs a cast animation or cast menu click/entry
	 *
	 * @param itemContainer the new inventory
	 * @param oldInv        the old inventory
	 */
	private void checkUsedRunes(ItemContainer itemContainer, Item[] oldInv)
	{
		try
		{
			for (int i = 0; i < itemContainer.getItems().length; i++)
			{
				Item newItem = itemContainer.getItems()[i];
				Item oldItem = oldInv[i];
				if (!runeIds.contains(oldItem.getId()))
				{
					continue;
				}
				// if item ids or quantity changed
				if (newItem.getId() != oldItem.getId() || newItem.getQuantity() != oldItem.getQuantity())
				{
					int quantity = oldItem.getQuantity();
					if (newItem.getId() == oldItem.getId())
					{
						quantity -= newItem.getQuantity();
					}
					// ensure that only positive quantities are added since it is reported
					// that sometimes checkUsedRunes is called on the same tick that a player
					// gains runes in their inventory
					if (quantity > 0 && quantity < 35)
					{
						buildEntries(oldItem.getId(), quantity);
					}
				}
			}

		}
		catch (IndexOutOfBoundsException ignored)
		{
		}
	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged animationChanged)
	{
		if (animationChanged.getActor() != client.getLocalPlayer()) {
			return;
		}
		int playerAniId = animationChanged.getActor().getAnimation();

		switch (playerAniId)
		{
			case HIGH_LEVEL_MAGIC_ATTACK:
				//Trident of the seas
				if (TRIDENT_OF_THE_SEAS_IDS.contains(mainHandId)) {
					if (config.chargesBox())
					{
						buildChargesEntries(TRIDENT_OF_THE_SEAS);
					}
					else
					{
						buildEntries(CHAOS_RUNE);
						buildEntries(DEATH_RUNE);
						buildEntries(FIRE_RUNE, 5);
						buildEntries(COINS_995, 10);
					}
				}
				//Trident of the swamp
				else if (TRIDENT_OF_THE_SWAMP_IDS.contains(mainHandId))
				{
					if (config.chargesBox())
					{
						buildChargesEntries(TRIDENT_OF_THE_SWAMP);
					}
					else
					{
						buildEntries(CHAOS_RUNE);
						buildEntries(DEATH_RUNE);
						buildEntries(FIRE_RUNE, 5);
						buildEntries(ZULRAHS_SCALES);
					}
				}
				// Warped Sceptre
				else if (mainHandId == WARPED_SCEPTRE) {
					if (config.chargesBox()) 
					{
						buildChargesBox(WARPED_SCEPTRE)
					}
					else 
					{
						buildEntries(CHAOS_RUNE, 2);
						buildEntries(EARTH_RUNE, 5);
					}
				}
				//Sang Staff
				else if (mainHandId == SANGUINESTI_STAFF || mainHandId == HOLY_SANGUINESTI_STAFF)
				{
					if (config.chargesBox())
					{
						buildChargesEntries(SANGUINESTI_STAFF);
					}
					else
					{
						buildEntries(BLOOD_RUNE, 3);
					}
				} else if (mainHandId == THAMMARONS_SCEPTRE || mainHandId == ACCURSED_SCEPTRE || mainHandId == ACCURSED_SCEPTRE_A)
				{
					if (config.chargesBox())
					{
						buildChargesEntries(THAMMARONS_SCEPTRE);
					}
					else
					{
						buildEntries(REVENANT_ETHER, 1);
					}
				}
				break;
			case IBANS_STAFF_ANIMATION:
				//Iban's staff
				if (IBANS_STAFF_IDS.contains(mainHandId) && config.chargesBox()) {
					buildChargesEntries(IBANS_STAFF);
				}
				// let case fall through to handle non-charge path through regular cast path
				// to prevent double counting when manually casting from the spell book.
			case LOW_LEVEL_MAGIC_ATTACK:
			case BARRAGE_ANIMATION:
			case BLITZ_ANIMATION:
			case LOW_LEVEL_STANDARD_SPELLS:
			case WAVE_SPELL_ANIMATION:
			case SURGE_SPELL_ANIMATION:
			case ENTANGLE_ANIMATION:
			case ENFEEBLE_ANIMATION:
			case VULNERABILITY_ANIMATION:
			case STUN_ANIMATION:
			case CRUMBLE_UNDEAD_ANIMATION:
			case ACCURSED_SPEC_ANIMATION:
				if (mainHandId == THAMMARONS_SCEPTRE || mainHandId == ACCURSED_SCEPTRE || mainHandId == ACCURSED_SCEPTRE_A)
				{
					if (config.chargesBox())
					{
						buildChargesEntries(THAMMARONS_SCEPTRE);
					}
					else
					{
						buildEntries(REVENANT_ETHER, 1);
					}
				}
			case SLAYERS_STAFF_ANIMATION:
			case HIGH_ALCH_ANIMATION:
			case LUNAR_HUMIDIFY:
				ItemContainer oldInv = client.getItemContainer(InventoryID.INVENTORY);

				if (oldInv != null && actionStack.stream().noneMatch(a -> a.getType() == CAST)) {
					ItemMenuAction newAction = new ItemMenuAction(CAST, oldInv.getItems());
					actionStack.push(newAction);
				}
				if (!xpDropTracker.hadXpThisTick(Skill.MAGIC))
				{
					noXpCast = true;
				}{

			}
				break;
			case BOW_SHOOT_ANIMATION:
			case WEBWEAVER_SPEC_ANIMATION:
				if (mainHandId == CRAWS_BOW || mainHandId == WEBWEAVER_BOW)
				{
					if (config.chargesBox())
					{
						buildChargesEntries(CRAWS_BOW);
					}
					else
					{
						buildEntries(REVENANT_ETHER, 1);
					}
				}
				break;
			case CHAINMACE_ANIMATION:
			case CHAINMACE_SPEC_ANIMATION:
				if (mainHandId == VIGGORAS_CHAINMACE || mainHandId == URSINE_CHAINMACE)
				{
					if (config.chargesBox())
					{
						buildChargesEntries(VIGGORAS_CHAINMACE);
					}
					else
					{
						buildEntries(REVENANT_ETHER, 1);
					}
				}
				break;
			case SCYTHE_OF_VITUR_ANIMATION:
				if (mainHandId == SCYTHE_OF_VITUR || mainHandId == HOLY_SCYTHE_OF_VITUR || mainHandId == SANGUINE_SCYTHE_OF_VITUR)
				{
					if (config.chargesBox())
					{
						buildChargesEntries(SCYTHE_OF_VITUR);
					}
					else
					{
						buildEntries(BLOOD_RUNE, 3);
						buildEntries(COINS_995, itemManager.getItemPrice(VIAL_OF_BLOOD_22446) / 100);
					}
				}
				break;
			case VENATOR_BOW_ANIMATION:
				if(mainHandId == VENATOR_BOW)
				{
					buildChargesEntries(VENATOR_BOW);
//					if (config.chargesBox())
//					{
//						buildChargesEntries(VENATOR_BOW);
//					}
//					else
//					{
//						buildEntries(ANCIENT_ESSENCE, 1);
//					}
				}
				break;
			case TUMEKENS_SHADOW_ANIMATION:
				if(TUMEKENS_SHADOW_IDS.contains(mainHandId))
				{
					if (config.chargesBox())
					{
						buildChargesEntries(TUMEKENS_SHADOW);
					}
					else
					{
						buildEntries(SOUL_RUNE, 2);
						buildEntries(CHAOS_RUNE, 5);
					}
				}
				break;
			case ONEHAND_SLASH_SWORD:
			case ONEHAND_STAB_SWORD:
				if (mainHandId == BLADE_OF_SAELDOR)
				{
					buildChargesEntries(BLADE_OF_SAELDOR);
				}
				break;
			case ENSOULED_HEADS_ANIMATION:
				if (ensouledHeadId > 0)
				{
					buildEntries(ensouledHeadId);
				}
				ensouledHeadId = -1;
				break;
		}
	}

	@Subscribe
	private void onItemContainerChanged(ItemContainerChanged itemContainerChanged)
	{
		ItemContainer itemContainer = itemContainerChanged.getItemContainer();
		int containerId = itemContainer.getId();

		if (containerId == InventoryID.INVENTORY.getId())
		{
			loadInvChanges(itemContainer);
			xpDropLinkedSupplies();
			processInvChange(itemContainer);
		}

		if (containerId == InventoryID.EQUIPMENT.getId())
		{
			processEquipChange(itemContainer);
		}
	}

	private void xpDropLinkedSupplies() {
		if (xpDropTracker.hadXpThisTick(Skill.FISHING)) {
			bait.onXpDrop();
		}
		if (xpDropTracker.hadXpThisTick(Skill.PRAYER)) {
			for (Map.Entry<Integer, Integer> entry : changedItems.entrySet()) {
				int itemId = entry.getKey();
				String itemName = itemManager.getItemComposition(itemId).getName().toLowerCase();
				if (itemName.endsWith(" bones") || itemName.endsWith("ashes")) {
					buildEntries(itemId, -entry.getValue());
				}
			}
		}
	}

	private void loadInvChanges(ItemContainer itemContainer) {
		if (itemAmounts == null) {
			itemAmounts = new HashMap<>(28 * 4 / 3);
			for (Item item : client.getItemContainer(InventoryID.INVENTORY).getItems()) {
				if (item.getId() < 0)
					continue;
				itemAmounts.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
			return;
		}

		// load new inv
		Map<Integer, Integer> newItemAmounts = new HashMap<>(28 * 4 / 3);
		changedItems.clear();
		for (Item item : itemContainer.getItems()) {
			if (item.getId() < 0)
				continue;
			newItemAmounts.merge(item.getId(), item.getQuantity(), Integer::sum);
		}

		// compute changed items
		newItemAmounts.forEach((key, value) -> {
			int dif = value - itemAmounts.getOrDefault(key, 0);
			if (dif != 0) {
				changedItems.put(key, dif);
			}
		});
		itemAmounts.forEach((key, value) -> {
			if (!newItemAmounts.containsKey(key)) {
				int dif = newItemAmounts.getOrDefault(key, 0) - value;
				if (dif != 0) {
					changedItems.put(key, dif);
				}
			}
		});
		itemAmounts = newItemAmounts;
	}

	private void processEquipChange(ItemContainer itemContainer) {
		//set mainhand for trident tracking
		if (itemContainer.getItems().length > EQUIPMENT_MAINHAND_SLOT)
		{
			Item mainHandItem = itemContainer.getItems()[EQUIPMENT_MAINHAND_SLOT];
			mainHandId = mainHandItem.getId();
			if (thrownWeaponIds.contains(mainHandId))
			{
				if (throwingAmmoLoaded)
				{
					if (thrownId == mainHandId)
					{
						if (thrownAmount - 1 == mainHandItem.getQuantity())
						{
							buildEntries(mainHandId);
						}
					}
					else
					{
						thrownId = mainHandId;
					}
					thrownAmount = mainHandItem.getQuantity();
				}
				else
				{
					thrownId = mainHandId;
					thrownAmount = mainHandItem.getQuantity();
					throwingAmmoLoaded = true;
				}
			}
			else
			{
				throwingAmmoLoaded = false;
			}
		}
		//Ammo tracking
		if (itemContainer.getItems().length > EQUIPMENT_AMMO_SLOT)
		{
			Item ammoSlot = itemContainer.getItems()[EQUIPMENT_AMMO_SLOT];
			if (ammoSlot != null)
			{
				if (ammoLoaded)
				{
					if (ammoId == ammoSlot.getId())
					{
						if (ammoAmount - 1 == ammoSlot.getQuantity())
						{
							buildEntries(ammoSlot.getId());
						}
					}
					else
					{
						ammoId = ammoSlot.getId();
					}
					ammoAmount = ammoSlot.getQuantity();
				}
				else
				{
					ammoId = ammoSlot.getId();
					ammoAmount = ammoSlot.getQuantity();
					ammoLoaded = true;
				}
			}
		}
	}

	private void processInvChange(ItemContainer itemContainer) {
		while (!actionStack.isEmpty())
		{
			ItemMenuAction frame = actionStack.pop();
			ActionType type = frame.getType();
			ItemMenuAction.ItemAction itemFrame;
			Item[] oldInv = frame.getOldInventory();
			switch (type)
			{
				case CONSUMABLE:
					itemFrame = (ItemMenuAction.ItemAction) frame;
					int nextItem = itemFrame.getItemID();
					int nextSlot = itemFrame.getSlot();
					if (itemContainer.getItems()[nextSlot].getId() != oldInv[nextSlot].getId())
					{
						buildEntries(nextItem);
					}
					break;
				case TELEPORT:
					itemFrame = (ItemMenuAction.ItemAction) frame;
					int teleid = itemFrame.getItemID();
					int slot = itemFrame.getSlot();
					if (itemContainer.getItems()[slot].getId() != oldInv[slot].getId() ||
							itemContainer.getItems()[slot].getQuantity() != oldInv[slot].getQuantity())
					{
						buildEntries(teleid);
					}
					break;
				case CAST:
					checkUsedRunes(itemContainer, oldInv);
					break;
			}
		}
	}

	@Subscribe
	private void onMenuOptionClicked(final MenuOptionClicked event)
	{
		// Fix for house pool
		/*switch (event.getId())
		{

			case 33:
			case 34:
			case 35:
			case 36:
			case 37:
			case 1007:
			case 39:
			case 40:
			case 41:
			case 42:
			case 43:
			case 57:
				break;
			default:
				return;
		}*/

		// Uses stacks to push/pop for tick eating
		// Create pattern to find eat/drink at beginning

		String target = Text.removeTags(event.getMenuTarget()).toLowerCase();
		String menuOption = Text.removeTags(event.getMenuOption()).toLowerCase();

		if ((eatPattern.matcher(menuOption).find() || drinkPattern.matcher(menuOption).find()) &&
			actionStack.stream().noneMatch(a ->
			{
				if (a instanceof ItemMenuAction.ItemAction)
				{
					ItemMenuAction.ItemAction i = (ItemMenuAction.ItemAction) a;
					return i.getItemID() == event.getItemId();
				}
				return false;
			}))
		{
			ItemContainer oldInv = client.getItemContainer(InventoryID.INVENTORY);
			int slot = event.getMenuEntry().getParam0();
			int pushItem = oldInv.getItems()[event.getMenuEntry().getParam0()].getId();
			if (pushItem == PURPLE_SWEETS || pushItem == PURPLE_SWEETS_10476)
			{
				return;
			}
			ItemMenuAction newAction = new ItemMenuAction.ItemAction(ActionType.CONSUMABLE, oldInv.getItems(), pushItem, slot);
			actionStack.push(newAction);
		}

		// Create pattern for teleport scrolls and tabs
		if (teleportPattern.matcher(menuOption).find() ||
			teletabPattern.matcher(menuOption).find())
		{
			ItemContainer oldInv = client.getItemContainer(InventoryID.INVENTORY);

			// Makes stack only contains one teleport type to stop from adding multiple of one teleport
			if (oldInv != null && actionStack.stream().noneMatch(a ->
					a.getType() == ActionType.TELEPORT)) {
				int teleid = event.getItemId();
				ItemMenuAction newAction = new ItemMenuAction.ItemAction(ActionType.TELEPORT, oldInv.getItems(), teleid, event.getMenuEntry().getParam0());
				actionStack.push(newAction);
			}
		}

		// note that here we look at the option not target b/c the option for all spells is cast
		// but the target differs based on each spell name
		if (spellPattern.matcher(menuOption).find())
		{
			ItemContainer oldInv = client.getItemContainer(InventoryID.INVENTORY);

			if (oldInv != null && actionStack.stream().noneMatch(a ->
					a.getType() == CAST)) {
				ItemMenuAction newAction = new ItemMenuAction(CAST, oldInv.getItems());
				actionStack.push(newAction);
			}
		}

		if (menuOption.equals("use"))
		{
			if (itemManager.getItemComposition(event.getItemId()).getName().toLowerCase().contains("compost"))
			{
				farming.setBucketId(event.getItemId());
			}
			else if (!target.contains("->"))
			{
				farming.setPlantId(event.getItemId());
			}
		}

		if (target.contains(" reanimation ->") && event.getMenuAction() == MenuAction.WIDGET_TARGET_ON_WIDGET)
		{
			ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
			if (inv != null) {
				Item item = inv.getItem(event.getParam0());
				if (item != null) {
					ensouledHeadId = item.getId();
				}
			}
		}

		//Adds tracking to Master Scroll Book
		if (menuOption.equalsIgnoreCase("activate") && target.endsWith(" teleport scroll")) {
			switch (target) {
				case "watson teleport scroll":
					buildEntries(WATSON_TELEPORT);
					break;
				case "zul-andra teleport scroll":
					buildEntries(ZULANDRA_TELEPORT);
					break;
				case "nardah teleport scroll":
					buildEntries(NARDAH_TELEPORT);
					break;
				case "digsite teleport scroll":
					buildEntries(DIGSITE_TELEPORT);
					break;
				case "feldip hills teleport scroll":
					buildEntries(FELDIP_HILLS_TELEPORT);
					break;
				case "lunar isle teleport scroll":
					buildEntries(LUNAR_ISLE_TELEPORT);
					break;
				case "mort'ton teleport scroll":
					buildEntries(MORTTON_TELEPORT);
					break;
				case "pest control teleport scroll":
					buildEntries(PEST_CONTROL_TELEPORT);
					break;
				case "piscatoris teleport scroll":
					buildEntries(PISCATORIS_TELEPORT);
					break;
				case "iorwerth camp teleport scroll":
					buildEntries(IORWERTH_CAMP_TELEPORT);
					break;
				case "mos le'harmless teleport scroll":
					buildEntries(MOS_LEHARMLESS_TELEPORT);
					break;
				case "lumberyard teleport scroll":
					buildEntries(LUMBERYARD_TELEPORT);
					break;
				case "revenant cave teleport scroll":
					buildEntries(REVENANT_CAVE_TELEPORT);
					break;
				case "tai bwo wannai teleport scroll":
					buildEntries(TAI_BWO_WANNAI_TELEPORT);
					break;
				case "key master teleport":
					buildEntries(KEY_MASTER_TELEPORT);
					break;
			}
		}
	}

	@Subscribe
	private void onChatMessage(ChatMessage event) {
		String message = event.getMessage();

		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM) {
			return;
		}
		if (message.toLowerCase().contains("you plant "))
		{
			farming.onChatPlant(message.toLowerCase());
		}

		else if (message.toLowerCase().contains("you treat "))
		{
			farming.setEndlessBucket(message);
			farming.onChatTreat(message.toLowerCase());
		}
		else if (message.toLowerCase().contains("you eat the sweets."))
		{
			buildEntries(PURPLE_SWEETS_10476);
		}
		else if (message.toLowerCase().contains("your amulet has") ||
			message.toLowerCase().contains("your amulet's last charge"))
		{
			buildChargesEntries(AMULET_OF_GLORY6);
		}
		else if (message.toLowerCase().contains("your ring of dueling has") ||
			message.toLowerCase().contains("your ring of dueling crumbles"))
		{
			buildChargesEntries(RING_OF_DUELING8);
		}
		else if (message.toLowerCase().contains("your ring of wealth has"))
		{
			buildChargesEntries(RING_OF_WEALTH_5);
		}
		else if (message.toLowerCase().contains("your combat bracelet has") ||
			message.toLowerCase().contains("your combat bracelet's last charge"))
		{
			buildChargesEntries(COMBAT_BRACELET6);
		}
		else if (message.toLowerCase().contains("your games necklace has") ||
			message.toLowerCase().contains("your games necklace crumbles"))
		{
			buildChargesEntries(GAMES_NECKLACE8);
		}
		else if (message.toLowerCase().contains("your skills necklace has") ||
			message.toLowerCase().contains("your skills necklace's last charge"))
		{
			buildChargesEntries(SKILLS_NECKLACE6);
		}
		else if (message.toLowerCase().contains("your necklace of passage has") ||
			message.toLowerCase().contains("your necklace of passage crumbles"))
		{
			buildChargesEntries(NECKLACE_OF_PASSAGE5);
		}
		else if (message.toLowerCase().contains("your burning amulet has") ||
			message.toLowerCase().contains("your burning amulet crumbles"))
		{
			buildChargesEntries(BURNING_AMULET5);
		}

		//Cannon
		else if (message.equals("You add the furnace."))
		{
			cannonPlaced = true;
		}

		else if (message.contains("You pick up the cannon")
				|| message.contains("Your cannon has decayed. Speak to Nulodion to get a new one!"))
		{
			cannonPlaced = false;
		}
		else if (message.startsWith("You unload your cannon and receive Cannonball")
				|| message.startsWith("You unload your cannon and receive Granite cannonball"))
		{
			skipProjectileCheckThisTick = true;
		}
		else if (message.contains("A magical chest")
				&& message.contains("outside the Theatre of Blood"))
		{
			buildEntries(HEALER_ICON_20802);
		}
		else if (message.contains("Torfinn has retrieved some of your items."))
		{
			buildEntries(HEALER_ICON_22308);
		}

		Matcher bpMatcher = bpDartsPattern.matcher(message);
		if (bpMatcher.matches()) {
			String dartName = bpMatcher.group(1);
			blowpipeDart = BlowpipeDart.forName(dartName);
		}
	}

	/**
	 * used to enter location of cannon to check if cannon has shot
	 *
	 * @param event   checks if cannon is placed by player and location of cannon
	 */
	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject gameObject = event.getGameObject();

		Player localPlayer = client.getLocalPlayer();
		if (gameObject.getId() != ObjectID.CANNON_BASE && gameObject.getId() != ObjectID.CANNON_BASE_43029 || cannonPlaced) {
			return;
		}
		if (localPlayer.getWorldLocation().distanceTo(gameObject.getWorldLocation()) <= 2
				&& localPlayer.getAnimation() == AnimationID.BURYING_BONES)
		{
			cannonPosition = gameObject.getWorldLocation();
		}
	}

	@Subscribe
	private void onProjectileMoved(ProjectileMoved event)
	{
		cannonball(event);
		blowpipeDartCheck(event);
	}

	private void blowpipeDartCheck(ProjectileMoved event) {
		if (mainHandId != TOXIC_BLOWPIPE || client.getLocalPlayer().getAnimation() != BLOWPIPE_ATTACK) {
			return;
		}
		Projectile projectile = event.getProjectile();
		if (projectile.equals(lastBlowpipeProj)) {
			return;
		}
		BlowpipeDart dart = BlowpipeDart.forProjID(projectile.getId());
		if (dart == null) {
			return;
		}
		LocalPoint localPoint = new LocalPoint(projectile.getX1(), projectile.getY1());
		WorldPoint point = WorldPoint.fromLocal(client, localPoint);
		if (!client.getLocalPlayer().getWorldLocation().equals(point)) {
			return;
		}
		for (Player player : client.getPlayers()) {
			if (player == client.getLocalPlayer()) {
				continue;
			}
			if (player.getWorldLocation().equals(client.getLocalPlayer().getWorldLocation())
				&& player.getPlayerComposition().getEquipmentId(KitType.WEAPON) == TOXIC_BLOWPIPE
				&& player.getAnimation() == BLOWPIPE_ATTACK) {
				// player is on same tile and also using blowpipe, we can't guarantee dart projectile is ours
				return;
			}
		}
		blowpipeDart = dart;
		lastBlowpipeProj = projectile;
	}

	private void cannonball(ProjectileMoved event) {
		if (cannonPosition == null) {
			return;
		}
		Projectile projectile = event.getProjectile();
		int pId = projectile.getId();
		boolean regCball = pId == CANNONBALL || pId == GraphicID.CANNONBALL_OR;
		boolean graniteCball = pId == GRANITE_CANNONBALL || pId == GraphicID.GRANITE_CANNONBALL_OR;
		if (!regCball && !graniteCball) {
			return;
		}
		WorldPoint projectileLoc = WorldPoint.fromLocal(client, projectile.getX1(), projectile.getY1(), client.getPlane());

		if (projectileLoc.distanceTo2D(cannonPosition) > 1 || projectile.getX() != 0 || projectile.getY() != 0) {
			return;
		}
		if (!skipProjectileCheckThisTick)
		{
			buildEntries(graniteCball ? ItemID.GRANITE_CANNONBALL : ItemID.CANNONBALL);
		}
	}

	/**
	 * correct prices for potions, pizzas pies, and cakes
	 * tracker tracks each dose of a potion/pizza/pie/cake as an entire one
	 * so must divide price by total amount of doses in each
	 * this is necessary b/c the most correct/accurate price for these resources is the
	 * full price not the 1-dose price
	 *
	 * @param name   the item name
	 * @param itemId the item id
	 * @param price  the current calculated price
	 * @return the price modified by the number of doses
	 */
	private long scalePriceByDoses(String name, int itemId, long price)
	{
		if (isPotion(name))
		{
			return price / POTION_DOSES;
		}
		if (isPizzaPie(name))
		{
			return price / PIZZA_PIE_DOSES;
		}
		if (isCake(name, itemId))
		{
			return price / CAKE_DOSES;
		}
		return price;
	}

	/**
	 * Add an item to the supply tracker (with 1 count for that item)
	 *
	 * @param itemId the id of the item
	 */
	public void buildEntries(int itemId)
	{
		buildEntries(itemId, 1);
	}

	/**
	 * Add an item to the supply tracker
	 *
	 * @param itemId the id of the item
	 * @param count  the amount of the item to add to the tracker
	 */
	public void buildEntries(int itemId, int count)
	{
		final ItemComposition itemComposition = itemManager.getItemComposition(itemId);
		String name = itemComposition.getName();
		long calculatedPrice;

		if (itemId == GAUNTLET_PADDLEFISH)
		{
			return;
		}

		for (String raidsConsumables : RAIDS_CONSUMABLES)
		{
			if (name.toLowerCase().contains(raidsConsumables))
			{
				return;
			}
		}

		// convert potions, pizzas/pies, and cakes to their full equivalents
		// e.g. a half pizza becomes full pizza, 3 dose potion becomes 4, etc...
		if (isPotion(name))
		{
			name = name.replaceAll(POTION_PATTERN, "(4)");
			itemId = getPotionID(name);
		}
		else if (isPizzaPie(name))
		{
			itemId = getFullVersionItemID(itemId);
			name = itemManager.getItemComposition(itemId).getName();
		}
		else if (isCake(name, itemId))
		{
			itemId = getFullVersionItemID(itemId);
			name = itemManager.getItemComposition(itemId).getName();
		}

		int newQuantity;
		if (suppliesEntry.containsKey(itemId))
		{
			newQuantity = suppliesEntry.get(itemId).getQuantity() + count;
		}
		else
		{
			newQuantity = count;
		}

		int newQuantityC;
		if (currentSuppliesEntry.containsKey(itemId))
		{
			newQuantityC = currentSuppliesEntry.get(itemId).getQuantity() + count;
		}
		else
		{
			newQuantityC = count;
		}

		// calculate price for amount of doses used
		calculatedPrice = itemManager.getItemPrice(itemId);
		calculatedPrice = scalePriceByDoses(name, itemId, calculatedPrice);

		// write the new quantity and calculated price for this entry
		SuppliesTrackerItem newEntry = new SuppliesTrackerItem(
			itemId,
			name,
			newQuantity,
			calculatedPrice * newQuantity);

		suppliesEntry.put(itemId, newEntry);

		SuppliesTrackerItem newEntryC = new SuppliesTrackerItem(
			itemId,
			name,
			newQuantityC,
			calculatedPrice * newQuantityC);

		if (!sessionLoading)
		{
			sessionHandler.addToSession(itemId, count, false);
			currentSuppliesEntry.put(itemId, newEntryC);
		}

		SwingUtilities.invokeLater(() -> panel.addItem(showSession ? newEntryC : newEntry));
	}

	private void buildChargesEntries(int itemId)
	{
		buildChargesEntries(itemId, 1);
	}

	/**
	 * Add an item to the supply tracker
	 *
	 * @param itemId the id of the item
	 */
	private void buildChargesEntries(int itemId, int count)
	{
		final ItemComposition itemComposition = itemManager.getItemComposition(itemId);
		String name = itemComposition.getName();
		long calculatedPrice = 0;


		int newQuantity;
		if (suppliesEntry.containsKey(itemId))
		{
			newQuantity = suppliesEntry.get(itemId).getQuantity() + count;
		}
		else
		{
			newQuantity = count;
		}

		int newQuantityC;
		if (currentSuppliesEntry.containsKey(itemId))
		{
			newQuantityC = currentSuppliesEntry.get(itemId).getQuantity() + count;
		}
		else
		{
			newQuantityC = count;
		}

		switch (itemId)
		{
			case AMULET_OF_GLORY6:
				calculatedPrice = (itemManager.getItemPrice(AMULET_OF_GLORY6) - itemManager.getItemPrice(AMULET_OF_GLORY)) / 6;
				break;
			case RING_OF_DUELING8:
				calculatedPrice = itemManager.getItemPrice(RING_OF_DUELING8) / 8;
				break;
			case RING_OF_WEALTH_5:
				calculatedPrice = (itemManager.getItemPrice(RING_OF_WEALTH_5) - itemManager.getItemPrice(RING_OF_WEALTH)) / 5;
				break;
			case COMBAT_BRACELET6:
				calculatedPrice = (itemManager.getItemPrice(COMBAT_BRACELET6) - itemManager.getItemPrice(COMBAT_BRACELET)) / 6;
				break;
			case GAMES_NECKLACE8:
				calculatedPrice = itemManager.getItemPrice(GAMES_NECKLACE8) / 8;
				break;
			case SKILLS_NECKLACE6:
				calculatedPrice = (itemManager.getItemPrice(SKILLS_NECKLACE6) - itemManager.getItemPrice(SKILLS_NECKLACE)) / 6;
				break;
			case NECKLACE_OF_PASSAGE5:
				calculatedPrice = itemManager.getItemPrice(NECKLACE_OF_PASSAGE5) / 5;
				break;
			case BURNING_AMULET5:
				calculatedPrice = itemManager.getItemPrice(BURNING_AMULET5) / 5;
				break;
			case SCYTHE_OF_VITUR:
				calculatedPrice = itemManager.getItemPrice(BLOOD_RUNE) * 3L + itemManager.getItemPrice(VIAL_OF_BLOOD_22446) / 100;
				break;
			case VENATOR_BOW:
				calculatedPrice = itemManager.getItemPrice(ANCIENT_ESSENCE);
				break;
			case TUMEKENS_SHADOW:
				calculatedPrice = itemManager.getItemPrice(SOUL_RUNE) * 2L
						+ itemManager.getItemPrice(CHAOS_RUNE) * 5L;
				break;
			case TRIDENT_OF_THE_SWAMP:
				calculatedPrice = itemManager.getItemPrice(CHAOS_RUNE) + itemManager.getItemPrice(DEATH_RUNE) +
						itemManager.getItemPrice(FIRE_RUNE) * 5L + itemManager.getItemPrice(ZULRAHS_SCALES);
				break;
			case TRIDENT_OF_THE_SEAS:
				calculatedPrice = itemManager.getItemPrice(CHAOS_RUNE) + itemManager.getItemPrice(DEATH_RUNE) +
						itemManager.getItemPrice(FIRE_RUNE) * 5L + itemManager.getItemPrice(COINS_995) * 10L;
				break;
			case SANGUINESTI_STAFF:
				calculatedPrice = itemManager.getItemPrice(BLOOD_RUNE) * 3L;
				break;
			case IBANS_STAFF:
				calculatedPrice = itemManager.getItemPrice(DEATH_RUNE) + itemManager.getItemPrice(FIRE_RUNE) * 5L;
				break;
			case BLADE_OF_SAELDOR:
				calculatedPrice = 0;
				break;
			case THAMMARONS_SCEPTRE:
			case CRAWS_BOW:
			case VIGGORAS_CHAINMACE:
				calculatedPrice = itemManager.getItemPrice(REVENANT_ETHER);
				break;
		}

		// write the new quantity and calculated price for this entry
		SuppliesTrackerItem newEntry = new SuppliesTrackerItem(
				itemId,
				name,
				newQuantity,
				calculatedPrice * newQuantity);

		suppliesEntry.put(itemId, newEntry);

		SuppliesTrackerItem newEntryC = new SuppliesTrackerItem(
				itemId,
				name,
				newQuantityC,
				calculatedPrice * newQuantityC);

		if (!sessionLoading)
		{
			sessionHandler.addToSession(itemId, count, true);
			currentSuppliesEntry.put(itemId, newEntryC);
		}

		SwingUtilities.invokeLater(() -> panel.addItem(showSession ? newEntryC : newEntry));
	}


	/**
	 * reset all item stacks
	 */
	public void clearSupplies()
	{
		suppliesEntry.clear();
		currentSuppliesEntry.clear();
		sessionHandler.clearSupplies();
	}

	/**
	 * reset an individual item stack
	 *
	 * @param itemId the id of the item stack
	 */
	public void clearItem(int itemId)
	{
		suppliesEntry.remove(itemId);
		currentSuppliesEntry.remove(itemId);
		sessionHandler.clearItem(itemId);
	}

	/**
	 * Removes one item from an individual item stack
	 *
	 * @param itemId the id of the item stack
	 */
	public void removeOneItem(int itemId)
	{
		if (!suppliesEntry.containsKey(itemId)) {
			return;
		}
		if (suppliesEntry.get(itemId).getQuantity() == 1)
		{
			clearItem(itemId);
		}
		else
		{
			suppliesEntry.get(itemId).setQuantity(suppliesEntry.get(itemId).getQuantity() - 1);
		}
	}

	/**
	 * Gets the item id that matches the provided name within the itemManager
	 *
	 * @param name the given name
	 * @return the item id for this name
	 */
	public int getPotionID(String name)
	{
		int itemId = 0;

		List<ItemPrice> items = itemManager.search(name);
		for (ItemPrice item : items)
		{
			if (item.getName().contains(name))
			{
				itemId = item.getId();
			}
		}
		return itemId;
	}

	/**
	 * Takes the item id of a partial item (e.g. 1 dose potion, 1/2 a pizza, etc...) and returns
	 * the corresponding full item
	 *
	 * @param itemId the partial item id
	 * @return the full item id
	 */
	private int getFullVersionItemID(int itemId)
	{
		switch (itemId)
		{
			case _12_ANCHOVY_PIZZA:
				itemId = ANCHOVY_PIZZA;
				break;
			case _12_MEAT_PIZZA:
				itemId = MEAT_PIZZA;
				break;
			case _12_PINEAPPLE_PIZZA:
				itemId = PINEAPPLE_PIZZA;
				break;
			case _12_PLAIN_PIZZA:
				itemId = PLAIN_PIZZA;
				break;
			case HALF_A_REDBERRY_PIE:
				itemId = REDBERRY_PIE;
				break;
			case HALF_A_GARDEN_PIE:
				itemId = GARDEN_PIE;
				break;
			case HALF_A_SUMMER_PIE:
				itemId = SUMMER_PIE;
				break;
			case HALF_A_FISH_PIE:
				itemId = FISH_PIE;
				break;
			case HALF_A_BOTANICAL_PIE:
				itemId = BOTANICAL_PIE;
				break;
			case HALF_A_MUSHROOM_PIE:
				itemId = MUSHROOM_PIE;
				break;
			case HALF_AN_ADMIRAL_PIE:
				itemId = ADMIRAL_PIE;
				break;
			case HALF_A_WILD_PIE:
				itemId = WILD_PIE;
				break;
			case HALF_AN_APPLE_PIE:
				itemId = APPLE_PIE;
				break;
			case HALF_A_MEAT_PIE:
				itemId = MEAT_PIE;
				break;
			case _23_CAKE:
			case SLICE_OF_CAKE:
				itemId = CAKE;
				break;
			case _23_CHOCOLATE_CAKE:
			case CHOCOLATE_SLICE:
				itemId = CHOCOLATE_CAKE;
				break;
		}
		return itemId;
	}

	private void checkUsedRunePouch()
	{
		if (!xpDropTracker.hadXpThisTick(Skill.MAGIC) && !noXpCast) {
			return;
		}
		if (amountused1 != 0 && amountused1 < 20)
		{
			buildEntries(Runes.getRune(rune1).getItemId(), amountused1);
		}
		if (amountused2 != 0 && amountused2 < 20)
		{
			buildEntries(Runes.getRune(rune2).getItemId(), amountused2);
		}
		if (amountused3 != 0 && amountused3 < 20)
		{
			buildEntries(Runes.getRune(rune3).getItemId(), amountused3);
		}
		if (amountused4 != 0 && amountused4 < 20)
		{
			buildEntries(Runes.getRune(rune4).getItemId(), amountused4);
		}
	}

	/**
	 * Checks local variable data against client data then returns differences then updates local to client
	 */
	private void updateRunePouch()
	{
		//check amounts
		if (OLD_AMOUNT_VARBITS[0] != client.getVarbitValue(AMOUNT_VARBITS[0]))
		{
			if (OLD_AMOUNT_VARBITS[0] > client.getVarbitValue(AMOUNT_VARBITS[0]))
			{
				amountused1 += OLD_AMOUNT_VARBITS[0] - client.getVarbitValue(AMOUNT_VARBITS[0]);
			}
			OLD_AMOUNT_VARBITS[0] = client.getVarbitValue(AMOUNT_VARBITS[0]);
		}
		if (OLD_AMOUNT_VARBITS[1] != client.getVarbitValue(AMOUNT_VARBITS[1]))
		{
			if (OLD_AMOUNT_VARBITS[1] > client.getVarbitValue(AMOUNT_VARBITS[1]))
			{
				amountused2 += OLD_AMOUNT_VARBITS[1] - client.getVarbitValue(AMOUNT_VARBITS[1]);
			}
			OLD_AMOUNT_VARBITS[1] = client.getVarbitValue(AMOUNT_VARBITS[1]);
		}
		if (OLD_AMOUNT_VARBITS[2] != client.getVarbitValue(AMOUNT_VARBITS[2]))
		{
			if (OLD_AMOUNT_VARBITS[2] > client.getVarbitValue(AMOUNT_VARBITS[2]))
			{
				amountused3 += OLD_AMOUNT_VARBITS[2] - client.getVarbitValue(AMOUNT_VARBITS[2]);
			}
			OLD_AMOUNT_VARBITS[2] = client.getVarbitValue(AMOUNT_VARBITS[2]);
		}
		if (OLD_AMOUNT_VARBITS[3] != client.getVarbitValue(AMOUNT_VARBITS[3]))
		{
			if (OLD_AMOUNT_VARBITS[3] > client.getVarbitValue(AMOUNT_VARBITS[3]))
			{
				amountused4 += OLD_AMOUNT_VARBITS[3] - client.getVarbitValue(AMOUNT_VARBITS[3]);
			}
			OLD_AMOUNT_VARBITS[3] = client.getVarbitValue(AMOUNT_VARBITS[3]);
		}

		//check runes
		if (OLD_RUNE_VARBITS[0] != client.getVarbitValue(RUNE_VARBITS[0]))
		{
			rune1 = client.getVarbitValue(RUNE_VARBITS[0]);
			OLD_RUNE_VARBITS[0] = client.getVarbitValue(RUNE_VARBITS[0]);
		}
		if (OLD_RUNE_VARBITS[1] != client.getVarbitValue(RUNE_VARBITS[1]))
		{
			rune2 = client.getVarbitValue(RUNE_VARBITS[1]);
			OLD_RUNE_VARBITS[1] = client.getVarbitValue(RUNE_VARBITS[1]);
		}
		if (OLD_RUNE_VARBITS[2] != client.getVarbitValue(RUNE_VARBITS[2]))
		{
			rune3 = client.getVarbitValue(RUNE_VARBITS[2]);
			OLD_RUNE_VARBITS[2] = client.getVarbitValue(RUNE_VARBITS[2]);
		}
		if (OLD_RUNE_VARBITS[3] != client.getVarbitValue(RUNE_VARBITS[3]))
		{
			rune4 = client.getVarbitValue(RUNE_VARBITS[3]);
			OLD_RUNE_VARBITS[3] = client.getVarbitValue(RUNE_VARBITS[3]);
		}
	}

	@Subscribe
	private void onGameStateChanged(final GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN || client.getAccountHash() == sessionHash) {
			return;
		}

		skillTracker.loadAll(client);

		sessionHash = client.getAccountHash();

		//clear on new username login
		suppliesEntry.clear();
		currentSuppliesEntry.clear();
		SwingUtilities.invokeLater(() -> panel.resetAll());
		sessionHandler.clearSession();

		try
		{
			File sessionFile = new File(RUNELITE_DIR + "/supplies-tracker/" + client.getAccountHash() + ".txt");

			if (sessionFile.createNewFile()) {
				// already exists
				return;
			}
			sessionLoading = true;
			List<String> savedSupplies = Files.readAllLines(sessionFile.toPath());

			for (String supplies: savedSupplies)
			{
				if (!supplies.contains(":")) {
					continue;
				}
				String[] temp = supplies.split(":");

				if (temp[0].contains("c"))
				{
					buildChargesEntries(Integer.parseInt(temp[0].replace("c", "")), Integer.parseInt(temp[1]));
					sessionHandler.setupMaps(Integer.parseInt(temp[0].replace("c", "")), Integer.parseInt(temp[1]), true);
				}
				else
				{
					buildEntries(Integer.parseInt(temp[0]), Integer.parseInt(temp[1]));
					sessionHandler.setupMaps(Integer.parseInt(temp[0]), Integer.parseInt(temp[1]), false);
				}
			}
			sessionLoading = false;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void switchTracking()
	{
		SwingUtilities.invokeLater(() -> panel.resetAll());
		showSession = !showSession;
		Map<Integer, SuppliesTrackerItem> map = showSession ? currentSuppliesEntry : suppliesEntry;
		for (SuppliesTrackerItem item: map.values())
		{
			SwingUtilities.invokeLater(() -> panel.addItem(item));
		}
	}
}
