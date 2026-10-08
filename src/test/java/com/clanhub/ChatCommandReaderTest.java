package com.clanhub;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link ChatCommandReader}: the answers of RuneLite's Chat Commands plugin, a kill count and a personal best, are read
 * from the text as drawn, and nothing else is.
 */
public class ChatCommandReaderTest
{
	/** The one card a text gives. */
	private static HoverCard theCard(String text)
	{
		// Re-aimed by the lead, 2026-10-08 (lot 8T): the reader takes the tables.
		final List<HoverCard> cards = ChatCommandReader.read(text, new CardTables());
		assertEquals("one card from: " + text, 1, cards.size());
		return cards.get(0);
	}

	/** The card's one row as its label and value, the two set apart by a space. */
	private static String rowOf(HoverCard card)
	{
		assertEquals("one row", 1, card.rows().size());
		assertEquals("one pair", 1, card.rows().get(0).size());
		final HoverCard.Pair pair = card.rows().get(0).get(0);
		return pair.label + " " + pair.value;
	}

	@Test
	public void readsAKillCountAnswerAsDrawn()
	{
		final HoverCard brutus = theCard("Brutus kill count: 343");
		assertEquals("Brutus kill count", brutus.title());
		assertEquals("Kills 343", rowOf(brutus));
		assertEquals("Brutus", brutus.plainLine().substring(brutus.start(), brutus.end()));
		assertFalse("the chat does not colour it: there is no event for the rewrite", brutus.marked());
		assertEquals("Brutus", brutus.wikiTitle());
		assertEquals(SkillSprites.spriteOf("Combat"), brutus.spriteId());
		assertEquals(-1, brutus.itemId());
		// Re-aimed by the lead, 2026-10-08 (lot 8U): the wiki's picture is asked for, the combat icon stands behind it.
		assertTrue("the kill count answer asks for the wiki's picture", brutus.wikiPicture());
		assertEquals("Brutus kill count: 343", brutus.plainLine());

		final HoverCard raid = theCard("Chambers of Xeric Challenge Mode kill count: 1,303");
		assertEquals("Chambers of Xeric Challenge Mode kill count", raid.title());
		assertEquals("Kills 1,303", rowOf(raid));
		assertEquals("Chambers of Xeric Challenge Mode", raid.plainLine().substring(raid.start(), raid.end()));
		assertEquals("Chambers of Xeric Challenge Mode", raid.wikiTitle());

		// "From zambzy: " is 13 characters, so the words stand at 13..19.
		final HoverCard sent = theCard("From zambzy: Brutus kill count: 343");
		assertEquals("Brutus kill count", sent.title());
		assertEquals(13, sent.start());
		assertEquals(19, sent.end());
		assertEquals("Brutus", sent.plainLine().substring(sent.start(), sent.end()));
		assertEquals("Kills 343", rowOf(sent));
	}

	@Test
	public void readsAPersonalBestAnswerAsDrawn()
	{
		final HoverCard raid = theCard("Chambers of Xeric personal best: 21:14");
		assertEquals("Chambers of Xeric personal best", raid.title());
		assertEquals("Time 21:14", rowOf(raid));
		assertEquals("Chambers of Xeric", raid.plainLine().substring(raid.start(), raid.end()));
		assertFalse("quiet", raid.marked());
		assertEquals(ItemID.TRAIL_WATCH, raid.itemId());
		assertEquals(-1, raid.spriteId());
		assertEquals("Chambers of Xeric", raid.wikiTitle());
		// Re-aimed by the lead, 2026-10-08 (lot 8U): the wiki's picture is asked for, the clue watch stands behind it.
		assertTrue("the personal best answer asks for the wiki's picture", raid.wikiPicture());

		assertEquals("Time 14:32.40", rowOf(theCard("Theatre of Blood personal best: 14:32.40")));
		assertEquals("Time 1:02:15", rowOf(theCard("Inferno personal best: 1:02:15")));
		assertTrue("a sender in front of the answer", theCard("Klar: Inferno personal best: 1:02:15").start() > 0);
	}

	@Test
	public void readsNothingElse()
	{
		final String[] none = {
			"Your Zulrah kill count is: 343.",
			"!kc brutus",
			"Iron Pete has a funny feeling like he's being followed: Pet snakeling at 2,678 killcount.",
			"Klar: gz",
			"",
			"zambzy: Brutus kill count: 343 lol",
			"kill count: 343",
		};
		for (final String text : none)
		{
			// Re-aimed by the lead, 2026-10-08 (lot 8T): the reader takes the tables.
			assertTrue("nothing from: " + text, ChatCommandReader.read(text, new CardTables()).isEmpty());
		}
		assertTrue("nothing from null", ChatCommandReader.read(null, new CardTables()).isEmpty());
	}

	/** The card's one row as each pair's label and value, in order. */
	private static List<String> pairsOf(HoverCard card)
	{
		assertEquals("one row", 1, card.rows().size());
		final List<String> pairs = new ArrayList<>();
		for (final HoverCard.Pair pair : card.rows().get(0))
		{
			pairs.add(pair.label + " " + pair.value);
		}
		return pairs;
	}

	// Re-aimed by the lead, 2026-10-08 (lot 8U): the wiki's picture replaces the pet.
	@Test
	public void theAnswersCarryTheBoss() throws Exception
	{
		final CardTables tables;
		try (Reader cards = new InputStreamReader(ChatCommandReaderTest.class.getResourceAsStream(
			"/com/clanhub/cards-fixture.json"), StandardCharsets.UTF_8);
			Reader bosses = new InputStreamReader(ChatCommandReaderTest.class.getResourceAsStream(
				"/com/clanhub/bosses-fixture.json"), StandardCharsets.UTF_8))
		{
			tables = CardTables.parse(cards, new Gson()).readBosses(bosses, new Gson());
		}

		final HoverCard brutus = ChatCommandReader.read("Brutus kill count: 343", tables).get(0);
		assertEquals("Brutus kill count", brutus.title());
		assertEquals(Arrays.asList("Kills 343", "Combat 70", "Hitpoints 100", "Max hit 10", "Quest The Ides of Milk"),
			pairsOf(brutus));
		assertEquals("a kill count has no item behind the picture", -1, brutus.itemId());
		assertTrue("it asks for the wiki's picture", brutus.wikiPicture());
		assertEquals("and the combat sprite stands behind it", SkillSprites.spriteOf("Combat"), brutus.spriteId());
		assertFalse("still quiet", brutus.marked());
		assertEquals("Brutus", brutus.plainLine().substring(brutus.start(), brutus.end()));

		final HoverCard zulrah = ChatCommandReader.read("zambzy: Zulrah kill count: 1,224", tables).get(0);
		assertEquals(Arrays.asList("Kills 1,224", "Combat 725", "Hitpoints 500", "Max hit 41"), pairsOf(zulrah));
		assertEquals("no pet: the wiki's picture replaces it", -1, zulrah.itemId());
		assertTrue(zulrah.wikiPicture());

		final HoverCard raid = ChatCommandReader.read("Chambers of Xeric personal best: 21:14", tables).get(0);
		assertEquals("a raid with no stats has the time alone", Arrays.asList("Time 21:14"), pairsOf(raid));
		assertEquals("the clue watch behind its picture", ItemID.TRAIL_WATCH, raid.itemId());
		assertTrue(raid.wikiPicture());
		assertEquals(-1, raid.spriteId());

		final HoverCard best = ChatCommandReader.read("Vorkath personal best: 2:28", tables).get(0);
		assertEquals(Arrays.asList("Time 2:28", "Combat 732", "Hitpoints 750", "Max hit 32", "Quest Dragon Slayer II"),
			pairsOf(best));

		final HoverCard bare = ChatCommandReader.read("Brutus kill count: 343", new CardTables()).get(0);
		assertEquals("a table that has not loaded gives the card as today", Arrays.asList("Kills 343"), pairsOf(bare));
		assertEquals(-1, bare.itemId());
		assertTrue("a table that has not loaded still asks for the wiki's picture", bare.wikiPicture());
		assertEquals(Arrays.asList("Kills 343"), pairsOf(ChatCommandReader.read("Nobody kill count: 343", tables).get(0)));
		final HoverCard inferno = ChatCommandReader.read("Inferno personal best: 1:02:15", tables).get(0);
		assertEquals("a personal best the table lacks keeps the clue watch", ItemID.TRAIL_WATCH, inferno.itemId());
		assertTrue(inferno.wikiPicture());
	}
}
