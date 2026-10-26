package com.clanhub;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Lot 10J: the words a recent-cards row shows under its title, who and what, for each kind of line. */
public class RowWordsTest
{
	/** One line and the two phrases it gives. */
	private static void check(String line, String who, String what)
	{
		assertEquals("who of: " + line, who, RowWords.who(line));
		assertEquals("what of: " + line, what, RowWords.what(line));
	}

	@Test
	public void whoAndWhatOfEachLine()
	{
		check("GMucker has completed a medium combat task: Smite Fight.", "GMucker", "medium task");
		check("Blade Fiery has completed an Easy combat task: Protection from Moss (1 point).", "Blade Fiery", "easy task");
		check("FishStonks has reached 43,000,000 XP in Strength.", "FishStonks", "XP milestone");
		check("aetzli has reached Crafting level 50.", "aetzli", "level-up");
		check("aetzli has completed a quest: Observatory Quest", "aetzli", "quest");
		check("Dragonrains received a drop: Broken dragon hook (773,703 coins).", "Dragonrains", "drop");
		check("Klar received a clue item: Ranger boots (25,296,193 coins)", "Klar", "clue item");
		check("Klar received a new collection log item: Abyssal whip (1/484)", "Klar", "collection log");
		check("Klar received special loot from a raid: Twisted bow.", "Klar", "raid loot");
		check("Clipper has a funny feeling like he's being followed: Pet snakeling at 2,678 killcount.", "Clipper", "pet");
		check("Dash Inc feels something weird sneaking into his backpack: Heron at 8,426,283 XP.", "Dash Inc", "pet");
		check("Klar has completed the Elite Ardougne diary.", "Klar", "diary");
		check("Klar has achieved a new Vorkath personal best: 2:28", "Klar", "personal best");
		check("Valuable drop: Abyssal whip (1,500,000 coins)", "You", "drop");
		check("New item added to your collection log: Abyssal whip", "You", "collection log");
		check("Your Vorkath kill count is: 420.", "You", "kill count");
		check("Law rune: Used for teleport spells.", "You", "examined");
		check("Torn from the hands of ancient demons.", "You", "examined");
		// Added by the lead, 2026-10-25 (lot 10J): an examine text that holds " has " is still the player's own.
		check("This sword has seen many battles.", "You", "examined");
		check("Rune scimitar: This sword has seen many battles.", "You", "examined");
		check(null, "", "");
	}

	/** Lot 10O: the player's own level-up line, from the plugin's own trace, is theirs and a level-up. */
	@Test
	public void anOwnLevelUpIsYoursAndALevelUp()
	{
		check("Congratulations, you've just advanced your Thieving level. You are now level 77.", "You", "level-up");
	}
}
