package com.clanhub;

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
	 * then the personal best; the card's words are the boss, and the picture is the combat icon for a kill count and the
	 * clue watch for a personal best. A null text, and a boss that is only spaces, give none.
	 */
	static List<HoverCard> read(String plain)
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
			return card(new InfoCard(boss + " kill count",
				Collections.singletonList(new HoverCard.Pair("Kills", found.group("count"), "")),
				Collections.<String>emptyList(), InfoCard.pageFor(boss), plain, start, start + boss.length(),
				SkillSprites.spriteOf("Combat"), -1, false));
		}
		found = PERSONAL_BEST.matcher(plain);
		if (found.matches() && !found.group("boss").trim().isEmpty())
		{
			final String boss = found.group("boss").trim();
			final int start = startOf(found, boss);
			return card(new InfoCard(boss + " personal best",
				Collections.singletonList(new HoverCard.Pair("Time", found.group("time"), "")),
				Collections.<String>emptyList(), InfoCard.pageFor(boss), plain, start, start + boss.length(), -1,
				ItemID.TRAIL_WATCH, false));
		}
		return Collections.emptyList();
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
