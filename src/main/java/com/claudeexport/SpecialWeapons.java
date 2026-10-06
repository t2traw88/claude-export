package com.claudeexport;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Every weapon (and tool/shield) with a special attack, by name, with the type of special.
 * Source: OSRS Wiki "Special attacks" page, checked 2026-10-05 (event-only weapons left out).
 * Names are matched after removing variant tags like "(p++)", "(or)" or "(i)", so all
 * versions of a weapon count. Add new weapons here when Jagex releases them.
 */
final class SpecialWeapons
{
	private static final Map<String, String> BY_NAME = build();

	private SpecialWeapons()
	{
	}

	/** The special attack type for this item name, or null if it has no special attack. */
	static String category(String itemName)
	{
		return itemName == null ? null : BY_NAME.get(normalize(itemName));
	}

	/** Lower-case and strip trailing variant tags: "Dragon dagger(p++)" becomes "dragon dagger". */
	static String normalize(String name)
	{
		String n = name.toLowerCase(Locale.ROOT).trim();
		String prev;
		do
		{
			prev = n;
			n = n.replaceAll("\\s*\\([^()]*\\)\\s*$", "").trim();
		}
		while (!n.equals(prev));
		return n;
	}

	private static void add(Map<String, String> m, String name, String category)
	{
		m.putIfAbsent(normalize(name), category);
	}

	private static Map<String, String> build()
	{
		Map<String, String> m = new LinkedHashMap<>();
		add(m, "Dragon axe", "Skilling");
		add(m, "Dragon felling axe", "Skilling");
		add(m, "Infernal axe", "Skilling");
		add(m, "3rd Age axe", "Skilling");
		add(m, "3rd Age felling axe", "Skilling");
		add(m, "Crystal axe", "Skilling");
		add(m, "Crystal felling axe", "Skilling");
		add(m, "Dragon harpoon", "Skilling");
		add(m, "Infernal harpoon", "Skilling");
		add(m, "Crystal harpoon", "Skilling");
		add(m, "Dragon pickaxe", "Skilling");
		add(m, "Infernal pickaxe", "Skilling");
		add(m, "3rd Age pickaxe", "Skilling");
		add(m, "Crystal pickaxe", "Skilling");
		add(m, "Ancient godsword", "Healing");
		add(m, "Eldritch Nightmare staff", "Healing");
		add(m, "Keris partisan of the sun", "Healing");
		add(m, "Purging staff", "Healing");
		add(m, "Toxic blowpipe", "Healing");
		add(m, "Saradomin godsword", "Healing");
		add(m, "Brine sabre", "Stat boost");
		add(m, "Dragon battleaxe", "Stat boost");
		add(m, "Excalibur", "Stat boost");
		add(m, "Dinh's bulwark", "Multi-target");
		add(m, "Dragon crossbow", "Multi-target");
		add(m, "Dragon halberd", "Multi-target");
		add(m, "Crystal halberd", "Multi-target");
		add(m, "Dragon 2h sword", "Multi-target");
		add(m, "Rune thrownaxe", "Multi-target");
		add(m, "Vesta's spear (Deadman Mode)", "Multi-target");
		add(m, "Abyssal whip", "Stat drain");
		add(m, "Accursed sceptre", "Stat drain");
		add(m, "Ancient mace", "Stat drain");
		add(m, "Bandos godsword", "Stat drain");
		add(m, "Barrelchest anchor", "Stat drain");
		add(m, "Bone dagger", "Stat drain");
		add(m, "Bone dagger (p)", "Stat drain");
		add(m, "Darklight", "Stat drain");
		add(m, "Arclight", "Stat drain");
		add(m, "Emberlight", "Stat drain");
		add(m, "Dorgeshuun crossbow", "Stat drain");
		add(m, "Dragon scimitar", "Stat drain");
		add(m, "Dragon warhammer", "Stat drain");
		add(m, "Statius's warhammer", "Stat drain");
		add(m, "Elder maul", "Stat drain");
		add(m, "Eye of Ayak", "Stat drain");
		add(m, "Morrigan's throwing axe", "Stat drain");
		add(m, "Seercull", "Stat drain");
		add(m, "Staff of the Dead", "Stat drain");
		add(m, "Toxic Staff of the Dead", "Stat drain");
		// The uncharged version is called "Toxic staff (uncharged)" in-game
		add(m, "Toxic staff", "Stat drain");

		// Differently named versions found on each weapon's Wiki page (checked 2026-10-05).
		// Deadman-only "corrupted" weapons are deliberately left out.
		add(m, "Volcanic abyssal whip", "Stat drain");
		add(m, "Frozen abyssal whip", "Stat drain");
		add(m, "Blazing blowpipe", "Healing");
		add(m, "Dinh's blazing bulwark", "Multi-target");
		add(m, "Sara's blessed sword", "Accuracy/damage");
		add(m, "Crystal halberd full", "Multi-target");
		add(m, "New crystal halberd full", "Multi-target");
		for (int i = 1; i <= 9; i++)
		{
			add(m, "Crystal halberd " + i + "/10", "Multi-target");
		}
		// Birthday event item: its special ("Celebrate") does no damage
		add(m, "Dragon candle dagger", "Novelty");
		add(m, "Staff of Light", "Stat drain");
		add(m, "Staff of Balance", "Stat drain");
		add(m, "Tonalztics of Ralos", "Stat drain");
		add(m, "Abyssal bludgeon", "Accuracy/damage");
		add(m, "Armadyl crossbow", "Accuracy/damage");
		add(m, "Arkan blade", "Accuracy/damage");
		add(m, "Armadyl godsword", "Accuracy/damage");
		add(m, "Blue Moon spear", "Accuracy/damage");
		add(m, "Crimson kisten", "Accuracy/damage");
		add(m, "Dawnbringer", "Accuracy/damage");
		add(m, "Dragon hasta", "Accuracy/damage");
		add(m, "Dragon hasta(p)", "Accuracy/damage");
		add(m, "Dragon hasta(kp)", "Accuracy/damage");
		add(m, "Dragon longsword", "Accuracy/damage");
		add(m, "Dragon mace", "Accuracy/damage");
		add(m, "Dragon sword", "Accuracy/damage");
		add(m, "Dragon thrownaxe", "Accuracy/damage");
		add(m, "Dual macuahuitl", "Accuracy/damage");
		add(m, "Eclipse atlatl", "Accuracy/damage");
		add(m, "Granite hammer", "Accuracy/damage");
		add(m, "Keris partisan of corruption", "Accuracy/damage");
		add(m, "Light ballista", "Accuracy/damage");
		add(m, "Heavy ballista", "Accuracy/damage");
		add(m, "Magic longbow", "Accuracy/damage");
		add(m, "Magic comp bow", "Accuracy/damage");
		add(m, "Morrigan's javelin", "Accuracy/damage");
		add(m, "Noxious halberd", "Accuracy/damage");
		add(m, "Osmumten's fang", "Accuracy/damage");
		add(m, "Rune claws", "Accuracy/damage");
		add(m, "Saradomin's blessed sword", "Accuracy/damage");
		add(m, "Soulflame horn", "Accuracy/damage");
		add(m, "Sunspear", "Accuracy/damage");
		add(m, "Vesta's longsword", "Accuracy/damage");
		add(m, "Vesta's blighted longsword", "Accuracy/damage");
		add(m, "Voidwaker", "Accuracy/damage");
		add(m, "Volatile Nightmare staff", "Accuracy/damage");
		add(m, "Zaryte crossbow", "Accuracy/damage");
		add(m, "Abyssal dagger", "Multi-hit");
		add(m, "Abyssal dagger (p)", "Multi-hit");
		add(m, "Burning claws", "Multi-hit");
		add(m, "Dark bow", "Multi-hit");
		add(m, "Dragon claws", "Multi-hit");
		add(m, "Dragon dagger", "Multi-hit");
		add(m, "Dragon dagger(p)", "Multi-hit");
		add(m, "Dragon knife", "Multi-hit");
		add(m, "Dragon knife(p)", "Multi-hit");
		add(m, "Granite maul", "Multi-hit");
		add(m, "Granite maul (ornate handle)", "Multi-hit");
		add(m, "Rosewood blowpipe", "Multi-hit");
		add(m, "Magic shortbow", "Multi-hit");
		add(m, "Magic shortbow (i)", "Multi-hit");
		add(m, "Saradomin sword", "Multi-hit");
		add(m, "Vesta's spear (bh)", "Multi-hit");
		add(m, "Webweaver bow", "Multi-hit");
		add(m, "Abyssal tentacle", "Binding");
		add(m, "Dragon spear", "Binding");
		add(m, "Dragon spear(p)", "Binding");
		add(m, "Dragon spear(kp)", "Binding");
		add(m, "Zamorakian hasta", "Binding");
		add(m, "Zamorakian spear", "Binding");
		add(m, "Rod of Ivandis", "Binding");
		add(m, "Ivandis flail", "Binding");
		add(m, "Blisterwood flail", "Binding");
		add(m, "Hallowed flail", "Binding");
		add(m, "Scorching bow", "Binding");
		add(m, "Ursine chainmace", "Binding");
		add(m, "Zamorak godsword", "Binding");
		add(m, "Ancient wyvern shield", "Semi-special");
		add(m, "Dragonfire shield", "Semi-special");
		add(m, "Dragonfire ward", "Semi-special");
		add(m, "Soulreaper axe", "Semi-special");
		return Collections.unmodifiableMap(m);
	}
}