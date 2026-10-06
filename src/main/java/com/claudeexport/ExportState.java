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
	// Added later; existing fields above are unchanged so older readers keep working
	SpecialAttack specialAttack = new SpecialAttack();
	// Region -> tier -> done, e.g. "Ardougne": {"easy": true, ...}
	Map<String, Map<String, Boolean>> diaries = new LinkedHashMap<>();
	// Boss name (lower case, as RuneLite stores it) -> kill count
	Map<String, Integer> bossKc = new LinkedHashMap<>();
	CachedItems seedVault = new CachedItems();
	CachedItems potionStorage = new CachedItems();

	static class SpecialAttack
	{
		// 0-100, or null if not exported
		Integer energy;
		List<SpecialWeapon> weapons = new ArrayList<>();
	}

	static class SpecialWeapon
	{
		int id;
		String name;
		String category;
		// "equipped", "inventory" or "bank"
		String where;
	}

	/** Storage that can only be read while open (seed vault, potion storage), cached like the bank. */
	static class CachedItems
	{
		String lastSeen;
		List<Item> items = new ArrayList<>();
	}

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
