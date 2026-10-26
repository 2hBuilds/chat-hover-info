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

	/** Lot 10O: one row per event, whichever channel the line came by and whether the player's own line or the clan's came first. */
	@Test
	public void oneRowPerEvent()
	{
		final CardHistory history = new CardHistory();
		final List<Integer> told = new ArrayList<>();
		history.listen(() -> told.add(told.size()));

		final String own = "Congratulations, you've just advanced your Thieving level. You are now level 77.";
		final String clan = "2hBuilds has reached Thieving level 77.";
		history.add(own, Collections.singletonList(card("Thieving 77", own)), 1_000L, "2hBuilds");
		history.add(clan, Collections.singletonList(card("Thieving 77", clan)), 2_000L, "2hBuilds");
		assertEquals("the own line and the clan line are one event", 1, history.size());
		assertEquals("the own line stays", own, history.entries().get(0).line());
		assertEquals("and the listener was told once", 1, told.size());

		final String klar = "Klar has reached Defence level 70.";
		history.add(klar, Collections.singletonList(card("Defence 70", klar)), 3_000L, "2hBuilds");
		history.add(klar, Collections.singletonList(card("Defence 70", klar)), 3_500L, "2hBuilds");
		assertEquals("Klar's two lines are one more", 2, history.size());
		assertEquals(2, told.size());

		final String mossy = "Mossy Rock has reached Defence level 70.";
		history.add(mossy, Collections.singletonList(card("Defence 70", mossy)), 4_000L, "2hBuilds");
		assertEquals("another player's line is kept", 3, history.size());

		history.add(klar, Collections.singletonList(card("Defence 70", klar)), 13_001L, "2hBuilds");
		assertEquals("Klar's line again after ten seconds is kept", 4, history.size());
		assertEquals(4, told.size());

		final String pete = "Congratulations, you've just advanced your Cooking level. You are now level 50.";
		final String petes = "iron pete has reached Cooking level 50.";
		history.add(pete, Collections.singletonList(card("Cooking 50", pete)), 20_000L, "Iron Pete");
		history.add(petes, Collections.singletonList(card("Cooking 50", petes)), 20_500L, "Iron Pete");
		assertEquals("a name with a no-break space and one in lower case are the player", 5, history.size());

		final String agilityClan = "2hBuilds has reached Agility level 60.";
		final String agilityOwn = "Congratulations, you've just advanced your Agility level. You are now level 60.";
		history.add(agilityClan, Collections.singletonList(card("Agility 60", agilityClan)), 30_000L, "2hBuilds");
		history.add(agilityOwn, Collections.singletonList(card("Agility 60", agilityOwn)), 30_400L, "2hBuilds");
		assertEquals("the clan line first and the own line second are one event too", 6, history.size());
		assertEquals("the clan line stays", agilityClan, history.entries().get(0).line());
		assertEquals(6, told.size());

		final CardHistory plain = new CardHistory();
		plain.add("one", Collections.singletonList(card("One", "one")), 1L);
		plain.add("two", Collections.singletonList(card("Two", "two")), 2L);
		assertEquals("the three-argument add still keeps two different titles", 2, plain.size());
	}
}
