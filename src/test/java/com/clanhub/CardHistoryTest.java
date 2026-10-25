package com.clanhub;

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

	/** Lot 10D: a leading "title: " is taken off a line; any other line comes back as it is. */
	@Test
	public void withoutNameDropsALeadingTitle()
	{
		assertEquals("a leading title and its colon go", "Used for teleport spells.",
			CardHistory.withoutName("Law rune: Used for teleport spells.", "Law rune"));
		assertEquals("a title inside the line stays", "Dragonrains received a drop: Law rune",
			CardHistory.withoutName("Dragonrains received a drop: Law rune", "Law rune"));
		assertEquals("a longer first word is not the title", "Law runes are nice",
			CardHistory.withoutName("Law runes are nice", "Law rune"));
		assertEquals("a null title leaves the line", "Law rune: Used for teleport spells.",
			CardHistory.withoutName("Law rune: Used for teleport spells.", null));
		assertEquals("a null line is empty", "", CardHistory.withoutName(null, "Law rune"));
	}
}
