package com.clanhub;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.ChatMessageType;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** {@link OwnLineReader}: the player's valuable drop, untradeable drop and collection log lines each give their item's card. */
public class OwnLineReaderTest
{
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
	public void readsValuableUntradeableAndCollectionLogLines()
	{
		// Re-aimed by the lead, 2026-10-08 (lot 8T): the reader takes the tables.
		final OwnLineReader reader = new OwnLineReader(echoing(), () -> true, new CardTables());

		final List<HoverCard> valuable = reader.read(ChatMessageType.GAMEMESSAGE,
			"Valuable drop: Dragon claws (121,453,000 coins)");
		assertEquals(1, valuable.size());
		assertEquals("Dragon claws", valuable.get(0).title());
		assertEquals(1, valuable.get(0).quantity());
		assertEquals(15, valuable.get(0).start());
		assertEquals(27, valuable.get(0).end());

		final List<HoverCard> stack = reader.read(ChatMessageType.GAMEMESSAGE,
			"Valuable drop: 10 x Black knife (4,600 coins)");
		assertEquals(1, stack.size());
		assertEquals(10, stack.get(0).quantity());
		assertEquals("Black knife x10", stack.get(0).title());

		final List<HoverCard> pet = reader.read(ChatMessageType.GAMEMESSAGE, "Untradeable drop: Pet snakeling");
		assertEquals(1, pet.size());
		assertEquals("Pet snakeling", pet.get(0).title());
		final List<HoverCard> valued = reader.read(ChatMessageType.GAMEMESSAGE,
			"Untradeable drop: Pet snakeling (1 coin)");
		assertEquals("Pet snakeling", valued.get(0).title());

		final List<HoverCard> log = reader.read(ChatMessageType.GAMEMESSAGE,
			"New item added to your collection log: Abyssal whip");
		assertEquals(1, log.size());
		assertEquals("Abyssal whip", log.get(0).title());
		assertEquals(39, log.get(0).start());
		assertEquals(51, log.get(0).end());

		assertTrue(reader.read(ChatMessageType.GAMEMESSAGE, "You have been poisoned.").isEmpty());
		assertTrue("only own game lines are read",
			reader.read(ChatMessageType.CLAN_MESSAGE, "Valuable drop: Dragon claws (121,453,000 coins)").isEmpty());
	}

	@Test
	public void aValuableDropWithASourceGivesTheBossCard()
	{
		final String line = "Valuable drop: 45 x Runite ore (456,345 coins) from Duke Sucellus";

		// Re-aimed by the lead, 2026-10-08 (lot 8T): the reader takes the tables.
		final List<HoverCard> both = new OwnLineReader(echoing(), () -> true, new CardTables())
			.read(ChatMessageType.GAMEMESSAGE, line);
		assertEquals("the item's card and the boss's", 2, both.size());
		assertEquals(45, both.get(0).quantity());
		final HoverCard boss = both.get(1);
		assertEquals("Duke Sucellus", boss.title());
		assertEquals("Duke Sucellus", boss.plainLine().substring(boss.start(), boss.end()));
		assertFalse("quiet", boss.marked());

		assertEquals("with the switch off, the item's card alone", 1,
			new OwnLineReader(echoing(), () -> false, new CardTables()).read(ChatMessageType.GAMEMESSAGE, line).size());
		assertEquals("a valuable drop with no source gives one card", 1,
			new OwnLineReader(echoing(), () -> true, new CardTables()).read(ChatMessageType.GAMEMESSAGE,
				"Valuable drop: Dragon claws (121,453,000 coins)").size());
	}

	// Re-aimed by the lead, 2026-10-08 (lot 8U): the wiki's picture replaces the pet.
	@Test
	public void theOwnDropBossCardCarriesTheBoss() throws Exception
	{
		final String line = "Valuable drop: 45 x Runite ore (456,345 coins) from Duke Sucellus";
		final CardTables tables;
		try (Reader cards = new InputStreamReader(OwnLineReaderTest.class.getResourceAsStream(
			"/com/clanhub/cards-fixture.json"), StandardCharsets.UTF_8);
			Reader bosses = new InputStreamReader(OwnLineReaderTest.class.getResourceAsStream(
				"/com/clanhub/bosses-fixture.json"), StandardCharsets.UTF_8))
		{
			tables = CardTables.parse(cards, new Gson()).readBosses(bosses, new Gson());
		}

		final HoverCard boss = new OwnLineReader(echoing(), () -> true, tables)
			.read(ChatMessageType.GAMEMESSAGE, line).get(1);
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
		assertEquals("https://oldschool.runescape.wiki/w/Duke_Sucellus", boss.wikiUrl());
		assertFalse("still quiet", boss.marked());

		final HoverCard bare = new OwnLineReader(echoing(), () -> true, new CardTables())
			.read(ChatMessageType.GAMEMESSAGE, line).get(1);
		assertTrue("a table that has not loaded gives no facts", bare.rows().get(0).isEmpty());
		assertEquals("and no pet", -1, bare.itemId());
		assertTrue("the wiki's picture is asked for all the same", bare.wikiPicture());
		assertEquals("the combat sprite is the picture, as in lot 8R", SkillSprites.spriteOf("Combat"), bare.spriteId());
	}
}
