package com.clanhub;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The two short phrases a recent-cards row shows under its title: who the line is about and what kind of card it gave.
 * Pure: a row's plain chat line in, words out, nothing read from the client.
 *
 * <p>{@link #who} is the text before the first broadcast phrase (" received a ", " has completed ", " has reached ",
 * " feels like " and the like, never a bare " has ", which an examine text may hold), as a broadcast starts with the
 * player's name ("Klar received a drop: ..."), and "You" for a line that has none of them, which is the player's own
 * line ("Valuable drop: ...", "Your Vorkath kill count is: 420.", an examine line).
 *
 * <p>{@link #what} is the kind, the first of these that fits, matched ignoring case:
 * <ul>
 * <li>"drop": "Dragonrains received a drop: Broken dragon hook (773,703 coins)." or "Valuable drop: ...", "Untradeable drop: ..."</li>
 * <li>"clue item": "Klar received a clue item: Ranger boots (25,296,193 coins)"</li>
 * <li>"collection log": "Klar received a new collection log item: Abyssal whip (1/484)" or "New item added to your collection log: ..."</li>
 * <li>"raid loot": "Klar received special loot from a raid: Twisted bow."</li>
 * <li>"quest": "aetzli has completed a quest: Observatory Quest"</li>
 * <li>"medium task", "easy task" and the other tiers, in lower case: "GMucker has completed a medium combat task: Smite Fight."</li>
 * <li>"pet": "Clipper has a funny feeling like he's being followed: Pet snakeling at 2,678 killcount.", "... sneaking into ...", "... acquired something special ..."</li>
 * <li>"XP milestone": "FishStonks has reached 43,000,000 XP in Strength."</li>
 * <li>"level-up": "aetzli has reached Crafting level 50." or the player's own "Congratulations, you've just advanced your Thieving level. You are now level 77."</li>
 * <li>"diary": "Klar has completed the Elite Ardougne diary."</li>
 * <li>"personal best": "Klar has achieved a new Vorkath personal best: 2:28"</li>
 * <li>"kill count": "Your Vorkath kill count is: 420."</li>
 * <li>"examined": any other line whose {@link #who} is "You", such as "Law rune: Used for teleport spells."</li>
 * <li>"" for anything else.</li>
 * </ul>
 */
final class RowWords
{
	/** What {@link #who} says for a line that names no player. */
	static final String YOU = "You";

	/** The words after a player's name in a broadcast: the first of them ends the name. */
	private static final String[] NAME_ENDS = {" received a ", " received special ", " has completed ", " has reached ",
		" has achieved ", " has a funny feeling ", " feels something ", " feels like "};
	/** "medium combat task": the tier, which {@link #what} says in lower case with "task". */
	private static final Pattern TASK = Pattern.compile(
		"\\b(easy|medium|hard|elite|master|grandmaster) combat task", Pattern.CASE_INSENSITIVE);

	private RowWords()
	{
	}

	/** Who the line is about: see the class comment. A null line gives "". */
	static String who(String line)
	{
		if (line == null)
		{
			return "";
		}
		int first = -1;
		for (final String end : NAME_ENDS)
		{
			final int at = line.indexOf(end);
			if (at >= 0 && (first < 0 || at < first))
			{
				first = at;
			}
		}
		return first < 0 ? YOU : line.substring(0, first).trim();
	}

	/** What kind of card the line gave: see the class comment. A null line gives "". */
	static String what(String line)
	{
		if (line == null)
		{
			return "";
		}
		final String lower = line.toLowerCase(Locale.ROOT);
		if (lower.contains("received a drop") || lower.contains("valuable drop") || lower.contains("untradeable drop"))
		{
			return "drop";
		}
		if (lower.contains("received a clue item"))
		{
			return "clue item";
		}
		if (lower.contains("new collection log item") || lower.contains("new item added to your collection log"))
		{
			return "collection log";
		}
		if (lower.contains("special loot from a raid"))
		{
			return "raid loot";
		}
		if (lower.contains("completed a quest"))
		{
			return "quest";
		}
		final Matcher task = TASK.matcher(line);
		if (task.find())
		{
			return task.group(1).toLowerCase(Locale.ROOT) + " task";
		}
		if (lower.contains("being followed") || lower.contains("sneaking into") || lower.contains("acquired something special"))
		{
			return "pet";
		}
		if (lower.contains(" xp in "))
		{
			return "XP milestone";
		}
		if (lower.contains("level")
			&& (lower.contains("has reached") || lower.contains("just advanced your") || lower.contains("just advanced a")))
		{
			return "level-up";
		}
		if (lower.contains(" diary"))
		{
			return "diary";
		}
		if (lower.contains("personal best"))
		{
			return "personal best";
		}
		if (lower.contains("count is"))
		{
			return "kill count";
		}
		return YOU.equals(who(line)) ? "examined" : "";
	}
}
