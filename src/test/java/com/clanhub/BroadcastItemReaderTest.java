package com.clanhub;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.ChatMessageType;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link BroadcastItemReader}: a drop line gives its item's card, a source gives the boss's too, other chat gives none. */
public class BroadcastItemReaderTest
{
	// Re-aimed by the lead, 2026-10-08 (lot 8T): the reader takes the tables.
	private static final String LINE = "Klar received a drop: Dragon claws (121,453,000 coins).";

	/** An {@link ItemCards} that answers every name with a card titled by it, at the offsets it was given. */
	private static ItemCards echoing()
	{
		final ItemCards itemCards = Mockito.mock(ItemCards.class);
		when(itemCards.forName(anyString(), anyInt(), anyString(), anyInt(), anyInt())).thenAnswer(call ->
			new ItemCard(0, call.getArgument(0), call.getArgument(1), 0L, 0, call.getArgument(2),
				call.getArgument(3), call.getArgument(4)));
		return itemCards;
	}

	@Test
	public void aDropLineGivesOneCardAndChatterNone()
	{
		final int start = LINE.indexOf("Dragon claws");
		final int end = start + "Dragon claws".length();
		final HoverCard card = new ItemCard(13652, "Dragon claws", 1, 121_453_000L, 123_000, LINE, start, end);
		final ItemCards itemCards = Mockito.mock(ItemCards.class);
		when(itemCards.forName("Dragon claws", 1, LINE, start, end)).thenReturn(card);
		final BroadcastItemReader reader = new BroadcastItemReader(itemCards, () -> true, new CardTables());

		assertEquals(Collections.singletonList(card), reader.read(ChatMessageType.CLAN_MESSAGE, LINE));

		final List<HoverCard> chatter = reader.read(ChatMessageType.CLAN_MESSAGE, "Klar: gz!!!");
		assertTrue("chatter holds no card", chatter.isEmpty());
		verify(itemCards, times(1)).forName(anyString(), anyInt(), anyString(), anyInt(), anyInt());
	}

	@Test
	public void aDropLineWithASourceGivesTheBossCardToo()
	{
		final String duke = "N o r s received a drop: 45 x Runite ore (456,345 coins) from Duke Sucellus.";

		final List<HoverCard> both = new BroadcastItemReader(echoing(), () -> true, new CardTables())
			.read(ChatMessageType.CLAN_MESSAGE, duke);
		assertEquals("the item's card and the boss's", 2, both.size());
		assertTrue("the item first: " + both.get(0).title(), both.get(0).title().contains("Runite ore"));
		assertEquals(45, both.get(0).quantity());
		final HoverCard boss = both.get(1);
		assertTrue("the boss's card is an info card", boss instanceof InfoCard);
		assertEquals("Duke Sucellus", boss.title());
		assertEquals("Duke Sucellus", boss.plainLine().substring(boss.start(), boss.end()));
		assertFalse("quiet, so the item stays the one blue word", boss.marked());
		assertEquals("Duke Sucellus", boss.wikiTitle());
		assertTrue("no facts", boss.rows().isEmpty() || (boss.rows().size() == 1 && boss.rows().get(0).isEmpty()));
		assertTrue("no notes of its own", boss.notes().isEmpty());

		final List<HoverCard> off = new BroadcastItemReader(echoing(), () -> false, new CardTables())
			.read(ChatMessageType.CLAN_MESSAGE, duke);
		assertEquals("with the switch off, the item's card alone", 1, off.size());
		assertTrue(off.get(0).title().contains("Runite ore"));

		final BroadcastItemReader reader = new BroadcastItemReader(echoing(), () -> true, new CardTables());
		assertEquals("a line with no 'from' gives the item's card alone", 1,
			reader.read(ChatMessageType.CLAN_MESSAGE, LINE).size());

		final List<HoverCard> raid = reader.read(ChatMessageType.CLAN_MESSAGE,
			"Klar received a drop: Dragon claws (121,453,000 coins) from Chambers of Xeric.");
		assertEquals(2, raid.size());
		assertEquals("Chambers of Xeric", raid.get(1).title());
	}

	/** The cards fixture's tables with the bosses fixture read into them, as the plugin's own are. */
	private static CardTables fixtureTables() throws Exception
	{
		try (Reader cards = new InputStreamReader(BroadcastItemReaderTest.class.getResourceAsStream(
			"/com/clanhub/cards-fixture.json"), StandardCharsets.UTF_8);
			Reader bosses = new InputStreamReader(BroadcastItemReaderTest.class.getResourceAsStream(
				"/com/clanhub/bosses-fixture.json"), StandardCharsets.UTF_8))
		{
			return CardTables.parse(cards, new Gson()).readBosses(bosses, new Gson());
		}
	}

	// Re-aimed by the lead, 2026-10-08 (lot 8U): the wiki's picture replaces the pet.
	@Test
	public void theBossCardCarriesTheTablesFactsAndAsksForThePicture() throws Exception
	{
		final String duke = "N o r s received a drop: 45 x Runite ore (456,345 coins) from Duke Sucellus.";

		final HoverCard boss = new BroadcastItemReader(echoing(), () -> true, fixtureTables())
			.read(ChatMessageType.CLAN_MESSAGE, duke).get(1);
		assertEquals("one row of facts", 1, boss.rows().size());
		final List<String> facts = new ArrayList<>();
		for (final HoverCard.Pair pair : boss.rows().get(0))
		{
			facts.add(pair.label + " " + pair.value);
		}
		assertEquals(Arrays.asList("Combat 450", "Hitpoints 440", "Max hit 24",
			"Quest Desert Treasure II - The Fallen Empire"), facts);
		assertEquals("no pet: the wiki's picture replaces it", -1, boss.itemId());
		assertTrue("the card asks for the wiki's picture", boss.wikiPicture());
		assertEquals("the combat sprite stands behind it", SkillSprites.spriteOf("Combat"), boss.spriteId());
		assertEquals("Duke Sucellus", boss.title());
		assertEquals("Duke Sucellus", boss.plainLine().substring(boss.start(), boss.end()));
		assertEquals("the wiki page is the table's", "https://oldschool.runescape.wiki/w/Duke_Sucellus", boss.wikiUrl());
		assertFalse("still quiet", boss.marked());

		final HoverCard bare = new BroadcastItemReader(echoing(), () -> true, new CardTables())
			.read(ChatMessageType.CLAN_MESSAGE, duke).get(1);
		assertTrue("a table that has not loaded gives no facts", bare.rows().get(0).isEmpty());
		assertEquals("and no pet", -1, bare.itemId());
		assertTrue("the wiki's picture is asked for all the same", bare.wikiPicture());
		assertEquals("the combat sprite is the picture, as in lot 8R", SkillSprites.spriteOf("Combat"), bare.spriteId());

		final HoverCard unknown = new BroadcastItemReader(echoing(), () -> true, fixtureTables())
			.read(ChatMessageType.CLAN_MESSAGE, "Klar received a drop: Dragon claws (121,453,000 coins) from Nobody.")
			.get(1);
		assertTrue("a boss the table lacks gives no facts", unknown.rows().get(0).isEmpty());
		assertEquals(-1, unknown.itemId());
		assertTrue("and still asks for the wiki's picture", unknown.wikiPicture());
		assertEquals("with the combat sprite behind it", SkillSprites.spriteOf("Combat"), unknown.spriteId());
		assertEquals("Nobody", unknown.wikiTitle());
	}
}
