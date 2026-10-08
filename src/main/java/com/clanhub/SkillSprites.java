package com.clanhub;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The game's own sprite ids for the pictures of the cards that are not an item's: each skill's icon, the combat and total
 * level icons, the quest icon, the diary icon and the first of the combat achievement tier swords. Plain data; the ids are
 * the gameval {@code SpriteID} constants of RuneLite 1.13.1 ({@code Staticons}, {@code Staticons2}, {@code SideIcons}).
 */
final class SkillSprites
{
	/** The quest icon. */
	static final int QUEST = 899;
	/** The achievement diary icon. */
	static final int DIARY = 1298;
	/** The Easy tier's sword; a tier's sword is this plus the tier's index, Easy 0 to Grandmaster 5. */
	static final int CA_TIER_SWORDS = 3393;

	/** The sprite of each skill, by its lower-case name. */
	private static final Map<String, Integer> SPRITES;

	static
	{
		final Map<String, Integer> sprites = new HashMap<>();
		sprites.put("attack", 197);
		sprites.put("strength", 198);
		sprites.put("defence", 199);
		sprites.put("ranged", 200);
		sprites.put("prayer", 201);
		sprites.put("magic", 202);
		sprites.put("hitpoints", 203);
		sprites.put("agility", 204);
		sprites.put("herblore", 205);
		sprites.put("thieving", 206);
		sprites.put("crafting", 207);
		sprites.put("fletching", 208);
		sprites.put("mining", 209);
		sprites.put("smithing", 210);
		sprites.put("fishing", 211);
		sprites.put("cooking", 212);
		sprites.put("firemaking", 213);
		sprites.put("woodcutting", 214);
		sprites.put("runecraft", 215);
		sprites.put("slayer", 216);
		sprites.put("farming", 217);
		sprites.put("hunter", 220);
		sprites.put("construction", 221);
		sprites.put("sailing", 228);
		sprites.put("combat", 168);
		sprites.put("total", 222);
		SPRITES = Collections.unmodifiableMap(sprites);
	}

	private SkillSprites()
	{
	}

	/**
	 * The sprite id of the skill called {@code skillName}, ignoring case: the 23 skills, "Combat" and "Total"; -1 for
	 * any other name, and for null.
	 */
	static int spriteOf(String skillName)
	{
		if (skillName == null)
		{
			return -1;
		}
		final Integer sprite = SPRITES.get(skillName.toLowerCase(Locale.ROOT));
		return sprite == null ? -1 : sprite;
	}
}
