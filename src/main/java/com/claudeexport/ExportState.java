package com.claudeexport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The shape of state.json. Gson turns these fields into JSON using the field
 * names as-is (in this order), so don't rename them: other tools depend on them.
 */
class ExportState
{
	int schemaVersion = 1;
	String exportedAt;
	PlayerInfo player;
	// LinkedHashMap keeps entries in the order they were added (game order)
	Map<String, SkillInfo> skills = new LinkedHashMap<>();
	int totalLevel;
	int questPoints;
	Map<String, String> quests = new LinkedHashMap<>();
	List<EquipmentItem> equipment = new ArrayList<>();
	List<Item> inventory = new ArrayList<>();
	Bank bank = new Bank();
	// null (no active task) is written as "slayer": null
	Slayer slayer;

	static class PlayerInfo
	{
		String name;
		int combatLevel;
		int world;
	}

	static class SkillInfo
	{
		int level;
		int boosted;
		int xp;
	}

	static class EquipmentItem
	{
		String slot;
		int id;
		String name;
		int qty;
	}

	static class Item
	{
		int id;
		String name;
		int qty;
	}

	static class BankItem
	{
		int id;
		String name;
		int qty;
		long gePrice;
	}

	static class Bank
	{
		// null until the bank has been opened at least once on this account
		String lastSeen;
		List<BankItem> items = new ArrayList<>();
		long totalGeValue;
	}

	static class Slayer
	{
		String taskName;
		int remaining;
	}
}
