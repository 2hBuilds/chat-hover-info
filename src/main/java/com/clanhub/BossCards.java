package com.clanhub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the four boss cards (a drop line's boss, a kill count, a personal best and the Chat Commands answers) take from
 * the bundled boss table: the facts under the title and the wiki page. Pure, no state: the {@link CardTables.BossInfo} in,
 * the pairs and the page out; a boss the table lacks gives no facts and the page built from its name, so its card is the
 * one the line alone gives. The picture is not the table's: the card asks the wiki for its own ({@link WikiPictures}).
 *
 * <p>Choice: a fact is a row only when there is something to say: a boss with no quest has no "Quest" pair, and a raid
 * whose page has no stats has the quest, if any, alone.
 */
final class BossCards
{
	private BossCards()
	{
	}

	/**
	 * The pairs Combat, Hitpoints, Max hit and Quest, in that order, each only when the boss's value for it is not empty;
	 * an empty list for no boss (null).
	 */
	static List<HoverCard.Pair> facts(CardTables.BossInfo info)
	{
		if (info == null)
		{
			return Collections.emptyList();
		}
		final List<HoverCard.Pair> facts = new ArrayList<>(4);
		add(facts, "Combat", info.combat);
		add(facts, "Hitpoints", info.hitpoints);
		add(facts, "Max hit", info.maxHit);
		add(facts, "Quest", info.quest);
		return facts;
	}

	/** The wiki page of the boss's card: the table's page for a boss it knows, else the page built from {@code name}. */
	static String page(CardTables.BossInfo info, String name)
	{
		return info == null || info.wiki.isEmpty() ? InfoCard.pageFor(name) : info.wiki;
	}

	/** The pair of a label and a value with no note, added when the value is not empty. */
	private static void add(List<HoverCard.Pair> facts, String label, String value)
	{
		if (!value.isEmpty())
		{
			facts.add(new HoverCard.Pair(label, value, ""));
		}
	}
}
