package com.claudeexport;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * Settings shown in RuneLite's config panel. Turning a section off writes it as empty
 * (or null) instead of removing it, so tools reading state.json always see the same keys.
 */
@ConfigGroup(ClaudeExportConfig.GROUP)
public interface ClaudeExportConfig extends Config
{
	String GROUP = "claudeexport";

	@ConfigItem(
		keyName = "exportSkills",
		name = "Export skills",
		description = "Include skill levels, XP and total level",
		position = 1
	)
	default boolean exportSkills()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportQuests",
		name = "Export quests",
		description = "Include quest points and each quest's status",
		position = 2
	)
	default boolean exportQuests()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportEquipment",
		name = "Export equipment",
		description = "Include worn items",
		position = 3
	)
	default boolean exportEquipment()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportInventory",
		name = "Export inventory",
		description = "Include inventory items",
		position = 4
	)
	default boolean exportInventory()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportBank",
		name = "Export bank",
		description = "Include the last-seen bank with GE prices. When off, the bank is not cached either",
		position = 5
	)
	default boolean exportBank()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportSlayer",
		name = "Export slayer task",
		description = "Include the current slayer task and remaining count",
		position = 6
	)
	default boolean exportSlayer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportSpecials",
		name = "Export special attacks",
		description = "Include special attack energy and every special-attack weapon you own (worn, inventory, bank)",
		position = 7
	)
	default boolean exportSpecials()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportDiaries",
		name = "Export achievement diaries",
		description = "Include which diary tiers are complete in each region",
		position = 8
	)
	default boolean exportDiaries()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportBossKc",
		name = "Export boss kill counts",
		description = "Include kill counts recorded by RuneLite's Chat Commands plugin (from your kill count chat messages)",
		position = 9
	)
	default boolean exportBossKc()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportStorage",
		name = "Export seed vault + potion storage",
		description = "Include the last-seen seed vault and bank potion storage. When off, they are not cached either",
		position = 10
	)
	default boolean exportStorage()
	{
		return true;
	}
}
