package com.claudeexport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.ScriptID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;

/**
 * Reads game state into an ExportState. Every method here must run on the client
 * thread, because the game data (and ItemManager lookups) are only safe to read there.
 */
@Slf4j
class StateCollector
{
	// Slayer task id meaning "boss task"; the actual boss is in a separate varbit
	private static final int BOSS_TASK_ID = 98;

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClaudeExportConfig config;

	@Inject
	private ConfigManager configManager;

	// Diary completion flags per region: easy, medium, hard, elite.
	// Karamja's first three tiers use older "ATJUN" varbits; the rest follow one naming pattern.
	private static final Object[][] DIARIES = {
		{"Ardougne", VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE, VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE},
		{"Desert", VarbitID.DESERT_DIARY_EASY_COMPLETE, VarbitID.DESERT_DIARY_MEDIUM_COMPLETE, VarbitID.DESERT_DIARY_HARD_COMPLETE, VarbitID.DESERT_DIARY_ELITE_COMPLETE},
		{"Falador", VarbitID.FALADOR_DIARY_EASY_COMPLETE, VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE, VarbitID.FALADOR_DIARY_HARD_COMPLETE, VarbitID.FALADOR_DIARY_ELITE_COMPLETE},
		{"Fremennik", VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE, VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE},
		{"Kandarin", VarbitID.KANDARIN_DIARY_EASY_COMPLETE, VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE, VarbitID.KANDARIN_DIARY_HARD_COMPLETE, VarbitID.KANDARIN_DIARY_ELITE_COMPLETE},
		{"Karamja", VarbitID.ATJUN_EASY_DONE, VarbitID.ATJUN_MED_DONE, VarbitID.ATJUN_HARD_DONE, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE},
		{"Kourend & Kebos", VarbitID.KOUREND_DIARY_EASY_COMPLETE, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE, VarbitID.KOUREND_DIARY_HARD_COMPLETE, VarbitID.KOUREND_DIARY_ELITE_COMPLETE},
		{"Lumbridge & Draynor", VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE, VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE},
		{"Morytania", VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE, VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE},
		{"Varrock", VarbitID.VARROCK_DIARY_EASY_COMPLETE, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE, VarbitID.VARROCK_DIARY_HARD_COMPLETE, VarbitID.VARROCK_DIARY_ELITE_COMPLETE},
		{"Western Provinces", VarbitID.WESTERN_DIARY_EASY_COMPLETE, VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE, VarbitID.WESTERN_DIARY_HARD_COMPLETE, VarbitID.WESTERN_DIARY_ELITE_COMPLETE},
		{"Wilderness", VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE, VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE},
	};
	private static final String[] TIERS = {"easy", "medium", "hard", "elite"};

	ExportState collect(Player player, BankCache bankCache, StorageCache storageCache)
	{
		ExportState state = new ExportState();
		state.exportedAt = Instant.now().toString();

		state.player = new ExportState.PlayerInfo();
		state.player.name = player.getName();
		state.player.combatLevel = player.getCombatLevel();
		state.player.world = client.getWorld();

		// Sections switched off in the config stay empty (or null) so the file keeps the same keys
		if (config.exportSkills())
		{
			for (Skill skill : Skill.values())
			{
				ExportState.SkillInfo info = new ExportState.SkillInfo();
				info.level = client.getRealSkillLevel(skill);
				info.boosted = client.getBoostedSkillLevel(skill);
				info.xp = client.getSkillExperience(skill);
				state.skills.put(skill.getName(), info);
			}
			state.totalLevel = client.getTotalLevel();
		}

		if (config.exportQuests())
		{
			state.questPoints = client.getVarpValue(VarPlayerID.QP);
			for (Quest quest : Quest.values())
			{
				// getState runs a game script, which is one reason this must be on the client thread
				state.quests.put(quest.getName(), quest.getState(client).name());
			}
		}

		if (config.exportEquipment())
		{
			collectEquipment(state.equipment);
		}
		if (config.exportInventory())
		{
			collectInventory(state.inventory);
		}
		if (config.exportBank())
		{
			collectBank(state.bank, bankCache);
		}
		if (config.exportSlayer())
		{
			state.slayer = collectSlayer();
		}
		if (config.exportSpecials())
		{
			collectSpecials(state.specialAttack, bankCache);
		}
		if (config.exportDiaries())
		{
			collectDiaries(state.diaries);
		}
		if (config.exportBossKc())
		{
			collectBossKc(state.bossKc);
		}
		if (config.exportStorage() && storageCache != null && storageCache.accountHash == client.getAccountHash())
		{
			copyCached(state.seedVault, storageCache.seedVaultSeen, storageCache.seedVault);
			copyCached(state.potionStorage, storageCache.potionsSeen, storageCache.potions);
		}
		return state;
	}

	/** Builds a new bank cache from the open bank's contents. */
	BankCache captureBank(ItemContainer bankContainer)
	{
		BankCache cache = new BankCache();
		cache.accountHash = client.getAccountHash();
		cache.lastSeen = Instant.now().toString();

		for (Item item : bankContainer.getItems())
		{
			// Skip empty slots, placeholders (quantity 0) and bank fillers
			if (item.getId() < 0 || item.getQuantity() <= 0 || item.getId() == ItemID.BANK_FILLER)
			{
				continue;
			}
			BankCache.Entry entry = new BankCache.Entry();
			entry.id = item.getId();
			entry.name = itemName(item.getId());
			entry.qty = item.getQuantity();
			cache.items.add(entry);
		}
		return cache;
	}

	private void collectEquipment(List<ExportState.EquipmentItem> out)
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return;
		}
		for (EquipmentInventorySlot slot : EquipmentInventorySlot.values())
		{
			Item item = worn.getItem(slot.getSlotIdx());
			if (item == null || item.getId() < 0)
			{
				continue;
			}
			ExportState.EquipmentItem e = new ExportState.EquipmentItem();
			e.slot = slot.name();
			e.id = item.getId();
			e.name = itemName(item.getId());
			e.qty = item.getQuantity();
			out.add(e);
		}
	}

	private void collectInventory(List<ExportState.Item> out)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		if (inv == null)
		{
			return;
		}
		for (Item item : inv.getItems())
		{
			if (item.getId() < 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			ExportState.Item e = new ExportState.Item();
			e.id = item.getId();
			e.name = itemName(item.getId());
			e.qty = item.getQuantity();
			out.add(e);
		}
	}

	/** Copies the cached bank in, with current GE prices. Leaves it empty if the cache is for another account. */
	private void collectBank(ExportState.Bank out, BankCache cache)
	{
		if (cache == null || cache.accountHash != client.getAccountHash())
		{
			return;
		}
		out.lastSeen = cache.lastSeen;
		for (BankCache.Entry entry : cache.items)
		{
			ExportState.BankItem b = new ExportState.BankItem();
			b.id = entry.id;
			b.name = entry.name;
			b.qty = entry.qty;
			// Price per item (handles coins/platinum and noted items for us)
			b.gePrice = itemManager.getItemPrice(entry.id);
			out.items.add(b);
			out.totalGeValue += b.gePrice * b.qty;
		}
	}

	/**
	 * Current slayer task, or null if there isn't one. Uses the same lookup as
	 * RuneLite's built-in Slayer plugin (the game's slayer task database).
	 *
	 * The task lookup is adapted from RuneLite's SlayerPlugin.updateTask():
	 *   Copyright (c) 2017, Tyler <https://github.com/tylerthardy>
	 *   Copyright (c) 2018, Shaun Dreclin <shaundreclin@gmail.com>
	 * Used under the BSD 2-Clause License, the same terms as this project's LICENSE file.
	 */
	private ExportState.Slayer collectSlayer()
	{
		int remaining = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
		if (remaining <= 0)
		{
			return null;
		}

		try
		{
			int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
			int taskRow;
			if (taskId == BOSS_TASK_ID)
			{
				List<Integer> bossRows = client.getDBRowsByValue(
					DBTableID.SlayerTaskSublist.ID,
					DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
					0,
					client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
				if (bossRows.isEmpty())
				{
					return null;
				}
				taskRow = (Integer) client.getDBTableField(bossRows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0)[0];
			}
			else
			{
				List<Integer> taskRows = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, taskId);
				if (taskRows.isEmpty())
				{
					return null;
				}
				taskRow = taskRows.get(0);
			}

			ExportState.Slayer slayer = new ExportState.Slayer();
			slayer.taskName = (String) client.getDBTableField(taskRow, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0)[0];
			slayer.remaining = remaining;
			return slayer;
		}
		catch (RuntimeException e)
		{
			// Don't let a slayer lookup problem stop the rest of the export
			log.debug("Could not read slayer task", e);
			return null;
		}
	}

	/** Spec energy plus every special-attack weapon found in equipment, inventory and the cached bank. */
	private void collectSpecials(ExportState.SpecialAttack out, BankCache bankCache)
	{
		// The varp stores energy x10 (0-1000)
		out.energy = client.getVarpValue(VarPlayerID.SA_ENERGY) / 10;
		addSpecials(out, client.getItemContainer(InventoryID.WORN), "equipped");
		addSpecials(out, client.getItemContainer(InventoryID.INV), "inventory");
		if (bankCache != null && bankCache.accountHash == client.getAccountHash())
		{
			for (BankCache.Entry e : bankCache.items)
			{
				addSpecial(out, e.id, e.name, "bank");
			}
		}
	}

	private void addSpecials(ExportState.SpecialAttack out, ItemContainer container, String where)
	{
		if (container == null)
		{
			return;
		}
		for (Item item : container.getItems())
		{
			if (item.getId() >= 0 && item.getQuantity() > 0)
			{
				addSpecial(out, item.getId(), itemName(item.getId()), where);
			}
		}
	}

	private void addSpecial(ExportState.SpecialAttack out, int id, String name, String where)
	{
		String category = SpecialWeapons.category(name);
		if (category == null)
		{
			return;
		}
		ExportState.SpecialWeapon w = new ExportState.SpecialWeapon();
		w.id = id;
		w.name = name;
		w.category = category;
		w.where = where;
		out.weapons.add(w);
	}

	private void collectDiaries(Map<String, Map<String, Boolean>> out)
	{
		for (Object[] row : DIARIES)
		{
			Map<String, Boolean> tiers = new LinkedHashMap<>();
			for (int i = 0; i < TIERS.length; i++)
			{
				tiers.put(TIERS[i], client.getVarbitValue((Integer) row[i + 1]) > 0);
			}
			out.put((String) row[0], tiers);
		}
	}

	/** Kill counts that RuneLite's Chat Commands plugin saved for this account (from KC chat messages). */
	private void collectBossKc(Map<String, Integer> out)
	{
		String profile = configManager.getRSProfileKey();
		if (profile == null)
		{
			return;
		}
		List<String> bosses = new ArrayList<>(configManager.getRSProfileConfigurationKeys("killcount", profile, ""));
		Collections.sort(bosses);
		for (String boss : bosses)
		{
			Integer kc = configManager.getRSProfileConfiguration("killcount", boss, Integer.class);
			if (kc != null && kc > 0)
			{
				out.put(boss, kc);
			}
		}
	}

	private static void copyCached(ExportState.CachedItems out, String seen, List<BankCache.Entry> items)
	{
		out.lastSeen = seen;
		for (BankCache.Entry e : items)
		{
			ExportState.Item i = new ExportState.Item();
			i.id = e.id;
			i.name = e.name;
			i.qty = e.qty;
			out.items.add(i);
		}
	}

	/** New storage cache with the seed vault replaced by what's in the open vault now. */
	StorageCache captureSeedVault(ItemContainer vault, StorageCache prev)
	{
		StorageCache c = prev == null ? new StorageCache().copyFor(client.getAccountHash()) : prev.copyFor(client.getAccountHash());
		c.seedVaultSeen = Instant.now().toString();
		c.seedVault = new ArrayList<>();
		for (Item item : vault.getItems())
		{
			if (item.getId() < 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			BankCache.Entry e = new BankCache.Entry();
			e.id = item.getId();
			e.name = itemName(item.getId());
			e.qty = item.getQuantity();
			c.seedVault.add(e);
		}
		return c;
	}

	/**
	 * New storage cache with potion storage replaced, read the same way RuneLite's Bank Tags plugin
	 * does (game enums + the potion store's own scripts). Only valid while the bank is open.
	 */
	StorageCache capturePotions(StorageCache prev)
	{
		StorageCache c = prev == null ? new StorageCache().copyFor(client.getAccountHash()) : prev.copyFor(client.getAccountHash());
		c.potionsSeen = Instant.now().toString();
		c.potions = new ArrayList<>();
		for (int enumId : new int[]{EnumID.POTIONSTORE_POTIONS, EnumID.POTIONSTORE_UNFINISHED_POTIONS})
		{
			for (int potionEnumId : client.getEnum(enumId).getIntVals())
			{
				client.runScript(ScriptID.POTIONSTORE_DOSES, potionEnumId);
				int doses = client.getIntStack()[0];
				if (doses <= 0)
				{
					continue;
				}
				EnumComposition potion = client.getEnum(potionEnumId);
				int oneDoseId = potion.getIntValue(1);
				BankCache.Entry e = new BankCache.Entry();
				e.id = oneDoseId;
				// Name without the "(1)" dose marker, e.g. "Prayer potion"
				e.name = itemName(oneDoseId).replaceAll("\\(\\d\\)$", "").trim();
				e.qty = doses;
				c.potions.add(e);
			}
		}
		return c;
	}

	private String itemName(int itemId)
	{
		ItemComposition comp = itemManager.getItemComposition(itemId);
		return comp.getName();
	}
}
