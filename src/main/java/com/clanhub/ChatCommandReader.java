package com.clanhub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.gameval.ItemID;

/**
 * The reader of the two answers RuneLite's own Chat Commands plugin writes into a line after it has arrived: a kill count
 * ("Brutus kill count: 343") and a personal best ("Chambers of Xeric personal best: 21:14"). A member types {@code !kc
 * brutus} or {@code !pb chambers of xeric}, and the Chat Commands plugin of everyone who runs it replaces the typed line
 * with the answer through {@code MessageNode.setRuneLiteFormatMessage}: {@code <boss> kill count: <n,nnn>}, the boss as
 * RuneLite's {@code longBossName} spells it and the count with its thousands commas, or {@code <boss> personal best:
 * <time>}, the time as {@code secondsToTimeString} writes it ("21:14", "14:32.40", "1:02:15"), the boss and the number in
 * the highlight colour. Both commands are registered asynchronously, so the rewrite lands after every {@code ChatMessage}
 * subscriber has run and no reader of the plugin ever sees the answer as an event. It is read where it can be seen, on
 * the drawn widget, by the overlay when the mouse reaches the line. Pure: the widget's plain text in, the one card out;
 * no RuneLite state.
 *
 * <p>The answer of the Slayer plugin to {@code !task} ("Slayer Task: Hellhounds: 10/155 killed") reaches the plugin the
 * same way and is read here too, by the {@link SlayerTaskReader} this reader asks last.
 *
 * <p>Choice: the whole drawn text is the key. The text has to be the answer from its first word (or its sender and a
 * colon, when the chatbox puts them in the same widget) to its last digit, so a line that goes on after the number, the
 * game's own "Your Zulrah kill count is: 343." and the typed command match nothing. The cards are the ones the
 * {@link BroadcastKindReader} gives the game's own kill count and personal best, and quiet like them: there is no event
 * for the rewrite, so the chat cannot colour the words, and the hover shows them.
 */
final class ChatCommandReader
{
	/** "Brutus kill count: 343" and "Chambers of Xeric kill count: 1,224", after the sender and a colon if any. */
	private static final Pattern KILL_COUNT = Pattern.compile(
		"(?:.*: )?(?<boss>[^:]+?) kill count: (?<count>\\d{1,3}(?:,\\d{3})*|\\d+)");
	/** "Chambers of Xeric personal best: 21:14", "14:32.40" and "1:02:15", after the sender and a colon if any. */
	private static final Pattern PERSONAL_BEST = Pattern.compile(
		"(?:.*: )?(?<boss>[^:]+?) personal best: (?<time>\\d+(?::\\d{2}){0,2}(?:\\.\\d{1,2})?)");

	private ChatCommandReader()
	{
	}

	/**
	 * The one card a widget's plain text ({@link ChatText#plain}) answers, or an empty list. The kill count is tried first,
	 * then the personal best, then the slayer task ({@link SlayerTaskReader}). The card of a kill count or a personal best
	 * has the boss as its words; its first fact is the count or the time, then, when {@code tables} knows the boss, its
	 * combat level, hitpoints, max hit and quest ({@link BossCards#facts}); the picture is the wiki's own of the boss's
	 * page, the combat icon behind it for a kill count and the clue watch for a personal best until it comes. A null text,
	 * and a boss that is only spaces, give none.
	 *
	 * <p>Choice: a table that has not loaded, or lacks the boss, gives the card as it was before the table: the count or
	 * the time alone, the wiki's picture asked for all the same.
	 */
	static List<HoverCard> read(String plain, CardTables tables)
	{
		if (plain == null)
		{
			return Collections.emptyList();
		}
		Matcher found = KILL_COUNT.matcher(plain);
		if (found.matches() && !found.group("boss").trim().isEmpty())
		{
			final String boss = found.group("boss").trim();
			final int start = startOf(found, boss);
			final CardTables.BossInfo info = tables.boss(boss);
			return card(new InfoCard(boss + " kill count", row("Kills", found.group("count"), info),
				Collections.<String>emptyList(), BossCards.page(info, boss), plain, start, start + boss.length(),
				SkillSprites.spriteOf("Combat"), -1, false, true));
		}
		found = PERSONAL_BEST.matcher(plain);
		if (found.matches() && !found.group("boss").trim().isEmpty())
		{
			final String boss = found.group("boss").trim();
			final int start = startOf(found, boss);
			final CardTables.BossInfo info = tables.boss(boss);
			return card(new InfoCard(boss + " personal best", row("Time", found.group("time"), info),
				Collections.<String>emptyList(), BossCards.page(info, boss), plain, start, start + boss.length(), -1,
				ItemID.TRAIL_WATCH, false, true));
		}
		return SlayerTaskReader.read(plain, tables);
	}

	/** The card's row: the pair of {@code label} and {@code value}, then the boss's facts. */
	private static List<HoverCard.Pair> row(String label, String value, CardTables.BossInfo info)
	{
		final List<HoverCard.Pair> row = new ArrayList<>();
		row.add(new HoverCard.Pair(label, value, ""));
		row.addAll(BossCards.facts(info));
		return row;
	}

	/** Where the trimmed boss starts in the text: the group's start, past the whitespace the trim took off its front. */
	private static int startOf(Matcher found, String trimmed)
	{
		return found.start("boss") + found.group("boss").indexOf(trimmed);
	}

	/** The one card in a list. */
	private static List<HoverCard> card(HoverCard card)
	{
		return Collections.singletonList(card);
	}
}
