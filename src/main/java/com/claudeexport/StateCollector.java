package com.claudeexport;

import java.time.Instant;
import java.util.List;
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

	ExportState collect(Player player, BankCache bankCache)
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

	private String itemName(int itemId)
	{
		ItemComposition comp = itemManager.getItemComposition(itemId);
		return comp.getName();
	}
}
