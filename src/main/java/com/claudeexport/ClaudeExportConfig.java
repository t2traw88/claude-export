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
}
