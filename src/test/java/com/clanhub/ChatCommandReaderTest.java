package com.clanhub;

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
		final List<HoverCard> cards = ChatCommandReader.read(text);
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
			assertTrue("nothing from: " + text, ChatCommandReader.read(text).isEmpty());
		}
		assertTrue("nothing from null", ChatCommandReader.read(null).isEmpty());
	}
}
