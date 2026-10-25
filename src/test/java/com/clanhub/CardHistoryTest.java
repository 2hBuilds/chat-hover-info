package com.clanhub;

import com.clanhub.ClanHubConfig.CardTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** {@link CardHistory}: the lines of the session that gave a card, newest first, and the words for how long ago they came. */
public class CardHistoryTest
{
	/** An info card of a title and a line, with no facts, notes or picture. */
	private static HoverCard card(String title, String line)
	{
		return new InfoCard(title, Collections.<HoverCard.Pair>emptyList(), Collections.<String>emptyList(), "Page", line, 0,
			line.length(), -1);
	}

	// Re-aimed by the lead, 2026-10-25 (lot 10F): the whole session, up to 5,000, the user's word.
	@Test
	public void keepsTheWholeSessionUpToFiveThousand()
	{
		final CardHistory history = new CardHistory();
		final List<HoverCard> cards = new ArrayList<>();
		for (int i = 1; i <= 5001; i++)
		{
			final HoverCard card = card("Card " + i, "line " + i);
			cards.add(card);
			history.add("line " + i, Collections.singletonList(card), 1000L * i);
		}

		assertEquals("five thousand are kept", 5000, history.size());
		final List<CardHistory.Entry> entries = history.entries();
		assertEquals("the copy holds five thousand", 5000, entries.size());
		assertEquals("the newest first", "line 5001", entries.get(0).line());
		assertEquals("the oldest kept last", "line 2", entries.get(4999).line());
		assertSame("an entry keeps its card", cards.get(5000), entries.get(0).card());
		assertEquals("and its time", 5_001_000L, entries.get(0).at());
		assertSame("the oldest kept keeps its card", cards.get(1), entries.get(4999).card());
		assertEquals("and its time", 2_000L, entries.get(4999).at());
	}

	@Test
	public void oneEntryPerLineItsFirstCard()
	{
		final CardHistory history = new CardHistory();
		final List<Integer> told = new ArrayList<>();
		history.listen(() -> told.add(told.size()));
		final HoverCard first = card("First", "two cards");
		final HoverCard second = card("Second", "two cards");

		history.add("two cards", Arrays.asList(first, second), 5L);

		assertEquals("one entry for the line", 1, history.size());
		assertSame("holding the first card", first, history.entries().get(0).card());
		assertEquals("and the line", "two cards", history.entries().get(0).line());
		assertEquals("the listener was told once", 1, told.size());

		history.add("nothing", Collections.<HoverCard>emptyList(), 6L);
		history.add("nothing either", null, 7L);

		assertEquals("an empty list and a null add nothing", 1, history.size());
		assertEquals("and tell nobody", 1, told.size());
	}

	@Test
	public void clearEmptiesAndTellsTheListeners()
	{
		final CardHistory history = new CardHistory();
		final int[] told = new int[1];
		history.listen(() -> told[0]++);

		history.add("one", Collections.singletonList(card("One", "one")), 1L);
		history.add("two", Collections.singletonList(card("Two", "two")), 2L);
		assertEquals("told after each add", 2, told[0]);

		history.clear();

		assertEquals("told after the clear too", 3, told[0]);
		assertEquals("the history is empty", 0, history.size());
		assertTrue("and holds no entries", history.entries().isEmpty());
	}

	@Test
	public void agoWords()
	{
		final long then = 1_000_000L;

		assertEquals("the same moment", "just now", CardHistory.ago(then, then));
		assertEquals("59 seconds", "just now", CardHistory.ago(then + 59_000L, then));
		assertEquals("60 seconds", "1m ago", CardHistory.ago(then + 60_000L, then));
		assertEquals("59 minutes", "59m ago", CardHistory.ago(then + 59 * 60_000L, then));
		assertEquals("60 minutes", "1h ago", CardHistory.ago(then + 60 * 60_000L, then));
		assertEquals("25 hours", "25h ago", CardHistory.ago(then + 25 * 3_600_000L, then));
	}

	/** Lot 10K: a row's time reads the way the player chose, the clocks in the zone given, the "ago" words from the present. */
	@Test
	public void timeOfReadsTheChosenWay()
	{
		final long at = Instant.parse("2026-10-25T14:32:10Z").toEpochMilli();
		final long early = Instant.parse("2026-10-25T09:05:00Z").toEpochMilli();
		final ZoneId utc = ZoneOffset.UTC;

		assertEquals("24 hours", "14:32", CardHistory.timeOf(at, CardTime.CLOCK_24, utc, at));
		assertEquals("12 hours", "2:32 PM", CardHistory.timeOf(at, CardTime.CLOCK_12, utc, at));
		assertEquals("24 hours, a morning hour keeps its zero", "09:05", CardHistory.timeOf(early, CardTime.CLOCK_24, utc, early));
		assertEquals("12 hours, a morning hour has no zero", "9:05 AM", CardHistory.timeOf(early, CardTime.CLOCK_12, utc, early));
		// Re-aimed by the lead, 2026-10-25 (lot 10N): the row's short time.
		assertEquals("ago, four minutes later", "4m", CardHistory.timeOf(at, CardTime.AGO, utc, at + 4 * 60_000L));
		assertEquals("ago, the same moment", "now", CardHistory.timeOf(at, CardTime.AGO, utc, at));
		assertEquals("ago, two hours later", "2h", CardHistory.timeOf(at, CardTime.AGO, utc, at + 2 * 3_600_000L));
		assertEquals("the zone moves the clock", "10:32", CardHistory.timeOf(at, CardTime.CLOCK_24, ZoneOffset.ofHours(-4), at));
		assertEquals("and the 12-hour one", "10:32 AM", CardHistory.timeOf(at, CardTime.CLOCK_12, ZoneOffset.ofHours(-4), at));
	}
}
