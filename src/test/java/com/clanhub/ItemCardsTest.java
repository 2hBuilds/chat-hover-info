package com.clanhub;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;
import org.junit.Assume;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/** {@link ItemCards}: a known item name becomes a card with its id and values, an unknown or only-similar name nothing. */
public class ItemCardsTest
{
	private static final String LINE = "Klar received a drop: Dragon claws (121,453,000 coins).";

	/** A search result the way the item manager gives one. */
	private static ItemPrice result(int id, String name)
	{
		final ItemPrice price = new ItemPrice();
		price.setId(id);
		price.setName(name);
		return price;
	}

	@Test
	public void aKnownNameBecomesACardAndAnUnknownNameNothing()
	{
		final ItemManager itemManager = Mockito.mock(ItemManager.class);
		when(itemManager.search("Dragon claws")).thenReturn(Collections.singletonList(result(13652, "Dragon claws")));
		when(itemManager.getItemPrice(13652)).thenReturn(121_453_000L);
		final ItemComposition composition = Mockito.mock(ItemComposition.class);
		when(composition.getHaPrice()).thenReturn(123_000);
		when(itemManager.getItemComposition(13652)).thenReturn(composition);
		final ItemCards cards = new ItemCards(itemManager);

		final HoverCard card = cards.forName("Dragon claws", 1, LINE, 24, 36);
		assertTrue("an item card", card instanceof ItemCard);
		final ItemCard item = (ItemCard) card;
		assertEquals(13652, item.id);
		assertEquals(121_453_000L, item.price);
		assertEquals(123_000, item.alch);
		assertEquals(1, item.quantity);
		assertEquals("Dragon claws", item.title());
		assertEquals(LINE, item.plainLine());
		assertEquals(24, item.start());
		assertEquals(36, item.end());

		assertNull("a name the item manager does not know", cards.forName("Nonsense", 1, LINE, 24, 36));

		final ItemManager similar = Mockito.mock(ItemManager.class);
		when(similar.search("Dragon claws")).thenReturn(Collections.singletonList(result(23_000, "Dragon claws (or)")));
		assertNull("only exact names count", new ItemCards(similar).forName("Dragon claws", 1, LINE, 24, 36));
	}

	@Test
	public void anUntradeableNameComesFromTheIndex()
	{
		final ItemManager itemManager = Mockito.mock(ItemManager.class);
		when(itemManager.search("Pet snakeling")).thenReturn(Collections.<ItemPrice>emptyList());
		when(itemManager.getItemPrice(12921)).thenReturn(0L);
		final ItemComposition composition = Mockito.mock(ItemComposition.class);
		when(composition.getHaPrice()).thenReturn(0);
		when(itemManager.getItemComposition(12921)).thenReturn(composition);
		final ItemNameIndex index = new ItemNameIndex();
		index.add(12921, "Pet snakeling");
		final String line = "Klar received a drop: Pet snakeling (0 coins).";

		final ItemCards cards = new ItemCards(itemManager, new DropRates(), index);
		final HoverCard card = cards.forName("Pet snakeling", 1, line, 0, 13);
		assertTrue("an item card", card instanceof ItemCard);
		final ItemCard item = (ItemCard) card;
		assertEquals(12921, item.id);
		assertEquals(0L, item.price);
		assertEquals("Pet snakeling", item.title());

		assertEquals(12921, cards.idOf("Pet Snakeling"));
		assertEquals(-1, cards.idOf("Nonsense"));
		assertEquals("with no index there is nothing to ask", -1, new ItemCards(itemManager).idOf("Pet snakeling"));
	}

	@Test
	public void theDropRatesFeedTheCard() throws Exception
	{
		final ItemManager itemManager = Mockito.mock(ItemManager.class);
		when(itemManager.search("Abyssal whip")).thenReturn(Collections.singletonList(result(4151, "Abyssal whip")));
		when(itemManager.getItemPrice(4151)).thenReturn(1_500_000L);
		final ItemComposition composition = Mockito.mock(ItemComposition.class);
		when(composition.getHaPrice()).thenReturn(72_000);
		when(itemManager.getItemComposition(4151)).thenReturn(composition);
		final String line = "Klar received a drop: Abyssal whip (1,500,000 coins).";
		final DropRates rates;
		try (Reader in = new InputStreamReader(ItemCardsTest.class.getResourceAsStream("/com/clanhub/drops-fixture.json"),
			StandardCharsets.UTF_8))
		{
			rates = DropRates.parse(in, new Gson());
		}

		final List<List<HoverCard.Pair>> rows = new ItemCards(itemManager, rates)
			.forName("Abyssal whip", 1, line, 0, 12).rows();
		// Re-aimed by the lead, 2026-10-08 (lot 8k): the table's order stands, four rows, then "and N more".
		assertEquals("the price row and two drop rows", 3, rows.size());
		// Re-aimed by the lead, 2026-10-08 (lot 8c): a drop row is the source and its rate, so a painter can right-align the rate.
		assertEquals("Abyssal demon", rows.get(1).get(0).label);
		assertEquals("1/512", rows.get(1).get(0).value);
		assertEquals("Greater abyssal demon", rows.get(2).get(0).label);
		assertEquals("1/256", rows.get(2).get(0).value);

		final List<List<HoverCard.Pair>> bare = new ItemCards(itemManager, new DropRates())
			.forName("Abyssal whip", 1, line, 0, 12).rows();
		assertEquals("a table that has not loaded adds no row", 1, bare.size());
	}

	@Test
	public void theRateOfANameIsItsFirstSources() throws Exception
	{
		final DropRates rates;
		try (Reader in = new InputStreamReader(ItemCardsTest.class.getResourceAsStream("/com/clanhub/drops-fixture.json"),
			StandardCharsets.UTF_8))
		{
			rates = DropRates.parse(in, new Gson());
		}
		final ItemCards cards = new ItemCards(Mockito.mock(ItemManager.class), rates);

		assertEquals("1/512", cards.rateOf("Abyssal whip"));
		assertEquals("the case rule is the lookup's", "1/512", cards.rateOf("abyssal WHIP"));
		assertEquals("1/4000", cards.rateOf("Pet Snakeling"));
		assertEquals("", cards.rateOf("Nothing at all"));
		assertEquals("no table", "", new ItemCards(Mockito.mock(ItemManager.class), null).rateOf("Abyssal whip"));
		assertEquals("a table that has not loaded", "",
			new ItemCards(Mockito.mock(ItemManager.class), new DropRates()).rateOf("Abyssal whip"));
	}

	/**
	 * A source whose name holds one of these is a container, which the table lists after every monster. A copy of
	 * CONTAINER_WORDS in docs/data/fetch_drops.py, which makes the order: keep the two the same.
	 */
	private static final String[] CONTAINER_WORDS = {"chest", "cache", "lockbox", "casket", "sack", "coffin", "impling",
		"reward", "crate", "supply", "jar", "pouch", "barrel", "urn", "nest", "unsired"};

	private static final String BUNDLED = "/com/clanhub/drops.json";

	private static boolean isContainer(String source)
	{
		final String name = source.toLowerCase(Locale.ROOT);
		for (final String word : CONTAINER_WORDS)
		{
			if (name.contains(word))
			{
				return true;
			}
		}
		return false;
	}

	private static Reader bundled()
	{
		return new InputStreamReader(ItemCardsTest.class.getResourceAsStream(BUNDLED), StandardCharsets.UTF_8);
	}

	@Test
	public void theBundledTableIsMonstersFirstAndSixAtMost() throws Exception
	{
		Assume.assumeNotNull(ItemCardsTest.class.getResource(BUNDLED));
		final JsonObject items;
		try (Reader in = bundled())
		{
			items = new Gson().fromJson(in, JsonObject.class).getAsJsonObject("items");
		}
		assertTrue("the table holds items", items.size() > 1000);
		for (final Map.Entry<String, JsonElement> item : items.entrySet())
		{
			final JsonArray sources = item.getValue().getAsJsonArray();
			assertTrue(item.getKey() + " has " + sources.size() + " sources, more than six", sources.size() <= 6);
			boolean containerSeen = false;
			for (final JsonElement source : sources)
			{
				final String from = source.getAsJsonObject().get("from").getAsString();
				if (isContainer(from))
				{
					containerSeen = true;
				}
				else
				{
					assertFalse(item.getKey() + " lists the monster " + from + " after a container", containerSeen);
				}
			}
		}

		final DropRates rates;
		try (Reader in = bundled())
		{
			rates = DropRates.parse(in, new Gson());
		}
		final List<DropRates.DropSource> whip = rates.lookup("Abyssal whip");
		assertEquals(3, whip.size());
		assertEquals("Greater abyssal demon", whip.get(0).from);
		assertEquals("Abyssal demon", whip.get(1).from);
		assertEquals("Unsired", whip.get(2).from);

		final List<DropRates.DropSource> platelegs = rates.lookup("Dragon platelegs");
		assertTrue("Dragon platelegs has at least five sources: " + platelegs.size(), platelegs.size() >= 5);
		assertFalse("and its first is a monster: " + platelegs.get(0).from, isContainer(platelegs.get(0).from));
	}
}
